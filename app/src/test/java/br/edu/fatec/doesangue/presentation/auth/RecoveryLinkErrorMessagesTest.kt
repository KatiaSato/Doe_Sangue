package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class RecoveryLinkErrorMessagesTest {
    @Test fun unavailableSessionRequestsNewLink() {
        assertEquals(
            "Não foi possível validar seu acesso. Solicite um novo link de recuperação.",
            AuthFailure(AuthError.SESSION_UNAVAILABLE).toRecoveryLinkErrorMessage(),
        )
    }

    @Test fun connectionAndRateLimitHaveSpecificGuidance() {
        assertEquals(
            "Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.",
            AuthFailure(AuthError.NETWORK).toRecoveryLinkErrorMessage(),
        )
        assertEquals("Muitas tentativas. Aguarde um pouco e tente novamente.",
            AuthFailure(AuthError.RATE_LIMITED).toRecoveryLinkErrorMessage())
    }

    @Test fun unknownOrUnrelatedFailuresKeepLinkContextAndHideRawDetails() {
        listOf(
            IllegalStateException("detalhe-interno-ficticio"),
            AuthFailure(AuthError.UNKNOWN),
            AuthFailure(AuthError.INVALID_CREDENTIALS),
            AuthFailure(AuthError.SAME_PASSWORD),
        ).forEach {
            assertEquals("Não foi possível validar o link. Tente novamente.", it.toRecoveryLinkErrorMessage())
        }
    }
}
