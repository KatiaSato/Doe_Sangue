package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.mapper.toDataFailure

import br.edu.fatec.doesangue.data.supabase.SupabaseDonationHistoryDataSource
import br.edu.fatec.doesangue.domain.model.DonationRecord
import br.edu.fatec.doesangue.domain.repository.DonationHistoryRepository
import kotlinx.coroutines.CancellationException

class SupabaseDonationHistoryRepository(private val dataSource: SupabaseDonationHistoryDataSource) : DonationHistoryRepository {
    override suspend fun getMyDonations(): Result<List<DonationRecord>> = try {
        Result.success(dataSource.getMyDonations())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception.toDataFailure())
    }
}
