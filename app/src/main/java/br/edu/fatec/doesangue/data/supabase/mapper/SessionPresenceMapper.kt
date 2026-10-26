package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.SessionPresence
import io.github.jan.supabase.auth.status.SessionStatus

internal fun SessionStatus.toSessionPresence(explicitSignOut: Boolean): SessionPresence = when (this) {
    SessionStatus.Initializing -> SessionPresence.Initializing
    is SessionStatus.Authenticated -> session.user?.id?.takeIf(String::isNotBlank)
        ?.let { SessionPresence.Authenticated(AuthenticatedUser(it)) }
        ?: SessionPresence.NotAuthenticated()
    is SessionStatus.RefreshFailure -> SessionPresence.RefreshPending
    // isSignOut também é true quando o SDK limpa um refresh rejeitado.
    is SessionStatus.NotAuthenticated -> SessionPresence.NotAuthenticated(explicitSignOut)
}
