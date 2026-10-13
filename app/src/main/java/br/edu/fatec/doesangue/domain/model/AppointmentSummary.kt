package br.edu.fatec.doesangue.domain.model

import java.time.Instant

// Reúne os dados necessários para listar uma reserva.
data class AppointmentSummary(
    val appointment: Appointment,
    val centerName: String,
    val startsAt: Instant,
    val timeZoneId: String,
)