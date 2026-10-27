package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.ReminderRoutineDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.ReminderRoutine
import br.edu.fatec.doesangue.domain.model.RoutineChoice
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseReminderRoutineDataSource(private val client: SupabaseClient) {
    suspend fun get(ownerId: String): ReminderRoutine? {
        val rows = client.ownedDataRpc(ownerId, "consultar_minha_rotina", buildJsonObject {}).decodeList<ReminderRoutineDto>()
        check(rows.size <= 1)
        return rows.singleOrNull()?.toDomain(ownerId)
    }

    suspend fun save(ownerId: String, choice: RoutineChoice): ReminderRoutine {
        choice.validated()
        val parameters = buildJsonObject {
            put("p_inicio", choice.startsOn.toString())
            put("p_intervalo", choice.intervalDays)
            put("p_fuso", choice.timeZoneId)
            put("p_versao", choice.expectedVersion)
        }
        return client.ownedDataRpc(ownerId, "salvar_minha_rotina", parameters)
            .decodeList<ReminderRoutineDto>().single().toDomain(ownerId)
    }

    suspend fun setActive(ownerId: String, active: Boolean, version: Long): ReminderRoutine {
        require(version > 0)
        val parameters = buildJsonObject {
            put("p_ativa", active)
            put("p_versao", version)
        }
        return client.ownedDataRpc(ownerId, "definir_minha_rotina_ativa", parameters)
            .decodeList<ReminderRoutineDto>().single().toDomain(ownerId)
    }

    suspend fun delete(ownerId: String, version: Long) {
        require(version > 0)
        val result = client.ownedDataRpc(ownerId, "excluir_minha_rotina", buildJsonObject { put("p_versao", version) })
        check(result.decodeAs<Boolean>())
    }
}
