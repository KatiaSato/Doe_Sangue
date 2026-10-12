package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.Appointment

// Define a criação de um agendamento para a conta autenticada.
interface AppointmentRepository {
    suspend fun createAppointment(
        timeSlotId: String,
        idempotencyKey: String,
    ): Result<Appointment>
}