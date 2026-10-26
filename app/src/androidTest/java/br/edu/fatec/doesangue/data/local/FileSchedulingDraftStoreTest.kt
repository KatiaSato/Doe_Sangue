package br.edu.fatec.doesangue.data.local

import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class FileSchedulingDraftStoreTest {
    private val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
        "t02-store-${UUID.randomUUID()}")
    private fun store() = FileSchedulingDraftStore(directory)
    private val draft = SchedulingDraft("synthetic-a", "1", "Unidade Sintética", "America/Sao_Paulo",
        "2026-11-20", "7", "2026-11-20T11:00:00Z", SchedulingStep.CONFIRM,
        "00000000-0000-4000-8000-000000000099")
    @After fun close() { directory.deleteRecursively() }
    @Test fun newInstanceReadsExactlyTheSamePreparedRequestWithoutSecrets() = runBlocking<Unit> {
        store().save(draft).getOrThrow()
        assertEquals(draft, store().read(draft.ownerId).getOrThrow())
        val text = directory.listFiles()!!.single().readText()
        assertFalse(text.contains("access_token")); assertFalse(text.contains("password")); assertFalse(text.contains("email"))
        assertFalse(directory.listFiles()!!.single().name.contains(draft.ownerId))
    }
    @Test fun accountsAreIsolatedAndClearingOneDoesNotRemoveAnother() = runBlocking<Unit> {
        val store = store(); store.save(draft).getOrThrow()
        val other = draft.copy(ownerId = "synthetic-b", requestKey = null)
        store.save(other).getOrThrow(); store.clear(draft.ownerId).getOrThrow()
        assertNull(store.read(draft.ownerId).getOrThrow())
        assertEquals(other, store.read(other.ownerId).getOrThrow())
    }
    @Test fun corruptUnsupportedOversizedOrWrongOwnerContentDoesNotBecomeARequest() = runBlocking<Unit> {
        val store = store(); store.save(draft).getOrThrow()
        val file = directory.listFiles()!!.single()
        val valid = file.readText()
        for (text in listOf("{", valid.replace("\"version\":1", "\"version\":9"),
            valid.replace("synthetic-a", "synthetic-b"), "x".repeat(17_000))) {
            file.writeText(text)
            assertTrue(store.read(draft.ownerId).exceptionOrNull() is InvalidSchedulingDraft)
        }
    }
    @Test fun backupRecoveryAndSerializedWritesReturnWholeSnapshots() = runBlocking<Unit> {
        val store = store(); store.save(draft).getOrThrow()
        val file = directory.listFiles()!!.single()
        file.renameTo(File(file.path + ".bak"))
        assertEquals(draft, store.read(draft.ownerId).getOrThrow())
        val variants = (1..12).map { draft.copy(centerName = "Unidade Sintética $it") }
        val jobs = variants.map { async { store().save(it).getOrThrow() } }
        repeat(20) {
            val current = store().read(draft.ownerId).getOrThrow()
            assertTrue(current == draft || current in variants)
            yield()
        }
        jobs.awaitAll()
        assertTrue(store.read(draft.ownerId).getOrThrow() in variants)
    }
    @Test fun failedDirectoryOrInvalidSnapshotReturnsFailure() = runBlocking<Unit> {
        directory.writeText("synthetic obstruction")
        assertTrue(store().save(draft).isFailure)
        directory.delete()
        assertTrue(store().save(draft.copy(requestKey = "bad")).isFailure)
        assertNull(store().read(draft.ownerId).getOrThrow())
    }
}
