package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.DonationTimeSlot
import java.time.Instant

// Define a consulta dos horários oferecidos por unidade e período.
interface DonationTimeSlotRepository {
    suspend fun getTimeSlots(
        centerId: String,        // Unidade que queremos consultar
        fromInclusive: Instant,  // Inicio do período - incluído na busca
        untilExclusive: Instant, // Fim do Período - excluído da busca
    ): Result<List<DonationTimeSlot>>
}