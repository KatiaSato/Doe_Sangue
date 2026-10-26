package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser

sealed interface SessionMonitorUiState {
    data object Checking : SessionMonitorUiState
    data class Available(val user: AuthenticatedUser) : SessionMonitorUiState
    data class Reconnecting(val lastVerifiedUser: AuthenticatedUser?) : SessionMonitorUiState
    data class Ended(val explicitSignOut: Boolean) : SessionMonitorUiState
}
