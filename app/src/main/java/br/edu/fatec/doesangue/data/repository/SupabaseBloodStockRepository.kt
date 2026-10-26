package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.mapper.toDataFailure

import br.edu.fatec.doesangue.data.supabase.SupabaseBloodStockDataSource
import br.edu.fatec.doesangue.domain.model.BloodStock
import br.edu.fatec.doesangue.domain.repository.BloodStockRepository
import kotlinx.coroutines.CancellationException

class SupabaseBloodStockRepository(private val dataSource: SupabaseBloodStockDataSource) : BloodStockRepository {
    override suspend fun getBloodStocks(): Result<List<BloodStock>> = try {
        Result.success(dataSource.getBloodStocks())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception.toDataFailure())
    }
}
