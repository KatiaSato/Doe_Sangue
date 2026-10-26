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
import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Exercita o grafo real com respostas controladas; não acessa o Supabase. */
@RunWith(AndroidJUnit4::class)
class AppointmentCreationFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: NavHostController
    private lateinit var model: SchedulingViewModel
    private val repository = ControlledAppointmentRepository()
    private val date = LocalDate.of(2026, 11, 20)

    private fun openScheduling() {
        val container = AppContainer(
            bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository(),
            campaignRepositoryOverride = FakeCampaignRepository(),
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            appointmentRepositoryOverride = repository,
            authRepositoryOverride = FakeAuthRepository(),
            donorProfileRepositoryOverride = FakeDonorProfileRepository(),
        )
        val factory = viewModelFactory {
            initializer {
                SchedulingViewModel(
                    container.donationCenterRepository,
                    container.getDonationTimeSlots,
                    repository,
                    container.getAvailableDonationDates,
                ).also { model = it }
            }
        }
        compose.setContent {
            nav = rememberNavController()
            DoeSangueTheme {
                AppNavHost(
                    recoveryLinkState = idleRecoveryLinkState,
                    navController = nav,
                    schedulingViewModelFactory = factory,
                    loginViewModelFactory = container.loginViewModelFactory,
                    profileViewModelFactory = container.profileViewModelFactory,
                    appointmentsViewModelFactory = container.appointmentsViewModelFactory,
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
            nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true
        }
        tap("Já tenho uma conta")
        compose.onNodeWithText("E-mail").performTextInput("teste@example.com")
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        tap("Entrar")
        openCenterSelection()
    }

    private fun openCenterSelection() {
        compose.onAllNodesWithText("Agendar").onLast().performClick()
        compose.waitForIdle()
        assertRoute<AppRoute.ScheduleCenter>()
    }

    private fun finishDraft() {
        tap("Unidade Acadêmica de Demonstração")
        tap("Continuar")
        // O calendário possui cobertura própria; aqui fixamos o dia do cenário.
        compose.runOnIdle { model.selectDate(date) }
        compose.waitForIdle()
        tap("09:30")
        repeat(2) { tap("Continuar") }
        assertRoute<AppRoute.ScheduleConfirm>()
    }

    @Test
    fun successShowsReturnedIdAndChoicesAndNewFlowUsesNewKey() {
        openScheduling()
        finishDraft()
        confirm()
        assertRoute<AppRoute.ScheduleSuccess>()
        compose.onNodeWithText("Número do agendamento: synthetic-1").assertExists()
        compose.onNodeWithText("Seu agendamento é para\n20/11/2026 às 09:30").assertExists()
        compose.onNodeWithText("Unidade Acadêmica de Demonstração").assertExists()
        compose.onNodeWithText("Endereço fictício").assertExists()
        compose.runOnIdle {
            assertFalse(nav.previousBackStackEntry?.destination
                ?.hasRoute<AppRoute.ScheduleConfirm>() == true)
            assertEquals(1, repository.calls.size)
            UUID.fromString(repository.calls.single().key)
        }
        tap("Ir para o início")
        assertRoute<AppRoute.Home>()
        compose.runOnIdle { assertNull(nav.previousBackStackEntry) }
        openCenterSelection()
        finishDraft()
        confirm()
        assertRoute<AppRoute.ScheduleSuccess>()
        compose.runOnIdle {
            assertEquals(2, repository.calls.size)
            assertNotEquals(repository.calls[0].key, repository.calls[1].key)
        }
    }

    @Test
    fun pendingRequestBlocksBackRepeatedSubmissionAndDraftChanges() {
        val response = CompletableDeferred<Result<Appointment>>()
        repository.answer = { response.await() }
        openScheduling()
        finishDraft()
        val original = model.uiState.value
        confirm()
        compose.onNodeWithText("Confirmando...").assertIsNotEnabled()
        compose.onNodeWithText("‹").assertDoesNotExist()
        pressBack()
        assertRoute<AppRoute.ScheduleConfirm>()
        compose.runOnIdle {
            model.confirmAppointment()
            model.selectDate(date.plusDays(1))
            model.selectTimeSlot(original.timeSlots.last().id)
            model.selectCenter(original.selectedCenter!!.copy(id = "another-center"))
            model.loadTimeSlots()
            assertEquals(1, repository.calls.size)
            assertEquals(original.selectedCenter, model.uiState.value.selectedCenter)
            assertEquals(original.selectedDate, model.uiState.value.selectedDate)
            assertEquals(original.selectedTimeSlotId, model.uiState.value.selectedTimeSlotId)
            response.complete(Result.success(repository.saved(repository.calls.single())))
        }
        assertRoute<AppRoute.ScheduleSuccess>()
    }

    @Test
    fun lostResponseRetriesSameRequestAndRecoversSingleRecord() {
        repository.answer = { call ->
            val stored = repository.saved(call)
            if (repository.calls.size == 1) {
                Result.failure(IllegalStateException("resposta-sintetica-perdida"))
            } else Result.success(stored)
        }
        openScheduling()
        finishDraft()
        confirm()
        assertRoute<AppRoute.ScheduleConfirm>()
        compose.onNodeWithText("Não foi possível confirmar o agendamento. Tente novamente.")
            .assertExists()
        compose.onNodeWithText("resposta-sintetica-perdida").assertDoesNotExist()
        confirmButton().assertIsEnabled()
        confirm()
        assertRoute<AppRoute.ScheduleSuccess>()
        compose.onNodeWithText("Número do agendamento: synthetic-1").assertExists()
        compose.runOnIdle {
            assertEquals(2, repository.calls.size)
            assertEquals(repository.calls[0], repository.calls[1])
            assertEquals(1, repository.records.size)
        }
    }

    @Test
    fun changingSlotAfterFailureStartsNewRequest() {
        repository.answer = { Result.failure(IllegalStateException("falha-sintetica")) }
        openScheduling()
        finishDraft()
        confirm()
        assertRoute<AppRoute.ScheduleConfirm>()
        repeat(2) { tap("‹") }
        assertRoute<AppRoute.ScheduleDate>()
        tap("11:00")
        repeat(2) { tap("Continuar") }
        compose.onNodeWithText("Não foi possível confirmar o agendamento. Tente novamente.")
            .assertDoesNotExist()
        compose.runOnIdle { repository.answer = { Result.success(repository.saved(it)) } }
        confirm()
        assertRoute<AppRoute.ScheduleSuccess>()
        compose.runOnIdle {
            assertEquals(2, repository.calls.size)
            assertNotEquals(repository.calls[0].slotId, repository.calls[1].slotId)
            assertNotEquals(repository.calls[0].key, repository.calls[1].key)
        }
    }

    @Test
    fun cancelledRecordStaysOnReviewAndDoesNotSubmitAgain() {
        repository.answer = { Result.success(repository.saved(it).copy(status = AppointmentStatus.CANCELLED)) }
        openScheduling()
        finishDraft()
        confirm()
        assertRoute<AppRoute.ScheduleConfirm>()
        compose.onNodeWithText("Este pedido está cancelado. Ele não representa uma reserva ativa.")
            .assertExists()
        compose.onNodeWithText("Agendamento confirmado!").assertDoesNotExist()
        confirmButton().assertIsNotEnabled()
        compose.runOnIdle {
            model.confirmAppointment()
            assertEquals(1, repository.calls.size)
        }
        tap("‹")
        assertRoute<AppRoute.ScheduleRecurrence>()
    }

    @Test
    fun incompleteDraftDoesNotReachRepository() {
        openScheduling()
        compose.runOnIdle {
            model.confirmAppointment()
            assertTrue(repository.calls.isEmpty())
            assertFalse(model.uiState.value.appointmentCreation.isLoading)
        }
        tap("Unidade Acadêmica de Demonstração")
        tap("Continuar")
        compose.runOnIdle { model.selectDate(date) }
        compose.waitForIdle()
        compose.runOnIdle {
            model.confirmAppointment()
            assertTrue(repository.calls.isEmpty())
        }
    }

    private fun confirmButton() = compose.onNode(hasText("Confirmar agendamento") and hasClickAction())

    private fun confirm() {
        confirmButton().performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        val node = compose.onNodeWithText(text)
        if (text in listOf("Entrar", "Unidade Acadêmica de Demonstração", "Ir para o início")) {
            node.performScrollTo()
        }
        node.performClick()
        compose.waitForIdle()
    }

    private inline fun <reified T : Any> assertRoute() {
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue("Destino esperado: ${T::class.simpleName}",
                nav.currentDestination?.hasRoute<T>() == true)
        }
    }

    private data class Request(val slotId: String, val key: String)

    private class ControlledAppointmentRepository : AppointmentRepository {
        override suspend fun cancelAppointment(appointmentId: String): Result<Appointment> =
            Result.failure(UnsupportedOperationException("Este roteiro testa somente a criação."))

        override suspend fun getMyAppointments(): Result<List<AppointmentSummary>> =
            Result.failure(UnsupportedOperationException("Este roteiro testa somente a criação."))

        val calls = mutableListOf<Request>()
        val records = mutableMapOf<String, Appointment>()
        var answer: suspend (Request) -> Result<Appointment> = { Result.success(saved(it)) }

        fun saved(call: Request): Appointment = records.getOrPut(call.key) {
            Appointment(
                id = "synthetic-${records.size + 1}",
                donorId = "test-user-1",
                timeSlotId = call.slotId,
                status = AppointmentStatus.SCHEDULED,
                createdAt = Instant.parse("2026-10-12T12:00:00Z"),
            )
        }

        override suspend fun createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment> {
            val call = Request(timeSlotId, idempotencyKey)
            calls += call
            return answer(call)
        }
    }
}
