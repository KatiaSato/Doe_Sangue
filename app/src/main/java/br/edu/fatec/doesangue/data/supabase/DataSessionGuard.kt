package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.domain.model.DataError
import br.edu.fatec.doesangue.domain.model.DataFailure
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession

/** Espera a inicialização; renovação pendente não significa saída. */
internal suspend fun SupabaseClient.requireDataUser(): UserInfo {
    return checkNotNull(requireDataSession().user)
}

internal suspend fun SupabaseClient.requireDataSession(): UserSession {
    auth.awaitInitialization()
    return when (val status = auth.sessionStatus.value) {
        is SessionStatus.Authenticated -> status.session.takeIf { !it.user?.id.isNullOrBlank() && it.accessToken.isNotBlank() }
            ?: throw DataFailure(DataError.SESSION_UNAVAILABLE)
        is SessionStatus.RefreshFailure -> throw DataFailure(DataError.SESSION_RETRYING)
        else -> throw DataFailure(DataError.SESSION_UNAVAILABLE)
    }
}
