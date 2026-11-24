package br.edu.fatec.doesangue.domain.notifications

import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppointmentNotificationScheduleTest {
    private val start = Instant.parse("2026-11-27T12:00:00Z")
    private val reminder = Instant.parse("2026-11-26T12:00:00Z")

    @Test
    fun schedulesExactly24HoursBeforeConfirmedStart() {
        assertEquals(reminder, appointment(reminder.minusSeconds(1)).appointmentReminderTime())
        assertEquals(reminder, appointment(reminder).appointmentReminderTime())
    }

    @Test
    fun recentBookingDoesNotQualify() {
        assertNull(appointment(reminder.plusSeconds(1)).appointmentReminderTime())
    }

    @Test
    fun cancelledBookingDoesNotQualify() {
        assertNull(appointment(reminder, AppointmentStatus.CANCELLED).appointmentReminderTime())
    }

    private fun appointment(createdAt: Instant, status: AppointmentStatus = AppointmentStatus.SCHEDULED) =
        AppointmentSummary(Appointment("id", "owner", "slot", status, createdAt), "Unidade", start, "America/Sao_Paulo")
}
