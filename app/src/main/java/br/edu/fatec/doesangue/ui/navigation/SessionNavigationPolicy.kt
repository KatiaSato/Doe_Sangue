package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.presentation.auth.SessionMonitorUiState

internal enum class SessionNavigationAction { NONE, WAIT, BIND_OWNER, LOGIN, WELCOME, LOGIN_CHANGED_ACCOUNT }

/** Decisão pura; o NavController permanece somente na integração Compose. */
internal fun sessionNavigationAction(
    privateArea: Boolean,
    recoveryActive: Boolean,
    ownerId: String?,
    state: SessionMonitorUiState?,
): SessionNavigationAction {
    if (!privateArea || recoveryActive || state == null) return SessionNavigationAction.NONE
    val user = when (state) {
        SessionMonitorUiState.Checking -> return SessionNavigationAction.WAIT
        is SessionMonitorUiState.Ended -> return if (state.explicitSignOut) SessionNavigationAction.WELCOME else SessionNavigationAction.LOGIN
        is SessionMonitorUiState.Available -> state.user
        is SessionMonitorUiState.Reconnecting -> state.lastVerifiedUser ?: return SessionNavigationAction.WAIT
    }
    return when {
        ownerId == null -> SessionNavigationAction.BIND_OWNER
        ownerId != user.id -> SessionNavigationAction.LOGIN_CHANGED_ACCOUNT
        else -> SessionNavigationAction.NONE
    }
}

internal fun SessionMonitorUiState?.permitsLoginUser(userId: String): Boolean =
    this == null || this is SessionMonitorUiState.Available && user.id == userId
