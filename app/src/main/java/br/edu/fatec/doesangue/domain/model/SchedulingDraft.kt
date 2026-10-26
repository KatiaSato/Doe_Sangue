package br.edu.fatec.doesangue.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

enum class SchedulingStep { CENTER, DATE, RECURRENCE, CONFIRM }

/** Escolhas e identidade do pedido; nenhum segredo da autenticação. */
data class SchedulingDraft(
    val ownerId: String,
    val centerId: String,
    val centerName: String,
    val timeZoneId: String,
    val dateIso: String? = null,
    val slotId: String? = null,
    val startsAtIso: String? = null,
    val step: SchedulingStep = SchedulingStep.CENTER,
    val requestKey: String? = null,
) {
    fun validated(): SchedulingDraft = apply {
        require(ownerId.isNotBlank() && ownerId.length <= 160)
        require(centerId.isNotBlank() && centerId.length <= 160)
        require(centerName.isNotBlank() && centerName.length <= 512)
        require(timeZoneId.length <= 100)
        val zone = ZoneId.of(timeZoneId)
        val date = dateIso?.let(LocalDate::parse)
        require(date == null || date.year in 1..9999)
        require((slotId == null) == (startsAtIso == null))
        slotId?.let { require(it.isNotBlank() && it.length <= 160 && date != null) }
        startsAtIso?.let { require(Instant.parse(it).atZone(zone).toLocalDate() == date) }
        requestKey?.let { require(slotId != null && UUID.fromString(it).toString() == it) }
    }
}

class InvalidSchedulingDraft : Exception()
