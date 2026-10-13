package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import java.time.Instant

// Responde em memória nos testes de navegação; não chama o Supabase.
class FakeAppointmentRepository : AppointmentRepository {
    private val appointmentsByKey = mutableMapOf<String, Appointment>()

    override suspend fun getMyAppointments(): Result<List<AppointmentSummary>> =
        Result.success(emptyList())

    override suspend fun createAppointment(
        timeSlotId: String,
        idempotencyKey: String,
    ): Result<Appointment> {
        val appointment = appointmentsByKey.getOrPut(idempotencyKey) {
            Appointment(
                id = "test-appointment-${appointmentsByKey.size + 1}",
                donorId = "test-user-1",
                timeSlotId = timeSlotId,
                status = AppointmentStatus.SCHEDULED,
                createdAt = Instant.parse("2026-10-12T12:00:00Z"),
            )
        }
        return Result.success(appointment)
    }
}
