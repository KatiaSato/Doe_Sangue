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
        compose.onNodeWithText("Criar minha conta").assertIsEnabled()
        compose.runOnIdle { auth.answer = { Result.success(RegistrationResult(null)) } }
        tap("Criar minha conta")
        compose.runOnIdle {
            assertEquals(2, auth.calls)
            assertEquals(BloodType.A_POSITIVE, auth.lastType)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.EmailVerification>() == true)
        }
    }

    private class RegistrationAuth : AuthRepository {
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
        override suspend fun restoreSession() = Result.success(user)
        override suspend fun signIn(email: String, password: String) = Result.failure<AuthenticatedUser>(UnsupportedOperationException())
        override suspend fun signOut() = Result.success(Unit).also { user = null }
    }
}
