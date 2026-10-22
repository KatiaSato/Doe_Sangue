package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthErrorMessagesTest {
    @Test fun passwordReuseAndUnavailableSessionHaveActionableMessages() {
        assertEquals("Escolha uma senha diferente da atual.",
            AuthFailure(AuthError.SAME_PASSWORD).toAuthErrorMessage("fallback"))
        assertEquals("Sua sessão não está disponível. Entre novamente para continuar.",
            AuthFailure(AuthError.SESSION_UNAVAILABLE).toAuthErrorMessage("fallback"))
    }

    @Test fun invalidCredentialsDoNotDistinguishMissingUserFromWrongPassword() {
        assertEquals("E-mail ou senha incorretos.",
            AuthFailure(AuthError.INVALID_CREDENTIALS).toAuthErrorMessage("fallback"))
    }

    @Test fun changingPasswordDistinguishesIncorrectCurrentPasswordFromUnavailableSession() {
        assertEquals("Senha atual incorreta.",
            AuthFailure(AuthError.CURRENT_PASSWORD_INCORRECT).toAuthErrorMessage("fallback"))
        assertEquals("Sua sessão não está disponível. Entre novamente para continuar.",
            AuthFailure(AuthError.SESSION_UNAVAILABLE).toAuthErrorMessage("fallback"))
    }

    @Test fun confirmationIsDifferentFromInvalidEmailAndDoesNotPromiseResend() {
        val confirmation = AuthFailure(AuthError.EMAIL_NOT_CONFIRMED).toAuthErrorMessage("fallback")
        val invalidEmail = AuthFailure(AuthError.INVALID_EMAIL).toAuthErrorMessage("fallback")
        assertNotEquals(invalidEmail, confirmation)
        assertTrue(confirmation.contains("Confirme seu e-mail"))
        assertFalse(confirmation.contains("reenviar", ignoreCase = true))
    }

    @Test fun knownCategoriesHaveDistinctSpecificMessages() {
        val messages = AuthError.entries.filter { it != AuthError.UNKNOWN }.map {
            AuthFailure(it).toAuthErrorMessage("fallback-sintetico")
        }
        assertEquals(messages.size, messages.toSet().size)
        assertTrue(messages.all { it.isNotBlank() && it != "fallback-sintetico" })
    }

    @Test fun unknownAndUnclassifiedFailuresUseOperationFallbackWithoutRawDetails() {
        val loginFallback = "Não foi possível entrar. Tente novamente."
        val registrationFallback = "Não foi possível concluir o cadastro. Tente novamente."
        val rawFailure = IllegalStateException("detalhe-interno-sintetico email_exists")
        assertEquals(loginFallback, rawFailure.toAuthErrorMessage(loginFallback))
        assertEquals(registrationFallback,
            AuthFailure(AuthError.UNKNOWN).toAuthErrorMessage(registrationFallback))
    }
}
