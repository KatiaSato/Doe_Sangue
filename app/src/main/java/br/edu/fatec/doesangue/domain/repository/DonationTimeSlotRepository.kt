package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.DonationTimeSlot
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

// Define a consulta dos horários oferecidos por unidade e período.
interface DonationTimeSlotRepository {
    suspend fun getAvailableDates(centerId: String, month: YearMonth): Result<Set<LocalDate>>

    suspend fun getTimeSlots(
        centerId: String,        // Unidade que queremos consultar
        fromInclusive: Instant,  // Inicio do período - incluído na busca
        untilExclusive: Instant, // Fim do Período - excluído da busca
    ): Result<List<DonationTimeSlot>>
}
