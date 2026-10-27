package br.edu.fatec.doesangue.domain.model

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Preferência de calendário; não define aptidão clínica nem cria reservas. */
data class RoutineChoice(val startsOn: LocalDate, val intervalDays: Int, val timeZoneId: String, val expectedVersion: Long) {
    fun validated() = apply {
        require(startsOn.year in 1900..9999 && intervalDays in 1..3650 && expectedVersion >= 0)
        ZoneId.of(timeZoneId)
    }
}

data class ReminderRoutine(
    val id: String, val ownerId: String, val startsOn: LocalDate, val intervalDays: Int,
    val timeZoneId: String, val active: Boolean, val version: Long, val originAppointmentId: String? = null,
) {
    fun nextDate(today: LocalDate): LocalDate? {
        if (!active) return null
        if (today <= startsOn) return startsOn
        val elapsed = ChronoUnit.DAYS.between(startsOn, today)
        val steps = (elapsed + intervalDays - 1) / intervalDays
        return runCatching { startsOn.plusDays(steps * intervalDays).takeIf { it.year <= 9999 } }.getOrNull()
    }
}
