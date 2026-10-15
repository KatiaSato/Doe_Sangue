package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.RegistrationResult

import br.edu.fatec.doesangue.domain.model.BloodType

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.presentation.auth.SessionViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Valida a abertura no grafo real com sessão sintética, sem rede. */
@RunWith(AndroidJUnit4::class)
class SessionRestoreFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: NavHostController
    private lateinit var model: SessionViewModel
    private val auth = ControlledAuthRepository()

    private fun openApp() {
        val container = AppContainer(
            bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository(),
            campaignRepositoryOverride = FakeCampaignRepository(),
            centerRepositoryOverride = FakeDonationCenterRepository(),
            appointmentRepositoryOverride = FakeAppointmentRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            authRepositoryOverride = auth,
            donorProfileRepositoryOverride = FakeDonorProfileRepository(),
        )
        val sessionFactory = viewModelFactory {
            initializer { SessionViewModel(auth).also { model = it } }
        }
        compose.setContent {
            nav = rememberNavController()
            DoeSangueTheme {
                AppNavHost(
                    navController = nav,
                    schedulingViewModelFactory = container.schedulingViewModelFactory,
                    loginViewModelFactory = container.loginViewModelFactory,
                    profileViewModelFactory = container.profileViewModelFactory,
                    appointmentsViewModelFactory = container.appointmentsViewModelFactory,
                    appointmentDetailViewModelFactory = container::appointmentDetailViewModelFactory,
                    logoutViewModelFactory = container.logoutViewModelFactory,
                    sessionViewModelFactory = sessionFactory,
                    registrationViewModelFactory = container.registrationViewModelFactory,
                    campaignsViewModelFactory = container.campaignsViewModelFactory,
                    campaignDetailViewModelFactory = container::campaignDetailViewModelFactory,
                    homeViewModelFactory = isolatedHomeFactory(),
                    bloodStockViewModelFactory = container.bloodStockViewModelFactory,
                    donationHistoryViewModelFactory = container.donationHistoryViewModelFactory,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun finishSplashDelay() {
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
    }

    private fun assertHomeWithoutSplashHistory() {
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true)
            assertNull(nav.previousBackStackEntry)
        }
        compose.onNodeWithText("Já tenho uma conta").assertDoesNotExist()
    }

    private fun assertWelcomeWithoutSplashHistory() {
        compose.onNodeWithText("Já tenho uma conta").assertExists()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true)
            assertNull(nav.previousBackStackEntry)
        }
    }

    @Test
    fun restoredSessionOpensHomeAndCanSignOutNormally() {
        auth.answer = { Result.success(AuthenticatedUser("test-user-1")) }
        openApp()
        finishSplashDelay()
        assertHomeWithoutSplashHistory()
        compose.runOnIdle { assertEquals(1, auth.restoreCalls) }

        compose.onAllNodesWithText("Perfil").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Doador de teste").assertExists()
        compose.onNodeWithText("Configurações").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Sair da conta").performScrollTo().performClick()
        compose.waitForIdle()
        assertWelcomeWithoutSplashHistory()
        compose.onNodeWithText("Doador de teste").assertDoesNotExist()
        compose.runOnIdle { assertNull(auth.user) }
    }

    @Test
    fun missingSessionOpensWelcomeAndRemovesSplashHistory() {
        openApp()
        finishSplashDelay()
        assertWelcomeWithoutSplashHistory()
        compose.runOnIdle { assertEquals(1, auth.restoreCalls) }
    }

    @Test
    fun pendingRestorationWaitsAndIgnoresRepeatedRequests() {
        val response = CompletableDeferred<Result<AuthenticatedUser?>>()
        auth.answer = { response.await() }
        openApp()
        compose.mainClock.advanceTimeBy(10_000)
        compose.waitForIdle()
        compose.onNodeWithText("Verificando sua sessão...").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Splash>() == true)
            model.restoreSession()
            assertEquals(1, auth.restoreCalls)
            response.complete(Result.success(AuthenticatedUser("test-user-1")))
        }
        compose.waitForIdle()
        finishSplashDelay()
        assertHomeWithoutSplashHistory()
    }

    @Test
    fun failedRestorationShowsGenericErrorAndRetryCanRestoreSession() {
        auth.answer = { Result.failure(IllegalStateException("detalhe-interno-sintetico")) }
        openApp()
        compose.mainClock.advanceTimeBy(10_000)
        compose.waitForIdle()
        compose.onNodeWithText(ERROR_MESSAGE).assertIsDisplayed()
        compose.onNodeWithText("detalhe-interno-sintetico").assertDoesNotExist()
        compose.onNodeWithText("Tentar novamente").assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Splash>() == true)
            auth.answer = { Result.success(AuthenticatedUser("test-user-1")) }
        }
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.waitForIdle()
        finishSplashDelay()
        assertHomeWithoutSplashHistory()
        compose.runOnIdle { assertEquals(2, auth.restoreCalls) }
    }

    @Test
    fun pendingRetryHidesButtonAndSecondFailureAllowsAnotherAttempt() {
        auth.answer = { Result.failure(IllegalStateException("falha-sintetica")) }
        openApp()
        val response = CompletableDeferred<Result<AuthenticatedUser?>>()
        compose.runOnIdle { auth.answer = { response.await() } }
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
        compose.onNodeWithText(ERROR_MESSAGE).assertDoesNotExist()
        compose.onNodeWithText("Verificando sua sessão...").assertIsDisplayed()
        compose.runOnIdle {
            model.restoreSession()
            assertEquals(2, auth.restoreCalls)
            response.complete(Result.failure(IllegalStateException("outra-falha-sintetica")))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Tentar novamente").assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle { auth.answer = { Result.success(null) } }
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.waitForIdle()
        finishSplashDelay()
        assertWelcomeWithoutSplashHistory()
        compose.runOnIdle { assertEquals(3, auth.restoreCalls) }
    }

    private class ControlledAuthRepository : AuthRepository {
        override suspend fun signUp(displayName: String, email: String, password: String, bloodType: BloodType): Result<RegistrationResult> =
            Result.failure(UnsupportedOperationException("Cadastro não configurado neste roteiro."))

        var restoreCalls = 0
        var user: AuthenticatedUser? = null
        var answer: suspend () -> Result<AuthenticatedUser?> = { Result.success(null) }

        override suspend fun restoreSession(): Result<AuthenticatedUser?> {
            restoreCalls++
            return answer().also { result -> result.onSuccess { user = it } }
        }

        override suspend fun signIn(email: String, password: String): Result<AuthenticatedUser> =
            Result.failure(UnsupportedOperationException("Este teste não faz login."))

        override suspend fun signOut(): Result<Unit> {
            user = null
            return Result.success(Unit)
        }
    }

    private companion object {
        const val ERROR_MESSAGE = "Não foi possível verificar sua sessão. Tente novamente."
    }
}
