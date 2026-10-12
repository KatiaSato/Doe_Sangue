package br.edu.fatec.doesangue.domain.model

import java.time.Instant

// Representa um agendamento já persistido no banco.
data class Appointment(
    val id: String,
    val donorId: String,
    val timeSlotId: String,
    val status: AppointmentStatus,
    val createdAt: Instant,
)