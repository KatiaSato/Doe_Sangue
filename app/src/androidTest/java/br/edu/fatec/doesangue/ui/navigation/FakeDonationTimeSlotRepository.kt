package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.DonationTimeSlot
import br.edu.fatec.doesangue.domain.repository.DonationTimeSlotRepository
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

// Fornece horários previsíveis para os testes de navegação.
class FakeDonationTimeSlotRepository : DonationTimeSlotRepository {

    override suspend fun getTimeSlots(
        centerId: String,
        fromInclusive: Instant,
        untilExclusive: Instant,
    ): Result<List<DonationTimeSlot>> {
        if (centerId != "test-center-1") {
            return Result.success(emptyList())
        }

        // Mesmo fuso da unidade fictícia usada nos testes.
        val zone = ZoneId.of("America/Sao_Paulo")
        val date = fromInclusive.atZone(zone).toLocalDate()

        val slots = listOf(
            "08:00", "09:30", "11:00",
            "14:00", "15:30", "17:00",
        ).map { time ->
            val startsAt = date
                .atTime(LocalTime.parse(time))
                .atZone(zone)
                .toInstant()

            DonationTimeSlot(
                id = "test-slot-$startsAt",
                centerId = centerId,
                startsAt = startsAt,
                capacity = 3,
            )
        }.filter { slot ->
            slot.startsAt >= fromInclusive &&
                    slot.startsAt < untilExclusive
        }

        return Result.success(slots)
    }
}
