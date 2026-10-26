package br.edu.fatec.doesangue.domain.model

import java.time.Instant

// Representa um horário oferecido por uma unidade de coleta.
data class DonationTimeSlot(
    val id: String,
    val centerId: String,
    val startsAt: Instant,
    val capacity: Int,
    val availableSpots: Int = capacity,
) {
    init {
        require(capacity > 0 && availableSpots in 0..capacity) { "Disponibilidade inválida." }
    }
}
