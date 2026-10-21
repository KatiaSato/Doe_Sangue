package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class ResetPasswordErrorMessagesTest {
    @Test fun knownUpdateFailuresKeepActionableMessages() {
        val cases = mapOf(
            AuthError.WEAK_PASSWORD to "Escolha uma senha mais forte e confira os requisitos.",
            AuthError.SAME_PASSWORD to "Escolha uma senha diferente da atual.",
            AuthError.RATE_LIMITED to "Muitas tentativas. Aguarde um pouco e tente novamente.",
            AuthError.NETWORK to
                "Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.",
        )
        cases.forEach { (reason, expected) ->
            assertEquals(expected, AuthFailure(reason).toResetPasswordErrorMessage())
        }
    }

    @Test fun unavailableSessionRequestsNewRecoveryLink() {
        assertEquals(
            "Não foi possível validar seu acesso. Solicite um novo link de recuperação.",
            AuthFailure(AuthError.SESSION_UNAVAILABLE).toResetPasswordErrorMessage(),
        )
    }

    @Test fun unknownAndUnrelatedFailuresDoNotExposeInternalDetailsOrLoginMessages() {
        val fallback = "Não foi possível alterar sua senha. Tente novamente."
        listOf(
            AuthFailure(AuthError.UNKNOWN),
            AuthFailure(AuthError.INVALID_CREDENTIALS),
            IllegalStateException("detalhe-interno-ficticio"),
        ).forEach { assertEquals(fallback, it.toResetPasswordErrorMessage()) }
    }
}
