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
import androidx.test.espresso.Espresso.pressBack
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import br.edu.fatec.doesangue.presentation.auth.LogoutViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Testa a saída pelo grafo real; sessões e perfis são sintéticos, sem rede. */
@RunWith(AndroidJUnit4::class)
class LogoutFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: NavHostController
    private lateinit var model: LogoutViewModel
    private val auth = ControlledAuthRepository()

    private fun openSettings() {
        val profiles = object : DonorProfileRepository {
        override suspend fun updateCurrentProfile(
        displayName: String,
        bloodType: BloodType,
        phone: String?,
        birthDate: java.time.LocalDate?,
    ): Result<DonorProfile> = error("Atualização completa não configurada neste teste.")
    override suspend fun updateCurrentBloodType(bloodType: BloodType): Result<DonorProfile> =
            Result.failure(UnsupportedOperationException("Edição do tipo não configurada neste roteiro."))

            override suspend fun getCurrentProfile(): Result<DonorProfile?> = Result.success(
                if (auth.user?.id == "test-user-a") DonorProfile("test-user-a", "Doador Demo A") else null,
            )

            override suspend fun createCurrentProfile(displayName: String): Result<DonorProfile> =
                Result.failure(UnsupportedOperationException("Este teste não cria perfis."))
        }
        val container = AppContainer(
            bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository(),
            campaignRepositoryOverride = FakeCampaignRepository(),
            centerRepositoryOverride = FakeDonationCenterRepository(),
            appointmentRepositoryOverride = FakeAppointmentRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            authRepositoryOverride = auth,
            donorProfileRepositoryOverride = profiles,
        )
        val logoutFactory = viewModelFactory {
            initializer { LogoutViewModel(auth).also { model = it } }
        }
        compose.setContent {
            nav = rememberNavController()
            DoeSangueTheme {
                AppNavHost(
                    recoveryLinkState = idleRecoveryLinkState,
                    navController = nav,
                    schedulingViewModelFactory = container.schedulingViewModelFactory,
                    loginViewModelFactory = container.loginViewModelFactory,
                    profileViewModelFactory = container.profileViewModelFactory,
                    appointmentsViewModelFactory = container.appointmentsViewModelFactory,
                    appointmentDetailViewModelFactory = container::appointmentDetailViewModelFactory,
                    logoutViewModelFactory = logoutFactory,
                    sessionViewModelFactory = container.sessionViewModelFactory,
                    registrationViewModelFactory = container.registrationViewModelFactory,
                    campaignsViewModelFactory = container.campaignsViewModelFactory,
                    campaignDetailViewModelFactory = container::campaignDetailViewModelFactory,
                    homeViewModelFactory = isolatedHomeFactory(),
                    bloodStockViewModelFactory = container.bloodStockViewModelFactory,
                    donationHistoryViewModelFactory = container.donationHistoryViewModelFactory,
                    centersViewModelFactory = container.centersViewModelFactory,
                    centerDetailViewModelFactory = container::centerDetailViewModelFactory,
                    passwordRecoveryViewModelFactory = container.passwordRecoveryViewModelFactory,
                    resetPasswordViewModelFactory = { container.resetPasswordViewModelFactory },
                    changePasswordViewModelFactory = container.changePasswordViewModelFactory,
                    editProfileViewModelFactory = container.editProfileViewModelFactory,
                )
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        login("doador.a@example.com")
        openProfile()
        compose.onNodeWithText("Doador Demo A").assertExists()
        tap("Configurações")
    }

    private fun login(email: String) {
        compose.onNodeWithText("Já tenho uma conta").performClick()
        compose.onNodeWithText("E-mail").performTextInput(email)
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        tap("Entrar")
    }

    private fun openProfile() {
        compose.onAllNodesWithText("Perfil").onLast().performClick()
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun assertSignedOut() {
        compose.runOnIdle {
            assertNull(auth.user)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true)
            assertNull(nav.previousBackStackEntry)
        }
        compose.onNodeWithText("Doador Demo A").assertDoesNotExist()
    }

    @Test
    fun successRemovesMainHistoryAndAllowsAnotherLoginAndLogout() {
        openSettings()
        tap("Sair da conta")
        assertSignedOut()
        login("doador.a@example.com")
        openProfile()
        compose.onNodeWithText("Doador Demo A").assertExists()
        tap("Configurações")
        tap("Sair da conta")
        assertSignedOut()
        compose.runOnIdle { assertEquals(2, auth.signOutCalls) }
    }

    @Test
    fun failureKeepsSessionAndAllowsRetryWithoutTechnicalMessage() {
        auth.signOutAnswer = { Result.failure(IllegalStateException("detalhe-interno-sintetico")) }
        openSettings()
        tap("Sair da conta")
        compose.onNodeWithText("Não foi possível sair. Tente novamente.").assertExists()
        compose.onNodeWithText("detalhe-interno-sintetico").assertDoesNotExist()
        compose.onNodeWithText("Sair da conta").assertIsEnabled()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Settings>() == true)
            assertEquals("test-user-a", auth.user?.id)
            auth.signOutAnswer = { Result.success(Unit) }
        }
        tap("Sair da conta")
        assertSignedOut()
        compose.runOnIdle { assertEquals(2, auth.signOutCalls) }
    }

    @Test
    fun pendingLogoutDisablesButtonAndIgnoresRepeatedRequests() {
        val response = CompletableDeferred<Result<Unit>>()
        auth.signOutAnswer = { response.await() }
        openSettings()
        tap("Sair da conta")
        compose.onNodeWithText("Saindo...").assertIsNotEnabled()
        compose.runOnIdle {
            model.signOut()
            assertEquals(1, auth.signOutCalls)
            assertEquals("test-user-a", auth.user?.id)
            response.complete(Result.success(Unit))
        }
        compose.waitForIdle()
        assertSignedOut()
    }

    @Test
    fun secondAccountDoesNotDisplayPreviousAccountsProfile() {
        openSettings()
        tap("Sair da conta")
        assertSignedOut()
        login("doador.b@example.com")
        openProfile()
        compose.onNodeWithText("Você ainda não criou seu perfil.").assertExists()
        compose.onNodeWithText("Doador Demo A").assertDoesNotExist()
        compose.runOnIdle { assertEquals("test-user-b", auth.user?.id) }
    }

    @Test
    fun pendingLogoutCannotLeaveAuthenticatedScreensVisibleAfterCompletion() {
        val response = CompletableDeferred<Result<Unit>>()
        auth.signOutAnswer = { response.await() }
        openSettings()
        tap("Sair da conta")
        // As ações visíveis não devem abandonar a tela durante a saída.
        compose.onNodeWithText("Sobre o app").assertIsNotEnabled()
        tap("Sobre o app")
        compose.onNodeWithText("‹").assertDoesNotExist()
        compose.onNodeWithText("Início").performClick()
        compose.onNodeWithText("Agendar").performClick()
        compose.waitForIdle()
        pressBack()
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Settings>() == true)
            assertEquals("test-user-a", auth.user?.id)
        }
        compose.runOnIdle { response.complete(Result.success(Unit)) }
        compose.waitForIdle()
        assertSignedOut()
    }

    private class ControlledAuthRepository : AuthRepository {
        override suspend fun signUp(displayName: String, email: String, password: String, bloodType: BloodType): Result<RegistrationResult> =
            Result.failure(UnsupportedOperationException("Cadastro não configurado neste roteiro."))

        var user: AuthenticatedUser? = null
        var signOutCalls = 0
        var signOutAnswer: suspend () -> Result<Unit> = { Result.success(Unit) }

        override suspend fun signIn(email: String, password: String): Result<AuthenticatedUser> {
            user = AuthenticatedUser(if (email == "doador.a@example.com") "test-user-a" else "test-user-b")
            return Result.success(checkNotNull(user))
        }

        override suspend fun signOut(): Result<Unit> {
            signOutCalls++
            return signOutAnswer().also { result -> result.onSuccess { user = null } }
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

        override suspend fun restoreSession(): Result<AuthenticatedUser?> {
            return Result.success(user)
        }
    }
}
