package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure

// Essa função apresenta erros no contexto de validar o link,
// sem confundi-los com falhas de envio do e-mail ou de alteração da senha
internal fun Throwable.toRecoveryLinkErrorMessage(): String {
    val fallback =
        "Não foi possível validar o link. Tente novamente."

    return when ((this as? AuthFailure)?.reason) {
        AuthError.SESSION_UNAVAILABLE ->
            "Não foi possível validar seu acesso. Solicite um novo link de recuperação."

        AuthError.RATE_LIMITED,
        AuthError.NETWORK -> toAuthErrorMessage(fallback)

        else -> fallback
    }
}