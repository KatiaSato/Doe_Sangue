package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser

sealed interface RecoveryLinkUiState {
    data object Idle : RecoveryLinkUiState

    data object Processing : RecoveryLinkUiState

    data class Validated(
        val user: AuthenticatedUser,
        val validationId: Long,
    ) : RecoveryLinkUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = false,
    ) : RecoveryLinkUiState

    data class Leaving(val errorMessage: String? = null) : RecoveryLinkUiState
    data object Finished : RecoveryLinkUiState
}
