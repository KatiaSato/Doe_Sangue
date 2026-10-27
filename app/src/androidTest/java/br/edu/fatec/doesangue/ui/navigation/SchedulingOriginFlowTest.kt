package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.presentation.scheduling.CenterPreparationUiState
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** Grafos/telas reais; apenas dados sintéticos, sem chamadas de rede ou reservas reais. */
@RunWith(AndroidJUnit4::class)
class SchedulingOriginFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private val a = DonationCenter("test-center-1", "Unidade Sintética Alfa", "Cidade Alfa",
        "Endereço sintético A", "Horário fictício", "Contato fictício", "America/Sao_Paulo")
    private val b = a.copy(id = "test-center-2", name = "Unidade Sintética Beta", city = "Cidade Beta")
    private val campaign = Campaign("campanha-nao-e-unidade", b.id, "Campanha Sintética Beta",
        "Descrição fictícia", Instant.parse("2026-10-15T03:00:00Z"), Instant.parse("2026-12-01T03:00:00Z"),
        "Fonte sintética", "Nome anterior da unidade na campanha", b.timeZoneId, emptyList())
    private var reads = 0
    private var writes = 0
    private var answer: suspend () -> Result<List<DonationCenter>> = { Result.success(listOf(a, b)) }

    private fun openHome() {
        val centers = object : DonationCenterRepository {
            override suspend fun getCenters(): Result<List<DonationCenter>> {
                reads++
                return answer()
            }
        }
        val campaigns = object : CampaignRepository {
            override suspend fun getCampaigns(bloodType: BloodType?) = Result.success(listOf(campaign))
        }
        val auth = object : AuthRepository by FakeAuthRepository() {
            override suspend fun restoreSession() = Result.success(AuthenticatedUser("usuario-sintetico"))
        }
        val appointments = object : AppointmentRepository by FakeAppointmentRepository() {
            override suspend fun createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment> {
                writes++
                return Result.failure(AssertionError("Este roteiro não confirma reservas"))
            }
        }
        val container = AppContainer(routineRepositoryOverride = br.edu.fatec.doesangue.ui.navigation.FakeReminderRoutineRepository(), centerRepositoryOverride = centers,
            campaignRepositoryOverride = campaigns, authRepositoryOverride = auth,
            appointmentRepositoryOverride = appointments, timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            donorProfileRepositoryOverride = FakeDonorProfileRepository(),
            bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository())
        compose.setContent {
            nav = rememberNavController()
            DoeSangueTheme { FeatureTestHost(container, nav) }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Home>() == true }
        compose.waitForIdle()
    }

    private fun openUnitDetail() {
        openHome()
        compose.onNodeWithText("Unidades").performClick()
        compose.onNodeWithText("Buscar por nome ou cidade").performTextReplacement("alfa")
        compose.onNodeWithText(a.name).performScrollTo().performClick()
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.CenterDetailById>() == true }
        compose.waitForIdle()
    }

    private fun scheduleFromUnit() {
        compose.onNodeWithText("Agendar doação").performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun waitForDate() {
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.ScheduleDate>() == true }
        compose.waitForIdle()
        compose.onNodeWithText("Escolha uma data").assertIsDisplayed()
        compose.onNodeWithText("Escolha uma unidade").assertDoesNotExist()
        compose.onNodeWithText("Nenhuma unidade selecionada").assertDoesNotExist()
    }

    private fun schedulingModel() = ViewModelProvider(
        nav.getBackStackEntry<AppGraph.Scheduling>(),
    )[SchedulingViewModel::class.java]

    @Test fun unitShortcutRequeriesKeepsSelectedModelAndReturnsWithoutReenteringPreparation() {
        openUnitDetail()
        val readsBeforeScheduling = reads
        val fresh = a.copy(name = "Unidade Sintética Alfa atualizada")
        compose.runOnIdle { answer = { Result.success(listOf(fresh, b)) } }
        scheduleFromUnit()
        waitForDate()
        compose.onNodeWithText(fresh.name).assertIsDisplayed()
        lateinit var first: SchedulingViewModel
        var readsAtDate = 0
        compose.runOnIdle {
            first = schedulingModel()
            assertEquals(fresh, first.uiState.value.selectedCenter)
            assertEquals(CenterPreparationUiState.Ready(a.id), first.uiState.value.centerPreparation)
            assertTrue(reads > readsBeforeScheduling) // Origem exige leitura nova; init síncrono pode já ter terminado.
            readsAtDate = reads
            assertEquals(a.id, nav.previousBackStackEntry?.toRoute<AppRoute.CenterDetailById>()?.centerId)
        }
        pressBack()
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.CenterDetailById>() == true)
            assertEquals(readsAtDate, reads)
            assertTrue(runCatching { nav.getBackStackEntry<AppGraph.Scheduling>() }.isFailure)
        }
        scheduleFromUnit()
        waitForDate()
        compose.runOnIdle {
            assertNotSame(first, schedulingModel())
            assertTrue(reads > readsAtDate)
        }
        compose.onNodeWithText("‹").performClick()
        compose.onNodeWithText("‹").performClick()
        compose.onNodeWithText("Buscar por nome ou cidade").assertTextContains("alfa")
        compose.onNodeWithText(b.name).assertDoesNotExist()
        assertEquals(0, writes)
    }

    @Test fun campaignUsesUnitIdAndCurrentCatalogThenReturnsToCampaignAndFilter() {
        openHome()
        compose.runOnIdle { nav.navigate(AppRoute.Campaigns) }
        compose.onNodeWithText("Tipo sanguíneo: Todos").performClick()
        compose.onNodeWithText("A+").performClick()
        compose.onNodeWithText("Ver campanha").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Quero agendar uma doação").performScrollTo().performClick()
        waitForDate()
        compose.onNodeWithText(b.name).assertIsDisplayed()
        compose.onNodeWithText(campaign.centerName).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(b.id, schedulingModel().uiState.value.selectedCenter?.id)
            assertNotEquals(campaign.id, schedulingModel().uiState.value.selectedCenter?.id)
            assertEquals(campaign.id, nav.previousBackStackEntry?.toRoute<AppRoute.CampaignDetailById>()?.campaignId)
        }
        compose.onNodeWithText("‹").performClick()
        compose.onNodeWithText(campaign.title).assertExists()
        compose.onNodeWithText("‹").performClick()
        compose.onNodeWithText("Tipo sanguíneo: A+").assertExists()
        assertEquals(0, writes)
    }

    @Test fun pendingOriginKeepsSameViewModelThroughDateRecurrenceAndReviewWithoutWriting() {
        openUnitDetail()
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        compose.runOnIdle { answer = { pending.await() } }
        try {
            scheduleFromUnit()
            compose.onNodeWithText("Consultando unidade…").assertIsDisplayed()
            lateinit var preparing: SchedulingViewModel
            compose.runOnIdle {
                preparing = schedulingModel()
                assertEquals(a.id, nav.currentBackStackEntry?.toRoute<AppRoute.ScheduleFromCenter>()?.centerId)
                assertEquals(3, reads)
                pending.complete(Result.success(listOf(a, b)))
            }
            waitForDate()
            compose.runOnIdle {
                assertSame(preparing, schedulingModel())
                preparing.selectDate(LocalDate.of(2026, 11, 20))
            }
            compose.onNodeWithText("08:00").performScrollTo().performClick()
            compose.onNodeWithText("Continuar").performScrollTo().performClick()
            compose.onNodeWithText("Somente este agendamento").assertExists()
            compose.onNodeWithText("Continuar").performScrollTo().performClick()
            compose.onNodeWithText("Revise os dados").assertExists()
            compose.onNodeWithText(a.name).assertExists()
            compose.runOnIdle { assertSame(preparing, schedulingModel()) }
            pressBack()
            pressBack()
            waitForDate()
            compose.onNodeWithText("20/11/2026").assertExists()
            compose.runOnIdle { assertSame(preparing, schedulingModel()); assertEquals(3, reads) }
            assertEquals(0, writes)
        } finally {
            compose.runOnIdle { pending.complete(Result.success(emptyList())) }
        }
    }

    @Test fun unavailableUnitShowsSpecificErrorAndRetryNeverSelectsFallback() {
        openUnitDetail()
        compose.runOnIdle { answer = { Result.success(listOf(b)) } }
        scheduleFromUnit()
        compose.onNodeWithText("Esta unidade não está mais disponível para agendamento.").assertExists()
        compose.onNodeWithText("Escolha uma data").assertDoesNotExist()
        compose.runOnIdle {
            assertNull(schedulingModel().uiState.value.selectedCenter)
            answer = { Result.success(listOf(a, b)) }
        }
        compose.onNodeWithText("Tentar novamente").performClick()
        waitForDate()
        compose.runOnIdle { assertEquals(a.id, schedulingModel().uiState.value.selectedCenter?.id) }
        assertEquals(0, writes)
    }

    @Test fun catalogFailureCanRetryWithoutLeakingInternalDetailsOrLosingOrigin() {
        openUnitDetail()
        compose.runOnIdle { answer = { Result.failure(IllegalStateException("erro interno sintético")) } }
        scheduleFromUnit()
        compose.onNodeWithText("Não foi possível carregar as unidades.").assertExists()
        compose.onNodeWithText("erro interno sintético").assertDoesNotExist()
        compose.runOnIdle { answer = { Result.success(listOf(a)) } }
        compose.onNodeWithText("Tentar novamente").performClick()
        waitForDate()
        compose.onNodeWithText("‹").performClick()
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.CenterDetailById>() == true) }
    }

    @Test fun backDuringPendingReadDisposesFlowAndIgnoresLateSuccess() {
        openUnitDetail()
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        compose.runOnIdle { answer = { withContext(NonCancellable) { pending.await() } } }
        try {
            scheduleFromUnit()
            compose.onNodeWithText("Consultando unidade…").assertExists()
            lateinit var old: SchedulingViewModel
            compose.runOnIdle { old = schedulingModel() }
            val beforeBack = old.uiState.value
            pressBack()
            compose.waitForIdle()
            compose.runOnIdle { pending.complete(Result.success(listOf(a, b))) }
            compose.runOnIdle {
                assertTrue(nav.currentDestination?.hasRoute<AppRoute.CenterDetailById>() == true)
                assertEquals(beforeBack, old.uiState.value)
            }
            compose.onNodeWithText("Escolha uma data").assertDoesNotExist()
            compose.onNodeWithText(a.name).assertExists()
        } finally { compose.runOnIdle { pending.complete(Result.success(emptyList())) } }
    }

    @Test fun blankOriginDoesNotOfferPointlessRetryAndCanReturn() {
        openHome()
        compose.runOnIdle { nav.navigate(AppRoute.ScheduleFromCenter("   ")) }
        compose.onNodeWithText("Não foi possível identificar a unidade. Volte e tente novamente.").assertExists()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
        compose.onNodeWithText("Escolha uma data").assertDoesNotExist()
        compose.onNodeWithText("‹").performClick()
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true) }
    }

    @Test fun generalTabStillStartsAtUnitChoiceAndDateBackKeepsItsSelection() {
        openHome()
        compose.onNodeWithText("Agendar").performClick()
        compose.onNodeWithText("Escolha uma unidade").assertExists()
        compose.onNodeWithText(a.name).performScrollTo().performClick()
        compose.onNodeWithText("Continuar").performScrollTo().performClick()
        waitForDate()
        compose.onNodeWithText(a.name).assertExists()
        pressBack()
        compose.onNodeWithText("Escolha uma unidade").assertExists()
        compose.runOnIdle {
            assertEquals(CenterPreparationUiState.Idle, schedulingModel().uiState.value.centerPreparation)
            assertEquals(a.id, schedulingModel().uiState.value.selectedCenter?.id)
        }
        assertEquals(0, writes)
    }
}
