package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.presentation.auth.*
import br.edu.fatec.doesangue.ui.screens.auth.*
import br.edu.fatec.doesangue.ui.screens.profile.SecurityScreen
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercita a entrada OnAutofillText do Compose; não usa Google, rede ou dados reais. */
@RunWith(AndroidJUnit4::class)
class AuthAutofillTest {
    @get:Rule val compose = createComposeRule()
    private val email = "autofill@example.com"
    private val password = "SyntheticNew1!"

    private fun field(label: String, type: ContentType? = null): SemanticsNodeInteraction {
        val node = compose.onNodeWithText(label)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentType))
        // ContentType composto não implementa igualdade por valor no Compose 1.9.5.
        // Senhas usam as constantes individuais; e-mails são exercitados pela ação real de entrada.
        return if (type == null) node else node.assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentType, type),
        )
    }

    private fun SemanticsNodeInteraction.fill(value: String) =
        performSemanticsAction(SemanticsActions.OnAutofillText) { it(AnnotatedString(value)) }

    private fun tap(label: String) =
        compose.onNodeWithText(label).performScrollTo().performClick()

    @Test fun loginReceivesSavedCredentialsAndKeepsPasswordTypeWhenVisible() {
        var submitted: Pair<String, String>? = null
        compose.setContent { DoeSangueTheme {
            LoginScreen(LoginUiState(), { mail, pass -> submitted = mail to pass }, {}, {})
        } }
        field("E-mail").fill(email)
        field("Senha", ContentType.Password).fill(password)
        tap("Mostrar")
        field("Senha", ContentType.Password).assertTextContains(password)
        tap("Entrar")
        compose.runOnIdle { assertEquals(email to password, submitted) }
    }

    @Test fun registrationEmailAutofillsTheIdentityDraft() {
        var continuedEmail: String? = null
        compose.setContent { DoeSangueTheme {
            var state by remember { mutableStateOf(RegistrationUiState()) }
            RegisterStep1Screen(state, { state = state.copy(displayName = it) },
                { state = state.copy(email = it) }, {}, { continuedEmail = state.email }, {})
        } }
        field("E-mail", ContentType.EmailAddress).fill(email)
        tap("Continuar")
        compose.runOnIdle { assertEquals(email, continuedEmail) }
    }

    @Test fun registrationAssociatesAccountEmailWithTwoNewPasswords() {
        var submitted: Pair<String, String>? = null
        var submittedEmail: String? = null
        compose.setContent { DoeSangueTheme {
            var state by remember { mutableStateOf(RegistrationUiState(email = email)) }
            RegisterStep4Screen(state, {},
                { pass, confirmation -> submitted = pass to confirmation; submittedEmail = state.email },
                { state = state.copy(email = it) })
        } }
        field("E-mail da conta")
            .assertTextContains(email)
            .fill("nova-conta@example.com")
        field("Senha", ContentType.NewPassword).fill(password)
        field("Confirmar senha", ContentType.NewPassword).fill(password)
        tap("Criar minha conta")
        compose.runOnIdle {
            assertEquals(password to password, submitted)
            assertEquals("nova-conta@example.com", submittedEmail)
        }
    }

    @Test fun recoveryAndResendUseAutofilledEmailWithoutSubmittingOnFill() {
        var request: String? = null
        var showResend by mutableStateOf(false)
        compose.setContent { DoeSangueTheme {
            var mail by remember { mutableStateOf("") }
            if (showResend) {
                EmailVerificationScreen(EmailConfirmationUiState(email = mail),
                    { mail = it }, { request = mail }, {})
            } else {
                ForgotPasswordScreen(PasswordRecoveryUiState(email = mail),
                    { mail = it }, { request = mail }, {})
            }
        } }
        field("E-mail").fill(email)
        compose.runOnIdle { assertNull(request) }
        tap("Enviar link")
        compose.runOnIdle { assertEquals(email, request); request = null; showResend = true }
        field("E-mail").fill("outro@example.com")
        compose.runOnIdle { assertNull(request) }
        tap("Reenviar confirmação")
        compose.runOnIdle { assertEquals("outro@example.com", request) }
    }

    @Test fun resetReceivesGeneratedPasswordAndClearsItAfterSuccess() {
        var state by mutableStateOf(ResetPasswordUiState())
        var submitted: Pair<String, String>? = null
        var finished = false
        compose.setContent { DoeSangueTheme {
            ResetPasswordScreen(state, { pass, confirmation -> submitted = pass to confirmation },
                { finished = true })
        } }
        field("Nova senha", ContentType.NewPassword).fill(password)
        field("Confirmar nova senha", ContentType.NewPassword).fill(password)
        tap("Salvar nova senha")
        compose.runOnIdle { assertEquals(password to password, submitted); state = state.copy(passwordChanged = true) }
        compose.waitUntil { finished }
        compose.onNodeWithText("Nova senha").assertTextEquals("Nova senha", "")
        compose.onNodeWithText("Confirmar nova senha").assertTextEquals("Confirmar nova senha", "")
    }

    @Test fun securityKeepsCurrentAndGeneratedPasswordsSeparate() {
        var submitted: List<String>? = null
        compose.setContent { DoeSangueTheme {
            SecurityScreen(ChangePasswordUiState(),
                { current, new, confirmation -> submitted = listOf(current, new, confirmation) }, {})
        } }
        field("Senha atual", ContentType.Password).fill("SyntheticOld1!")
        field("Nova senha", ContentType.NewPassword).fill(password)
        field("Confirmar nova senha", ContentType.NewPassword).fill(password)
        tap("Alterar senha")
        compose.runOnIdle { assertEquals(listOf("SyntheticOld1!", password, password), submitted) }
    }
}
