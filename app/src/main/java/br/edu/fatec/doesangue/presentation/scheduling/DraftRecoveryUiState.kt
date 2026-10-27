package br.edu.fatec.doesangue.presentation.scheduling

import br.edu.fatec.doesangue.domain.model.SchedulingDraft
import br.edu.fatec.doesangue.domain.model.SchedulingStep

enum class DraftRecoveryPhase { LOADING, OFFER, RESTORING, READY, ERROR }
data class DraftRecoveryUiState(
    val phase: DraftRecoveryPhase = DraftRecoveryPhase.READY,
    val draft: SchedulingDraft? = null,
    val message: String? = null,
    val canDiscard: Boolean = false,
    val resumeStep: SchedulingStep? = null,
    val preserveEntryIntent: Boolean = false,
)
