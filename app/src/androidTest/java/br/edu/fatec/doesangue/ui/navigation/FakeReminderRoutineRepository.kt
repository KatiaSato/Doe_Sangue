package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.ReminderRoutineRepository

/** Sempre em memória; nem constrói cliente Supabase. */
class FakeReminderRoutineRepository : ReminderRoutineRepository {
    val routines = mutableMapOf<String,ReminderRoutine>()
    var calls = 0
    var failure: Throwable? = null
    private var version = 0L
    override suspend fun get(ownerId: String): Result<ReminderRoutine?> { calls++; return failure?.let { Result.failure(it) } ?: Result.success(routines[ownerId]) }
    override suspend fun save(ownerId: String,choice: RoutineChoice): Result<ReminderRoutine> {
        calls++; failure?.let { return Result.failure(it) }
        val current = routines[ownerId]
        if (choice.expectedVersion != (current?.version ?: 0L)) return Result.failure(DataFailure(DataError.ROUTINE_CHANGED))
        val next = ReminderRoutine(current?.id ?: "1",ownerId,choice.startsOn,choice.intervalDays,choice.timeZoneId,current?.active ?: true,++version)
        routines[ownerId]=next;return Result.success(next)
    }
    override suspend fun setActive(ownerId: String,active: Boolean,version: Long): Result<ReminderRoutine> {
        calls++; failure?.let { return Result.failure(it) }
        val next = routines.getValue(ownerId).copy(active=active,version=++this.version)
        routines[ownerId]=next;return Result.success(next)
    }
    override suspend fun delete(ownerId: String,version: Long): Result<Unit> {
        calls++; failure?.let { return Result.failure(it) }; routines.remove(ownerId);return Result.success(Unit)
    }
}
