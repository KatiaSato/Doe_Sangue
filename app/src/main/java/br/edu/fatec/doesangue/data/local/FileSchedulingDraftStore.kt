package br.edu.fatec.doesangue.data.local

import android.util.AtomicFile
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.SchedulingDraftStore
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Instância única no container; diretório privado excluído do backup pelo Application. */
class FileSchedulingDraftStore(private val directory: File) : SchedulingDraftStore {
    // AtomicFile não trava leitores/escritores. A trava cobre também instâncias recriadas.
    private val mutex get() = processMutex
    private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true }
    private fun file(ownerId: String): AtomicFile {
        require(ownerId.isNotBlank() && ownerId.length <= 160)
        val name = MessageDigest.getInstance("SHA-256").digest(ownerId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        return AtomicFile(File(directory, "$name.json"))
    }
    private suspend fun <T> operation(block: () -> T): Result<T> = try {
        withContext(Dispatchers.IO) { mutex.withLock { ensureActive(); Result.success(block()) } }
    } catch (cancelled: CancellationException) { throw cancelled }
      catch (failure: Exception) { Result.failure(failure) }

    override suspend fun read(ownerId: String): Result<SchedulingDraft?> = operation {
        val file = file(ownerId)
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) null
        else file.openRead().use { input ->
            val bytes = input.readBytesLimited()
            try {
                val dto = json.decodeFromString<DraftDto>(bytes.toString(Charsets.UTF_8))
                require(dto.version in 1..2 && dto.ownerId == ownerId)
                dto.toDomain().validated()
            } catch (_: Exception) { throw InvalidSchedulingDraft() }
        }
    }
    private fun java.io.InputStream.readBytesLimited(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            if (output.size() + count > 16_384) throw InvalidSchedulingDraft()
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
    override suspend fun save(draft: SchedulingDraft): Result<Unit> = operation {
        draft.validated()
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Draft directory unavailable")
        val file = file(draft.ownerId)
        val output = file.startWrite()
        try {
            val bytes = json.encodeToString(DraftDto.from(draft)).toByteArray(Charsets.UTF_8)
            output.write(bytes)
            output.fd.sync()
            file.finishWrite(output)
            // finishWrite pode apenas registrar uma falha de rename; não enviar confiando nisso.
            if (!file.readFully().contentEquals(bytes)) throw IOException("Draft verification failed")
        } catch (failure: Exception) { file.failWrite(output); throw failure }
    }
    override suspend fun clear(ownerId: String): Result<Unit> = operation {
        val file = file(ownerId)
        file.delete()
        if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) throw IOException("Draft removal failed")
    }
    @Serializable private data class OriginDto(val id: String,val slot: String,val center: String,val start: String,val zone: String) {
        fun domain() = RescheduleOrigin(id,slot,center,java.time.Instant.parse(start),zone).validated()
    }
    @Serializable private data class ChoiceDto(val start: String,val interval: Int,val zone: String,val version: Long) {
        fun domain() = RoutineChoice(java.time.LocalDate.parse(start),interval,zone,version).validated()
    }
    companion object { private val processMutex = Mutex() }
    @Serializable private data class DraftDto(
        val version: Int, val ownerId: String, val centerId: String,
        val centerName: String, val timeZoneId: String, val dateIso: String? = null,
        val slotId: String? = null, val startsAtIso: String? = null,
        val step: String = SchedulingStep.CENTER.name, val requestKey: String? = null,
        val origin: OriginDto? = null, val routine: ChoiceDto? = null,
    ) {
        fun toDomain() = SchedulingDraft(ownerId, centerId, centerName, timeZoneId, dateIso, slotId, startsAtIso, SchedulingStep.valueOf(step), requestKey, origin?.domain(), routine?.domain())
        companion object {
            fun from(draft: SchedulingDraft) = with(draft) {
                DraftDto(version = 2, ownerId = ownerId, centerId = centerId, centerName = centerName,
                    timeZoneId = timeZoneId, dateIso = dateIso, slotId = slotId,
                    startsAtIso = startsAtIso, step = step.name, requestKey = requestKey,
                    origin = rescheduleOrigin?.let { OriginDto(it.appointmentId,it.timeSlotId,it.centerName,it.startsAt.toString(),it.timeZoneId) },
                    routine = routine?.let { ChoiceDto(it.startsOn.toString(),it.intervalDays,it.timeZoneId,it.expectedVersion) })
            }
        }
    }
}
