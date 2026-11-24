package br.edu.fatec.doesangue.domain.notifications

import br.edu.fatec.doesangue.domain.model.ReminderRoutine
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal fun nextRoutineNotification(
    routine: ReminderRoutine,
    time: LocalTime,
    clock: Clock,
): ZonedDateTime? {
    if (!routine.active) return null

    val now = clock.instant()
    val zone = ZoneId.of(routine.timeZoneId)
    val today = now.atZone(zone).toLocalDate()
    val date = routine.nextDate(today) ?: return null
    val scheduledAt = date.atTime(time).atZone(zone)
    if (!scheduledAt.toInstant().isBefore(now)) return scheduledAt

    return routine.nextDate(date.plusDays(1))?.atTime(time)?.atZone(zone)
}
