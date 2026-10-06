package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.SupabaseDonationCenterDataSource
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import kotlinx.coroutines.CancellationException

// Implementa a consulta às unidades usando a fonte Supabase.
class SupabaseDonationCenterRepository(
    private val dataSource: SupabaseDonationCenterDataSource,
) : DonationCenterRepository {

    override suspend fun getCenters(): Result<List<DonationCenter>> {
        return try {
            val centers = dataSource.getCenters()
            Result.success(centers)
        } catch (exception: CancellationException) {
            // Permite que o cancelamento da corrotina continue.
            throw exception
        } catch (exception: Exception) {
            // Devolve a falha para quem solicitou a consulta.
            Result.failure(exception)
        }
    }
}
