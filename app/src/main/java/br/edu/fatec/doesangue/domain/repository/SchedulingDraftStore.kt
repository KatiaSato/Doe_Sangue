package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.SchedulingDraft

interface SchedulingDraftStore {
    suspend fun read(ownerId: String): Result<SchedulingDraft?>
    suspend fun save(draft: SchedulingDraft): Result<Unit>
    suspend fun clear(ownerId: String): Result<Unit>
}
