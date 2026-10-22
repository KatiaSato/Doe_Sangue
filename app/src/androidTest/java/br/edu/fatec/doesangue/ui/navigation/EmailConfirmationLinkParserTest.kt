package br.edu.fatec.doesangue.ui.navigation

import android.net.Uri
import br.edu.fatec.doesangue.domain.model.EmailConfirmationCredential
import org.junit.Assert.*
import org.junit.Test

class EmailConfirmationLinkParserTest {
    private val base = "br.edu.fatec.doesangue://auth/confirmation"
    @Test fun acceptsOnlySignupCodeOrBearerFromResendAndRedactsDescriptions() {
        val code = confirmationCredentialFromUri(Uri.parse("$base?code=synthetic%2Bcode"))
        assertEquals(EmailConfirmationCredential.Code("synthetic+code"), code)
        val token = confirmationCredentialFromUri(Uri.parse("$base#access_token=synthetic-token&refresh_token=ignored&type=signup&token_type=bearer&expires_in=3600"))
        assertEquals(EmailConfirmationCredential.AccessToken("synthetic-token"), token)
        assertFalse(token.toString().contains("synthetic-token"))
        assertFalse(code.toString().contains("synthetic+code"))
    }
    @Test fun rejectsAliasesDuplicatedMixedMissingAndRecoveryCredentials() {
        listOf("https://auth/confirmation?code=x", "br.edu.fatec.doesangue://auth:443/confirmation?code=x",
            "br.edu.fatec.doesangue://user@auth/confirmation?code=x", "br.edu.fatec.doesangue://auth/recovery?code=x",
            "br.edu.fatec.doesangue://auth/%63onfirmation?code=x", "$base/extra?code=x",
            base, "$base?code=", "$base?code=a&code=b", "$base?code=%20x", "$base?code=x#",
            "$base?code=x&error=denied", "$base#error_code=otp_expired",
            "$base#access_token=x&token_type=bearer&type=recovery",
            "$base#access_token=x&access_token=y&token_type=bearer&type=signup",
            "$base#access_token=x&type=signup", "$base#access_token=x&token_type=bearer&type=signup&code=x"
        ).forEach { assertNull(it, confirmationCredentialFromUri(Uri.parse(it))) }
    }
}
