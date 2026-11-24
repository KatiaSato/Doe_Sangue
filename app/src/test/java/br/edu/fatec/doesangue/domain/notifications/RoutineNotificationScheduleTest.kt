package br.edu.fatec.doesangue.domain.notifications

import br.edu.fatec.doesangue.domain.model.ReminderRoutine
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineNotificationScheduleTest {
    private val routine = ReminderRoutine(
        id = "routine",
        ownerId = "owner",
        startsOn = LocalDate.parse("2026-11-23"),
        intervalDays = 3,
        timeZoneId = "America/Sao_Paulo",
        active = true,
        version = 1,
    )
    private val time = LocalTime.of(9, 0)

    @Test
    fun schedulesTodayWhenLocalTimeIsStillAhead() {
        assertEquals(
            Instant.parse("2026-11-23T12:00:00Z"),
            nextRoutineNotification(routine, time, clockAt("2026-11-23T11:59:00Z"))?.toInstant(),
        )
    }

    @Test
    fun keepsCurrentOccurrenceAtExactScheduledInstant() {
        assertEquals(
            Instant.parse("2026-11-23T12:00:00Z"),
            nextRoutineNotification(routine, time, clockAt("2026-11-23T12:00:00Z"))?.toInstant(),
        )
    }

    @Test
    fun skipsPastTimeAndPreservesTheRoutineInterval() {
        assertEquals(
            Instant.parse("2026-11-26T12:00:00Z"),
            nextRoutineNotification(routine, time, clockAt("2026-11-23T12:00:01Z"))?.toInstant(),
        )
    }

    @Test
    fun usesRoutineLocalDateEvenWhenUtcIsAlreadyOnTheNextDay() {
        assertEquals(
            Instant.parse("2026-11-24T02:00:00Z"),
            nextRoutineNotification(
                routine, LocalTime.of(23, 0), clockAt("2026-11-24T01:00:00Z"),
            )?.toInstant(),
        )
    }

    @Test
    fun movesNonexistentLocalTimeForwardByTheDaylightSavingGap() {
        val summerRoutine = routine.copy(
            startsOn = LocalDate.parse("2026-03-08"),
            timeZoneId = "America/New_York",
        )
        assertEquals(
            Instant.parse("2026-03-08T07:30:00Z"),
            nextRoutineNotification(
                summerRoutine, LocalTime.of(2, 30), clockAt("2026-03-08T05:00:00Z"),
            )?.toInstant(),
        )
    }

    @Test
    fun pausedRoutineHasNoScheduledNotification() {
        assertNull(nextRoutineNotification(routine.copy(active = false), time, clockAt("2026-11-23T11:00:00Z")))
    }

    private fun clockAt(instant: String): Clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
}
