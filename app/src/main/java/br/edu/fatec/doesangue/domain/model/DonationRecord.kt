package br.edu.fatec.doesangue.domain.model

import java.time.Instant

// Registro de demonstração publicado administrativamente, independente de reservas.
data class DonationRecord(
    val id: String,
    val centerName: String,
    val occurredAt: Instant,
    val timeZoneId: String,
    val source: String,
)
