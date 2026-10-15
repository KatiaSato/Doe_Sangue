package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.RegistrationResult

import br.edu.fatec.doesangue.domain.model.BloodType

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.presentation.auth.LoginViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercita tela, ViewModel e navegação reais, sem rede ou credenciais reais. */
@RunWith(AndroidJUnit4::class)
class LoginFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: NavHostController
    private lateinit var model: LoginViewModel
    private val repository = ControlledAuthRepository()

    private fun openLogin() {
        val container = AppContainer(
            campaignRepositoryOverride = FakeCampaignRepository(),
            centerRepositoryOverride = FakeDonationCenterRepository(),
            appointmentRepositoryOverride = FakeAppointmentRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            authRepositoryOverride = repository,
            donorProfileRepositoryOverride = FakeDonorProfileRepository(),
        )
        val factory = viewModelFactory {
            initializer { LoginViewModel(repository).also { model = it } }
        }
        compose.setContent {
            nav = rememberNavController()
            DoeSangueTheme {
                AppNavHost(
                    navController = nav,
                    schedulingViewModelFactory = container.schedulingViewModelFactory,
                    loginViewModelFactory = factory,
                    profileViewModelFactory = container.profileViewModelFactory,
                    appointmentsViewModelFactory = container.appointmentsViewModelFactory,
                    appointmentDetailViewModelFactory = container::appointmentDetailViewModelFactory,
                    logoutViewModelFactory = container.logoutViewModelFactory,
                    sessionViewModelFactory = container.sessionViewModelFactory,
                    registrationViewModelFactory = container.registrationViewModelFactory,
                    campaignsViewModelFactory = container.campaignsViewModelFactory,
                    campaignDetailViewModelFactory = container::campaignDetailViewModelFactory,
                )
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onNodeWithText("Já tenho uma conta").performClick()
    }

    @Test
    fun missingEmailOrPasswordDoesNotCallRepository() {
        openLogin()
        submit()
        assertMissingFields()

        compose.onNodeWithText("E-mail").performTextInput("teste@example.com")
        submit()
        assertMissingFields()

        compose.onNodeWithText("E-mail").performTextReplacement("   ")
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        submit()
        assertMissingFields()
    }

    @Test
    fun failureStaysOnLoginAndAllowsRetryWithoutExposingTechnicalDetails() {
        repository.answer = { Result.failure(IllegalStateException("detalhe-interno-sintetico")) }
        openLogin()
        fillCredentials()
        submit()

        compose.onNodeWithText("Não foi possível entrar. Tente novamente.")
            .assertExists()
        compose.onNodeWithText("detalhe-interno-sintetico").assertDoesNotExist()
        compose.onNodeWithText("Entrar").assertIsEnabled()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Login>() == true)
            assertFalse(model.uiState.value.isLoading)
            assertNull(model.uiState.value.authenticatedUser)
            assertEquals(1, repository.calls)
            assertTrue(repository.receivedExpectedCredentials)
            repository.answer = { Result.success(AuthenticatedUser("test-user-1")) }
        }
        submit()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true)
            assertEquals(2, repository.calls)
            assertNull(model.uiState.value.errorMessage)
        }
    }

    @Test
    fun pendingLoginDisablesFieldsAndIgnoresRepeatedRequests() {
        // A resposta só chega quando o teste a libera; não depende de atrasos da rede.
        val response = CompletableDeferred<Result<AuthenticatedUser>>()
        repository.answer = { response.await() }
        openLogin()
        fillCredentials()
        submit()

        compose.onNodeWithText("Entrando...").assertIsNotEnabled()
        compose.onNodeWithText("E-mail").assertIsNotEnabled()
        compose.onNodeWithText("Senha").assertIsNotEnabled()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Login>() == true)
            model.signIn("outro@example.com", "outra-senha-ficticia")
            assertEquals(1, repository.calls)
            response.complete(Result.success(AuthenticatedUser("test-user-1")))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true)
            assertFalse(model.uiState.value.isLoading)
            assertEquals("test-user-1", model.uiState.value.authenticatedUser?.id)
            model.signIn("outro@example.com", "outra-senha-ficticia")
            assertEquals(1, repository.calls)
        }
    }

    @Test
    fun invalidEmailFormatDoesNotCallRepository() {
        openLogin()
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        for (email in listOf("sem-arroba", "doador@", "doador@unidade", "doador teste@example.com")) {
            compose.onNodeWithText("E-mail").performTextReplacement(email)
            submit()
            compose.onNodeWithText("Informe um e-mail válido.").assertExists()
            compose.runOnIdle {
                assertEquals(0, repository.calls)
                assertFalse(model.uiState.value.isLoading)
                assertNull(model.uiState.value.authenticatedUser)
            }
        }
    }

    @Test
    fun knownFailuresShowSpecificMessagesAndAllowSuccessfulRetry() {
        openLogin()
        fillCredentials()
        val cases = listOf(
            AuthError.INVALID_EMAIL to "Este e-mail não é aceito. Confira o endereço ou informe outro e-mail válido.",
            AuthError.EMAIL_NOT_CONFIRMED to "Confirme seu e-mail antes de entrar. Verifique sua caixa de entrada e o spam.",
            AuthError.INVALID_CREDENTIALS to "E-mail ou senha incorretos.",
            AuthError.RATE_LIMITED to "Muitas tentativas. Aguarde um pouco e tente novamente.",
            AuthError.NETWORK to "Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.",
        )
        cases.forEachIndexed { index, (reason, message) ->
            compose.runOnIdle { repository.answer = { Result.failure(AuthFailure(reason)) } }
            submit()
            compose.onNodeWithText(message).assertExists()
            compose.onNodeWithText("Entrar").assertIsEnabled()
            compose.runOnIdle {
                assertEquals(index + 1, repository.calls)
                assertTrue(repository.receivedExpectedCredentials)
                assertFalse(model.uiState.value.isLoading)
                assertNull(model.uiState.value.authenticatedUser)
                assertTrue(nav.currentDestination?.hasRoute<AppRoute.Login>() == true)
            }
        }
        compose.runOnIdle { repository.answer = { Result.success(AuthenticatedUser("test-user-1")) } }
        submit()
        compose.runOnIdle {
            assertEquals(cases.size + 1, repository.calls)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true)
            assertNull(model.uiState.value.errorMessage)
        }
    }

    private fun fillCredentials() {
        compose.onNodeWithText("E-mail").performTextInput("  teste@example.com  ")
        compose.onNodeWithText("Senha").performTextInput(" senha-ficticia ")
    }

    private fun submit() {
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun assertMissingFields() {
        compose.onNodeWithText("Preencha o e-mail e a senha.").assertExists()
        compose.runOnIdle {
            assertEquals(0, repository.calls)
            assertFalse(model.uiState.value.isLoading)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Login>() == true)
        }
    }

    private class ControlledAuthRepository : AuthRepository {
        override suspend fun signUp(displayName: String, email: String, password: String, bloodType: BloodType): Result<RegistrationResult> =
            Result.failure(UnsupportedOperationException("Cadastro não configurado neste roteiro."))

        var calls = 0
        var receivedExpectedCredentials = false
        var answer: suspend () -> Result<AuthenticatedUser> = {
            Result.success(AuthenticatedUser("test-user-1"))
        }

        override suspend fun signIn(email: String, password: String): Result<AuthenticatedUser> {
            calls++
            // Confere trim somente do e-mail; a senha deve chegar sem alterações.
            receivedExpectedCredentials = email == "teste@example.com" && password == " senha-ficticia "
            return answer()
        }

        override suspend fun signOut(): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun restoreSession(): Result<AuthenticatedUser?> {
            return Result.success(null)
        }
    }
}
