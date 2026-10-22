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
import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import br.edu.fatec.doesangue.presentation.appointments.AppointmentsViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Exercita a navegação, o ViewModel e a tela reais com dados sintéticos, sem rede. */
@RunWith(AndroidJUnit4::class)
class AppointmentListFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private val auth = ControlledAuthRepository()
    private val repository = ControlledAppointmentRepository(auth)
    private lateinit var nav: NavHostController
    private lateinit var model: AppointmentsViewModel

    private fun openApp() {
        val profiles = object : DonorProfileRepository {
        override suspend fun updateCurrentProfile(
        displayName: String,
        bloodType: BloodType,
        phone: String?,
        birthDate: java.time.LocalDate?,
    ): Result<DonorProfile> = error("Atualização completa não configurada neste teste.")
    override suspend fun updateCurrentBloodType(bloodType: BloodType): Result<DonorProfile> =
            Result.failure(UnsupportedOperationException("Edição do tipo não configurada neste roteiro."))

            override suspend fun getCurrentProfile(): Result<DonorProfile?> =
                Result.success(auth.user?.let { DonorProfile(it.id, "Doador de teste") })

            override suspend fun createCurrentProfile(displayName: String): Result<DonorProfile> =
                Result.failure(UnsupportedOperationException("Este teste não cria perfis."))
        }
        val container = AppContainer(
            bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository(),
            campaignRepositoryOverride = FakeCampaignRepository(),
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            authRepositoryOverride = auth,
            donorProfileRepositoryOverride = profiles,
            appointmentRepositoryOverride = repository,
        )
        val factory = viewModelFactory {
            initializer { AppointmentsViewModel(repository).also { model = it } }
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
                    appointmentsViewModelFactory = factory,
                    appointmentDetailViewModelFactory = container::appointmentDetailViewModelFactory,
                    logoutViewModelFactory = container.logoutViewModelFactory,
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
        compose.waitUntil(10_000) {
            nav.currentDestination?.hasRoute<AppRoute.Home>() == true
        }
        compose.waitForIdle()
    }

    private fun openList() {
        tapTab("Doações")
        tap("Agendamentos")
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.DonationsAppointments>() == true)
        }
    }

    private fun tap(text: String) {
        val node = compose.onNodeWithText(text)
        if (text in listOf("Entrar", "Configurações", "Sair da conta", "Tentar novamente")) {
            node.performScrollTo()
        }
        node.performClick()
        compose.waitForIdle()
    }

    private fun tapTab(text: String) {
        compose.onAllNodesWithText(text).onLast().performClick()
        compose.waitForIdle()
    }

    private fun login(email: String) {
        tap("Já tenho uma conta")
        compose.onNodeWithText("E-mail").performTextInput(email)
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        tap("Entrar")
    }

    @Test
    fun pendingQueryShowsLoadingAndPreventsSimultaneousRequests() {
        val response = CompletableDeferred<Result<List<AppointmentSummary>>>()
        repository.answer = { response.await() }
        openApp()
        openList()
        compose.onNodeWithText("Carregando agendamentos…").assertExists()
        compose.onNodeWithText("Você ainda não possui agendamentos.").assertDoesNotExist()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
        compose.runOnIdle {
            repeat(3) { model.loadAppointments() }
            assertEquals(listOf("test-user-a"), repository.readOwners)
            response.complete(Result.success(emptyList()))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Você ainda não possui agendamentos.").assertExists()
        compose.onNodeWithText("Carregando agendamentos…").assertDoesNotExist()
    }

    @Test
    fun failureShowsGenericMessageAndRetryDisplaysReturnedReservation() {
        repository.answer = { Result.failure(IllegalStateException("detalhe-interno-sintetico")) }
        openApp()
        openList()
        compose.onNodeWithText(ERROR).assertExists()
        compose.onNodeWithText("detalhe-interno-sintetico").assertDoesNotExist()
        val response = CompletableDeferred<Result<List<AppointmentSummary>>>()
        compose.runOnIdle { repository.answer = { response.await() } }
        tap("Tentar novamente")
        compose.onNodeWithText("Carregando agendamentos…").assertExists()
        compose.onNodeWithText(ERROR).assertDoesNotExist()
        compose.runOnIdle {
            model.loadAppointments()
            assertEquals(2, repository.readOwners.size)
            response.complete(Result.success(listOf(summary("701"))))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Agendamento 701").assertExists()
        compose.onNodeWithText(ERROR).assertDoesNotExist()
    }

    @Test
    fun emptyQueryDoesNotDisplayPrototypeReservation() {
        openApp()
        openList()
        compose.onNodeWithText("Você ainda não possui agendamentos.").assertExists()
        compose.onNodeWithText("12 de setembro • 14:00").assertDoesNotExist()
        compose.onNodeWithText("Ver detalhes").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, repository.readOwners.size) }
    }

    @Test
    fun listUsesUnitTimeZoneAndPreservesCancelledPastReservation() {
        repository.records["test-user-a"] = listOf(
            summary("701"),
            summary("702").copy(
                appointment = summary("702").appointment.copy(status = AppointmentStatus.CANCELLED),
                centerName = "Unidade Sintética Tóquio",
                startsAt = Instant.parse("2026-09-30T18:30:00Z"),
                timeZoneId = "Asia/Tokyo",
            ),
        )
        openApp()
        openList()
        compose.onNodeWithText("19/10/2026 • 08:00").assertExists()
        compose.onNodeWithText("Unidade Sintética A").assertExists()
        compose.onNodeWithText("Agendamento 701").assertExists()
        compose.onNodeWithText("Agendado").assertExists()
        compose.onNodeWithText("01/10/2026 • 03:30").assertExists()
        compose.onNodeWithText("Unidade Sintética Tóquio").assertExists()
        compose.onNodeWithText("Agendamento 702").assertExists()
        compose.onNodeWithText("Cancelado").assertExists()
        compose.onNodeWithText("Doação realizada").assertDoesNotExist()
    }

    @Test
    fun leavingListCancelsPendingQueryAndReturningLoadsFreshResult() {
        val response = CompletableDeferred<Result<List<AppointmentSummary>>>()
        repository.answer = { response.await() }
        openApp()
        openList()
        val oldModel = model
        tap("‹")
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true)
            assertEquals(1, repository.cancelledReads)
            repository.answer = { Result.success(listOf(summary("702"))) }
        }
        openList()
        compose.runOnIdle {
            assertNotSame(oldModel, model)
            response.complete(Result.success(listOf(summary("701"))))
            assertEquals(2, repository.readOwners.size)
        }
        compose.waitForIdle()
        compose.onNodeWithText("Agendamento 702").assertExists()
        compose.onNodeWithText("Agendamento 701").assertDoesNotExist()
    }

    @Test
    fun logoutAndSecondLoginDoNotRetainFirstAccountsList() {
        repository.records["test-user-a"] = listOf(summary("701"))
        openApp()
        openList()
        compose.onNodeWithText("Agendamento 701").assertExists()
        val firstModel = model
        tapTab("Perfil")
        tap("Configurações")
        tap("Sair da conta")
        compose.runOnIdle {
            assertNull(auth.user)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true)
            assertNull(nav.previousBackStackEntry)
        }
        login("doador.b@example.com")
        openList()
        compose.onNodeWithText("Você ainda não possui agendamentos.").assertExists()
        compose.onNodeWithText("Agendamento 701").assertDoesNotExist()
        compose.runOnIdle {
            assertNotSame(firstModel, model)
            assertEquals(listOf("test-user-a", "test-user-b"), repository.readOwners)
        }
    }

    private class ControlledAuthRepository : AuthRepository {
    override suspend fun resendEmailConfirmation(email: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Confirmação não configurada neste teste."))
    override suspend fun verifyEmailConfirmation(credential: br.edu.fatec.doesangue.domain.model.EmailConfirmationCredential): Result<Unit> =
        Result.failure(UnsupportedOperationException("Confirmação não configurada neste teste."))

        override suspend fun signUp(displayName: String, email: String, password: String, bloodType: BloodType): Result<RegistrationResult> =
            Result.failure(UnsupportedOperationException("Cadastro não configurado neste roteiro."))

        var user: AuthenticatedUser? = AuthenticatedUser("test-user-a")

        // Recuperação exige resposta explícita nos testes desse fluxo.
        override suspend fun exchangeRecoveryCode(code: String): Result<AuthenticatedUser> =
            Result.failure(UnsupportedOperationException("Retorno de recuperação não configurado neste teste."))

        override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Alteração de senha não configurada neste teste."))

        override suspend fun updatePassword(password: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Atualização de senha não configurada neste teste."))

        override suspend fun requestPasswordReset(email: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Recuperação não configurada neste teste."))

        override suspend fun restoreSession(): Result<AuthenticatedUser?> = Result.success(user)

        override suspend fun signIn(email: String, password: String): Result<AuthenticatedUser> {
            user = AuthenticatedUser(if (email == "doador.a@example.com") "test-user-a" else "test-user-b")
            return Result.success(checkNotNull(user))
        }

        override suspend fun signOut(): Result<Unit> {
            user = null
            return Result.success(Unit)
        }
    }

    private class ControlledAppointmentRepository(private val auth: ControlledAuthRepository) : AppointmentRepository {
        override suspend fun cancelAppointment(appointmentId: String): Result<Appointment> =
            Result.failure(UnsupportedOperationException("Este roteiro testa somente leitura."))

        val readOwners = mutableListOf<String>()
        val records = mutableMapOf<String, List<AppointmentSummary>>()
        var cancelledReads = 0
        var answer: suspend (String) -> Result<List<AppointmentSummary>> = { owner ->
            Result.success(records[owner].orEmpty())
        }

        override suspend fun getMyAppointments(): Result<List<AppointmentSummary>> {
            val owner = checkNotNull(auth.user).id
            readOwners += owner
            return try {
                answer(owner)
            } catch (exception: CancellationException) {
                cancelledReads++
                throw exception
            }
        }

        override suspend fun createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment> =
            Result.failure(UnsupportedOperationException("Este roteiro testa somente leitura."))
    }

    private fun summary(id: String) = AppointmentSummary(
        appointment = Appointment(
            id = id,
            donorId = "test-user-a",
            timeSlotId = "801",
            status = AppointmentStatus.SCHEDULED,
            createdAt = Instant.parse("2026-10-12T12:00:00Z"),
        ),
        centerName = "Unidade Sintética A",
        startsAt = Instant.parse("2026-10-19T11:00:00Z"),
        timeZoneId = "America/Sao_Paulo",
    )

    private companion object {
        const val ERROR = "Não foi possível carregar os agendamentos. Tente novamente."
    }
}
