package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.ReminderRoutine
import br.edu.fatec.doesangue.domain.model.RoutineChoice

interface ReminderRoutineRepository {
    suspend fun get(ownerId: String): Result<ReminderRoutine?>
    suspend fun save(ownerId: String, choice: RoutineChoice): Result<ReminderRoutine>
    suspend fun setActive(ownerId: String, active: Boolean, version: Long): Result<ReminderRoutine>
    suspend fun delete(ownerId: String, version: Long): Result<Unit>
}
