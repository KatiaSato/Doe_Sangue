package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure

internal fun Throwable.toResetPasswordErrorMessage(): String {
    val fallback =
        "Não foi possível alterar sua senha. Tente novamente."

    return when ((this as? AuthFailure)?.reason) {
        AuthError.SESSION_UNAVAILABLE ->
            "Não foi possível validar seu acesso. Solicite um novo link de recuperação."

        AuthError.WEAK_PASSWORD,
        AuthError.SAME_PASSWORD,
        AuthError.RATE_LIMITED,
        AuthError.NETWORK -> toAuthErrorMessage(fallback)

        else -> fallback
    }
}