package br.edu.fatec.doesangue.domain.model

import java.time.Instant

// Representa um horário oferecido por uma unidade de coleta.
data class DonationTimeSlot(
    val id: String,
    val centerId: String,
    val startsAt: Instant,
    val capacity: Int,
)