package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.SupabaseReminderRoutineDataSource
import br.edu.fatec.doesangue.data.supabase.mapper.toAppointmentFailure
import br.edu.fatec.doesangue.domain.model.ReminderRoutine
import br.edu.fatec.doesangue.domain.model.RoutineChoice
import br.edu.fatec.doesangue.domain.repository.ReminderRoutineRepository
import kotlinx.coroutines.CancellationException

class SupabaseReminderRoutineRepository(private val dataSource: SupabaseReminderRoutineDataSource) : ReminderRoutineRepository {
    private suspend fun <T> result(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure.toAppointmentFailure())
    }

    override suspend fun get(ownerId: String): Result<ReminderRoutine?> = result { dataSource.get(ownerId) }
    override suspend fun save(ownerId: String, choice: RoutineChoice): Result<ReminderRoutine> = result { dataSource.save(ownerId, choice) }
    override suspend fun setActive(ownerId: String, active: Boolean, version: Long): Result<ReminderRoutine> = result { dataSource.setActive(ownerId, active, version) }
    override suspend fun delete(ownerId: String, version: Long): Result<Unit> = result { dataSource.delete(ownerId, version) }
}
