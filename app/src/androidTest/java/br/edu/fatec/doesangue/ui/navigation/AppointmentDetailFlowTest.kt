package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
import br.edu.fatec.doesangue.presentation.appointments.AppointmentDetailViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Navegação, ViewModel e tela reais; sessão e reservas somente em memória, sem rede. */
@RunWith(AndroidJUnit4::class)
class AppointmentDetailFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private val auth = ControlledAuthRepository()
    private val repository = ControlledAppointmentRepository(auth)
    private val createdIds = mutableListOf<String>()
    private lateinit var nav: NavHostController
    private lateinit var model: AppointmentDetailViewModel
    private lateinit var restoration: StateRestorationTester

    private fun openApp() {
        repository.records.putIfAbsent("test-user-a", listOf(summary("701")))
        val profiles = object : DonorProfileRepository {
            override suspend fun getCurrentProfile(): Result<DonorProfile?> =
                Result.success(auth.user?.let { DonorProfile(it.id, "Doador de teste") })

            override suspend fun createCurrentProfile(displayName: String): Result<DonorProfile> =
                Result.failure(UnsupportedOperationException("Este teste não cria perfis."))
        }
        val container = AppContainer(
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            authRepositoryOverride = auth,
            donorProfileRepositoryOverride = profiles,
            appointmentRepositoryOverride = repository,
        )
        restoration = StateRestorationTester(compose)
        restoration.setContent {
            nav = rememberNavController()
            DoeSangueTheme {
                AppNavHost(
                    navController = nav,
                    schedulingViewModelFactory = container.schedulingViewModelFactory,
                    loginViewModelFactory = container.loginViewModelFactory,
                    profileViewModelFactory = container.profileViewModelFactory,
                    appointmentsViewModelFactory = container.appointmentsViewModelFactory,
                    appointmentDetailViewModelFactory = { id ->
                        viewModelFactory {
                            initializer {
                                createdIds += id
                                AppointmentDetailViewModel(repository, id).also { model = it }
                            }
                        }
                    },
                    logoutViewModelFactory = container.logoutViewModelFactory,
                    sessionViewModelFactory = container.sessionViewModelFactory,
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
    }

    private fun openDetails(index: Int = 0) {
        compose.onAllNodesWithText("Ver detalhes")[index].performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun navigateToDetail(id: String) {
        compose.runOnIdle { nav.navigate(AppRoute.AppointmentDetailById(id)) }
        compose.waitForIdle()
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

    @Test
    fun selectedSecondReservationUsesFreshDataAndReturnsToList() {
        repository.records["test-user-a"] = listOf(summary("701"), summary("702"))
        openApp()
        openList()
        compose.runOnIdle {
            repository.records["test-user-a"] = listOf(
                summary("701"),
                summary("702").copy(
                    appointment = summary("702").appointment.copy(status = AppointmentStatus.CANCELLED),
                    centerName = "Unidade Sintética Tóquio Atualizada",
                    startsAt = Instant.parse("2026-09-30T18:30:00Z"),
                    timeZoneId = "Asia/Tokyo",
                ),
            )
        }
        openDetails(index = 1)
        compose.runOnIdle {
            assertEquals("702", nav.currentBackStackEntry?.toRoute<AppRoute.AppointmentDetailById>()?.appointmentId)
            assertEquals(listOf("702"), createdIds)
            assertEquals(2, repository.readOwners.size)
        }
        compose.onNodeWithText("Agendamento 702").assertExists()
        compose.onNodeWithText("Agendamento 701").assertDoesNotExist()
        compose.onNodeWithText("Unidade Sintética Tóquio Atualizada").assertExists()
        compose.onNodeWithText("01/10/2026 • 03:30").assertExists()
        compose.onNodeWithText("Cancelado").assertExists()
        compose.onNodeWithText("Alterar agendamento").assertDoesNotExist()
        compose.onNodeWithText("Cancelar agendamento").assertDoesNotExist()
        compose.onNodeWithText("Lembrete ativo ✓").assertDoesNotExist()
        tap("‹")
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.DonationsAppointments>() == true)
            assertEquals(2, repository.readOwners.size)
        }
        compose.onNodeWithText("Agendamento 701").assertExists()
    }

    @Test
    fun pendingDetailShowsLoadingAndPreventsSimultaneousQueries() {
        openApp()
        openList()
        val response = CompletableDeferred<Result<List<AppointmentSummary>>>()
        compose.runOnIdle { repository.answer = { response.await() } }
        openDetails()
        compose.onNodeWithText("Carregando agendamento…").assertExists()
        compose.onNodeWithText("Agendamento 701").assertDoesNotExist()
        compose.runOnIdle {
            repeat(3) { model.loadAppointment() }
            assertEquals(2, repository.readOwners.size)
            response.complete(Result.success(listOf(summary("701"))))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Agendamento 701").assertExists()
        compose.onNodeWithText("Carregando agendamento…").assertDoesNotExist()
    }

    @Test
    fun failureShowsGenericMessageAndRetryKeepsRequestedId() {
        openApp()
        openList()
        compose.runOnIdle {
            repository.answer = { Result.failure(IllegalStateException("detalhe-interno-sintetico")) }
        }
        openDetails()
        compose.onNodeWithText(ERROR).assertExists()
        compose.onNodeWithText("detalhe-interno-sintetico").assertDoesNotExist()
        val response = CompletableDeferred<Result<List<AppointmentSummary>>>()
        compose.runOnIdle { repository.answer = { response.await() } }
        tap("Tentar novamente")
        compose.onNodeWithText("Carregando agendamento…").assertExists()
        compose.onNodeWithText(ERROR).assertDoesNotExist()
        compose.runOnIdle {
            model.loadAppointment()
            assertEquals(3, repository.readOwners.size)
            response.complete(Result.success(listOf(summary("702"), summary("701"))))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Agendamento 701").assertExists()
        compose.onNodeWithText("Agendamento 702").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("701"), createdIds) }
    }

    @Test
    fun unknownIdAndEmptyOwnListShowNotFoundWithoutOtherRecordData() {
        openApp()
        navigateToDetail("999")
        compose.onNodeWithText(NOT_FOUND).assertExists()
        compose.onNodeWithText("Agendamento 701").assertDoesNotExist()
        compose.onNodeWithText("Unidade Sintética A").assertDoesNotExist()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
        compose.runOnIdle {
            repository.records["test-user-a"] = emptyList()
            model.loadAppointment()
        }
        compose.waitForIdle()
        compose.onNodeWithText(NOT_FOUND).assertExists()
        compose.runOnIdle { assertEquals(2, repository.readOwners.size) }
    }

    @Test
    fun leavingPendingDetailCancelsQueryAndAnotherIdUsesNewViewModel() {
        repository.records["test-user-a"] = listOf(summary("701"), summary("702"))
        openApp()
        openList()
        val response = CompletableDeferred<Result<List<AppointmentSummary>>>()
        compose.runOnIdle { repository.answer = { response.await() } }
        openDetails()
        val oldModel = model
        tap("‹")
        compose.runOnIdle {
            assertEquals(1, repository.cancelledReads)
            repository.answer = { Result.success(repository.records["test-user-a"].orEmpty()) }
        }
        openDetails(index = 1)
        compose.runOnIdle {
            assertNotSame(oldModel, model)
            assertEquals(listOf("701", "702"), createdIds)
            response.complete(Result.success(listOf(summary("701"))))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Agendamento 702").assertExists()
        compose.onNodeWithText("Agendamento 701").assertDoesNotExist()
    }

    @Test
    fun secondAccountCannotOpenFirstAccountsIdAndCanOpenItsOwn() {
        repository.records["test-user-b"] = listOf(summary("702", "test-user-b"))
        openApp()
        openList()
        openDetails()
        compose.onNodeWithText("Agendamento 701").assertExists()
        tapTab("Perfil")
        tap("Configurações")
        tap("Sair da conta")
        compose.runOnIdle {
            assertNull(auth.user)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true)
        }
        tap("Já tenho uma conta")
        compose.onNodeWithText("E-mail").performTextInput("doador.b@example.com")
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        tap("Entrar")
        openList()
        navigateToDetail("701")
        compose.onNodeWithText(NOT_FOUND).assertExists()
        compose.onNodeWithText("Agendamento 701").assertDoesNotExist()
        compose.onNodeWithText("Agendamento 702").assertDoesNotExist()
        tap("‹")
        openDetails()
        compose.onNodeWithText("Agendamento 702").assertExists()
        compose.onNodeWithText("Agendamento 701").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(listOf("test-user-a", "test-user-a", "test-user-b", "test-user-b", "test-user-b"), repository.readOwners)
        }
    }

    @Test
    fun savedNavigationRetainsReservationIdAndReturnToList() {
        val id = "4294967296"
        repository.records["test-user-a"] = listOf(summary(id))
        openApp()
        openList()
        openDetails()
        // Estado salvo do Compose/navegação; não simula morte real do processo ou renovação da sessão.
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(id, nav.currentBackStackEntry?.toRoute<AppRoute.AppointmentDetailById>()?.appointmentId)
        }
        compose.onNodeWithText("Agendamento $id").assertExists()
        tap("‹")
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.DonationsAppointments>() == true)
        }
    }

    private class ControlledAuthRepository : AuthRepository {
        var user: AuthenticatedUser? = AuthenticatedUser("test-user-a")

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
        val records = mutableMapOf<String, List<AppointmentSummary>>()
        val readOwners = mutableListOf<String>()
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

    private fun summary(id: String, owner: String = "test-user-a") = AppointmentSummary(
        appointment = Appointment(
            id = id,
            donorId = owner,
            timeSlotId = "801",
            status = AppointmentStatus.SCHEDULED,
            createdAt = Instant.parse("2026-10-12T12:00:00Z"),
        ),
        centerName = "Unidade Sintética A",
        startsAt = Instant.parse("2026-10-19T11:00:00Z"),
        timeZoneId = "America/Sao_Paulo",
    )

    private companion object {
        const val ERROR = "Não foi possível carregar o agendamento. Tente novamente."
        const val NOT_FOUND = "Agendamento não encontrado ou indisponível."
    }
}
