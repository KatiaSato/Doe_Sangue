package br.edu.fatec.doesangue.domain.model

import org.junit.Assert.*
import org.junit.Test

class SchedulingDraftTest {
    private val draft = SchedulingDraft("synthetic-a", "1", "Unidade Sintética", "America/Sao_Paulo",
        "2026-11-20", "7", "2026-11-20T11:00:00Z", SchedulingStep.CONFIRM,
        "00000000-0000-4000-8000-000000000099")
    @Test fun acceptsCompletePreparedRequestAndUnsentPartialChoices() {
        assertEquals(draft, draft.validated())
        assertNotNull(draft.copy(slotId = null, startsAtIso = null, requestKey = null).validated())
        assertNotNull(draft.copy(dateIso = null, slotId = null, startsAtIso = null, requestKey = null).validated())
    }
    @Test fun rejectsIncoherentDatesSlotsAndRequestKeys() {
        for (bad in listOf(draft.copy(dateIso = "2026-02-30"), draft.copy(dateIso = "2026-11-21"),
            draft.copy(slotId = null), draft.copy(startsAtIso = null), draft.copy(requestKey = "bad"),
            draft.copy(timeZoneId = "invalid-zone"), draft.copy(ownerId = ""), draft.copy(centerName = ""))) {
            assertTrue(runCatching { bad.validated() }.isFailure)
        }
    }
}
