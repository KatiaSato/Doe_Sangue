package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure

internal fun Throwable.toPasswordRecoveryErrorMessage(): String {
    val fallback =
        "Não foi possível solicitar a recuperação. Tente novamente."

    return when ((this as? AuthFailure)?.reason) {
        AuthError.EMAIL_NOT_AUTHORIZED ->
            "O envio de recuperação não está permitido para este endereço. Tente outro e-mail."

        AuthError.INVALID_EMAIL,
        AuthError.RATE_LIMITED,
        AuthError.NETWORK -> toAuthErrorMessage(fallback)

        else -> fallback
    }
}