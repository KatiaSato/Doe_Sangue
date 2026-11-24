package br.edu.fatec.doesangue.domain.notifications

import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import java.time.Duration
import java.time.Instant

internal fun AppointmentSummary.appointmentReminderTime(): Instant? {
    if (appointment.status != AppointmentStatus.SCHEDULED) return null
    val reminderTime = startsAt.minus(Duration.ofHours(24))
    return reminderTime.takeIf { appointment.createdAt <= it }
}
