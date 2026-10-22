package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.RegistrationResult
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.presentation.auth.RegistrationViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cadastro, sessão e navegação com dados sintéticos em memória; não envia e-mail. */
@RunWith(AndroidJUnit4::class)
class RegistrationFlowTest {
    @get:Rule val compose = createComposeRule()
    private val auth = RegistrationAuth()
    private lateinit var nav: NavHostController
    private lateinit var model: RegistrationViewModel

    private fun openApp() {
        val container = AppContainer(
            bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository(),
            authRepositoryOverride = auth, donorProfileRepositoryOverride = FakeDonorProfileRepository(),
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            appointmentRepositoryOverride = FakeAppointmentRepository(),
            campaignRepositoryOverride = FakeCampaignRepository(),
        )
        val factory = viewModelFactory { initializer { RegistrationViewModel(auth).also { model = it } } }
        compose.setContent {
            nav = rememberNavController()
            DoeSangueTheme { FeatureTestHost(container, nav, registrationFactory = factory) }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true }
        tap("Começar")
    }

    private fun tap(text: String) {
        val node = compose.onNodeWithText(text)
        runCatching { node.assertIsDisplayed() }.onFailure { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun identity() {
        compose.onNodeWithText("Nome de exibição").performTextInput("  Doador Sintético  ")
        compose.onNodeWithText("E-mail").performTextInput("cadastro@example.com")
        tap("Continuar")
    }

    private fun passwordStep(type: String) {
        identity(); tap(type); tap("Continuar"); tap("Continuar")
    }

    private fun passwords(value: String = "Sintetica123!", confirmation: String = value) {
        compose.onNodeWithText("Senha").performTextReplacement(value)
        compose.onNodeWithText("Confirmar senha").performTextReplacement(confirmation)
    }

    @Test fun identityAndExplicitBloodTypeChoiceAreRequired() {
        openApp(); tap("Continuar")
        compose.onNodeWithText("Informe seu nome.").assertExists()
        compose.runOnIdle { assertEquals(0, auth.calls) }
        compose.onNodeWithText("Nome de exibição").performTextInput("Doador")
        compose.onNodeWithText("E-mail").performTextInput("invalido")
        tap("Continuar")
        compose.onNodeWithText("Informe um e-mail válido.").assertExists()
        compose.onNodeWithText("E-mail").performTextReplacement("cadastro@example.com")
        tap("Continuar")
        compose.onNodeWithText("Continuar").assertIsNotEnabled()
        tap("Não sei")
        compose.onNodeWithText("Não sei").assertIsSelected()
        tap("Continuar"); tap("‹")
        compose.onNodeWithText("Não sei").assertIsSelected()
        compose.runOnIdle { assertEquals(0, auth.calls) }
    }

    @Test fun lastStepEmailRemainsValidatedAndIsUsedForTheNewAccount() {
        openApp(); passwordStep("Não sei")
        compose.onNodeWithText("E-mail da conta").assertTextContains("cadastro@example.com")
            .performTextReplacement("invalido")
        passwords(); tap("Criar minha conta")
        compose.onNodeWithText("Informe um e-mail válido.").assertExists()
        compose.runOnIdle { assertEquals(0, auth.calls) }
        compose.onNodeWithText("E-mail da conta").performTextReplacement("nova-conta@example.com")
        tap("Criar minha conta")
        compose.runOnIdle {
            assertEquals(1, auth.calls)
            assertEquals("nova-conta@example.com", auth.lastEmail)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.EmailVerification>() == true)
        }
    }

    @Test fun unknownTypeIsSentAndPendingConfirmationDoesNotEnterMain() {
        openApp(); passwordStep("Não sei")
        passwords("fraca", "fraca"); tap("Criar minha conta")
        compose.onNodeWithText(RegistrationViewModel.PASSWORD_RULE).assertExists()
        passwords(confirmation = "Diferente123!"); tap("Criar minha conta")
        compose.onNodeWithText("As senhas devem ser iguais.").assertExists()
        compose.runOnIdle { assertEquals(0, auth.calls) }
        passwords(); tap("Criar minha conta")
        compose.runOnIdle {
            assertEquals(BloodType.UNKNOWN, auth.lastType)
            assertEquals("Doador Sintético", auth.lastName)
            assertEquals("cadastro@example.com", auth.lastEmail)
            assertEquals(1, auth.calls)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.EmailVerification>() == true)
            assertNull(auth.user)
        }
        tap("Entendi")
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Login>() == true)
            assertTrue(nav.previousBackStackEntry?.destination?.hasRoute<AppRoute.Welcome>() == true)
        }
    }

    @Test fun authenticatedSignUpEntersMainWithoutRegistrationHistory() {
        auth.answer = { Result.success(RegistrationResult(AuthenticatedUser("registered-user"))) }
        openApp(); passwordStep("O-"); passwords(); tap("Criar minha conta")
        compose.runOnIdle {
            assertEquals(BloodType.O_NEGATIVE, auth.lastType)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true)
            assertNull(nav.previousBackStackEntry)
        }
    }

    @Test fun pendingRequestBlocksDuplicateAndBackAndFailureAllowsRetry() {
        val response = CompletableDeferred<Result<RegistrationResult>>()
        auth.answer = { response.await() }
        openApp(); passwordStep("A+"); passwords(); tap("Criar minha conta")
        compose.onNodeWithText("Criando conta…").assertIsNotEnabled()
        compose.onNodeWithText("Senha").assertIsNotEnabled()
        compose.onNodeWithText("‹").assertDoesNotExist()
        pressBack(); compose.waitForIdle()
        compose.runOnIdle {
            repeat(3) { model.register("Sintetica123!", "Sintetica123!") }
            model.selectBloodType(BloodType.B_NEGATIVE)
            assertEquals(1, auth.calls)
            assertEquals(BloodType.A_POSITIVE, model.uiState.value.bloodType)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Register4>() == true)
            response.complete(Result.failure(IllegalStateException("falha-interna-sintetica")))
        }
        compose.waitForIdle()
        compose.onNodeWithText("falha-interna-sintetica").assertDoesNotExist()
        compose.onNodeWithText("Não foi possível concluir o cadastro. Tente novamente.").assertExists()
        compose.onNodeWithText("Criar minha conta").assertIsEnabled()
        compose.runOnIdle { auth.answer = { Result.success(RegistrationResult(null)) } }
        tap("Criar minha conta")
        compose.runOnIdle {
            assertEquals(2, auth.calls)
            assertEquals(BloodType.A_POSITIVE, auth.lastType)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.EmailVerification>() == true)
        }
    }

    @Test fun knownFailuresPreserveRegistrationAndAllowRetry() {
        openApp(); passwordStep("Não sei"); passwords()
        val cases = listOf(
            AuthError.INVALID_EMAIL to "Este e-mail não é aceito. Confira o endereço ou informe outro e-mail válido.",
            AuthError.ACCOUNT_ALREADY_EXISTS to "Já existe uma conta com este e-mail.",
            AuthError.WEAK_PASSWORD to "Escolha uma senha mais forte e confira os requisitos.",
            AuthError.RATE_LIMITED to "Muitas tentativas. Aguarde um pouco e tente novamente.",
            AuthError.EMAIL_NOT_AUTHORIZED to "O envio de confirmação não está permitido para este endereço. Tente outro e-mail.",
            AuthError.NETWORK to "Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.",
        )
        cases.forEachIndexed { index, (reason, message) ->
            compose.runOnIdle { auth.answer = { Result.failure(AuthFailure(reason)) } }
            tap("Criar minha conta")
            compose.onNodeWithText(message).assertExists()
            compose.onNodeWithText("Criar minha conta").assertIsEnabled()
            compose.runOnIdle {
                assertEquals(index + 1, auth.calls)
                assertFalse(model.uiState.value.isLoading)
                assertNull(model.uiState.value.result)
                assertEquals("  Doador Sintético  ", model.uiState.value.displayName)
                assertEquals("cadastro@example.com", model.uiState.value.email)
                assertEquals(BloodType.UNKNOWN, model.uiState.value.bloodType)
                assertTrue(nav.currentDestination?.hasRoute<AppRoute.Register4>() == true)
            }
        }
        compose.runOnIdle { auth.answer = { Result.success(RegistrationResult(null)) } }
        tap("Criar minha conta")
        compose.runOnIdle {
            assertEquals(cases.size + 1, auth.calls)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.EmailVerification>() == true)
        }
    }

    private class RegistrationAuth : AuthRepository {
    override suspend fun resendEmailConfirmation(email: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Confirmação não configurada neste teste."))
    override suspend fun verifyEmailConfirmation(credential: br.edu.fatec.doesangue.domain.model.EmailConfirmationCredential): Result<Unit> =
        Result.failure(UnsupportedOperationException("Confirmação não configurada neste teste."))

        var user: AuthenticatedUser? = null
        var calls = 0
        var lastType: BloodType? = null
        var lastName = ""
        var lastEmail = ""
        var answer: suspend () -> Result<RegistrationResult> = { Result.success(RegistrationResult(null)) }
        override suspend fun signUp(displayName: String, email: String, password: String, bloodType: BloodType): Result<RegistrationResult> {
            calls++; lastType = bloodType; lastName = displayName; lastEmail = email
            return answer().onSuccess { user = it.authenticatedUser }
        }
        // Recuperação exige resposta explícita nos testes desse fluxo.
        override suspend fun exchangeRecoveryCode(code: String): Result<AuthenticatedUser> =
            Result.failure(UnsupportedOperationException("Retorno de recuperação não configurado neste teste."))

        override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Alteração de senha não configurada neste teste."))

        override suspend fun updatePassword(password: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Atualização de senha não configurada neste teste."))

        override suspend fun requestPasswordReset(email: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Recuperação não configurada neste teste."))

        override suspend fun restoreSession() = Result.success(user)
        override suspend fun signIn(email: String, password: String) = Result.failure<AuthenticatedUser>(UnsupportedOperationException())
        override suspend fun signOut() = Result.success(Unit).also { user = null }
    }
}
