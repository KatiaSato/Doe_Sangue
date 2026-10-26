package br.edu.fatec.doesangue.domain.model

/** Ciclo da sessão no cliente; não transporta credenciais nem tipos do SDK. */
sealed interface SessionPresence {
    data object Initializing : SessionPresence
    data class Authenticated(val user: AuthenticatedUser) : SessionPresence
    data object RefreshPending : SessionPresence
    data class NotAuthenticated(val explicitSignOut: Boolean = false) : SessionPresence
}
