package br.edu.fatec.doesangue.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.Instant

class ReminderRoutineTest {
    private val start = LocalDate.of(2026,10,26)
    private val routine = ReminderRoutine("1","synthetic",start,7,"America/Sao_Paulo",true,1)
    @Test fun nextDateIncludesTodayAndAdvancesWithoutPastOccurrences() {
        assertEquals(start,routine.nextDate(start.minusDays(1)))
        assertEquals(start,routine.nextDate(start))
        assertEquals(start.plusDays(7),routine.nextDate(start.plusDays(1)))
        assertEquals(start.plusDays(7),routine.nextDate(start.plusDays(7)))
        assertEquals(start.plusDays(14),routine.nextDate(start.plusDays(8)))
    }
    @Test fun pausedAndOverflowDoNotInventADate() {
        assertNull(routine.copy(active=false).nextDate(start))
        assertNull(routine.copy(startsOn=LocalDate.of(9999,12,30),intervalDays=7).nextDate(LocalDate.of(9999,12,31)))
    }
    @Test fun calendarRepeatsAcrossLeapDayAndYearBoundary() {
        val leap=routine.copy(startsOn=LocalDate.of(2028,2,28),intervalDays=2)
        assertEquals(LocalDate.of(2028,3,1),leap.nextDate(LocalDate.of(2028,2,29)))
        assertEquals(LocalDate.of(2027,1,2),routine.copy(startsOn=LocalDate.of(2026,12,31),intervalDays=2).nextDate(LocalDate.of(2027,1,1)))
    }
    @Test fun choiceRejectsInvalidTechnicalLimitsAndZones() {
        for(choice in listOf(RoutineChoice(start,0,"UTC",0),RoutineChoice(start,3651,"UTC",0),
            RoutineChoice(start,7,"invalid",0),RoutineChoice(start,7,"UTC",-1))) assertTrue(runCatching {choice.validated()}.isFailure)
        RoutineChoice(start,1,"UTC",0).validated();RoutineChoice(start,3650,"America/Sao_Paulo",10).validated()
    }
    @Test fun durableOperationsHaveExclusiveFingerprints() {
        val origin=RescheduleOrigin("23","1","Unidade Sintética",Instant.parse("2026-11-19T11:00:00Z"),"America/Sao_Paulo")
        val draft=SchedulingDraft("synthetic","1","Unidade Sintética","America/Sao_Paulo",
            "2026-11-20","7","2026-11-20T11:00:00Z",SchedulingStep.CONFIRM,"00000000-0000-4000-8000-000000000099")
        draft.copy(rescheduleOrigin=origin).validated()
        draft.copy(routine=RoutineChoice(start,7,"UTC",0)).validated()
        assertTrue(runCatching {draft.copy(rescheduleOrigin=origin,routine=RoutineChoice(start,7,"UTC",0)).validated()}.isFailure)
        assertTrue(runCatching {draft.copy(rescheduleOrigin=origin.copy(appointmentId="other")).validated()}.isFailure)
    }
}
