package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.SupabaseDonationTimeSlotDataSource
import br.edu.fatec.doesangue.domain.model.DonationTimeSlot
import br.edu.fatec.doesangue.domain.repository.DonationTimeSlotRepository
import kotlinx.coroutines.CancellationException
import java.time.Instant

// Implementa o contrato de consulta dos horários usando o Supabase.
class SupabaseDonationTimeSlotRepository(
    private val dataSource: SupabaseDonationTimeSlotDataSource,
) : DonationTimeSlotRepository {

    override suspend fun getTimeSlots(
        centerId: String,
        fromInclusive: Instant,
        untilExclusive: Instant,
    ): Result<List<DonationTimeSlot>> {
        return try {
            val slots = dataSource.getTimeSlots(
                centerId = centerId,
                fromInclusive = fromInclusive,
                untilExclusive = untilExclusive,
            )

            Result.success(slots)
        } catch (exception: CancellationException) {
            // Preserva o cancelamento da corrotina.
            throw exception
        } catch (exception: Exception) {
            // Comunica a falha para quem solicitou a consulta.
            Result.failure(exception)
        }
    }
}