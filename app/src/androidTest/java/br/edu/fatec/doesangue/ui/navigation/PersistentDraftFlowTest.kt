package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.data.local.FileSchedulingDraftStore
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.presentation.auth.SessionMonitorViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.*
import java.util.UUID

class PersistentDraftFlowTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val directory = File(instrumentation.targetContext.cacheDir, "t02-ui-${UUID.randomUUID()}")
    private val store = FileSchedulingDraftStore(directory)
    private val user = AuthenticatedUser("test-user-1")
    private val source = MutableStateFlow<SessionPresence>(SessionPresence.Authenticated(user))
    private val date = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(3)
    private val start = date.atTime(8,0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant()
    private val keys = mutableListOf<String>()
    private var cancelled = false
    private lateinit var nav: NavHostController
    private fun draft(pending: Boolean = false) = SchedulingDraft(user.id, "test-center-1", "Unidade Sintética do Rascunho",
        "America/Sao_Paulo", date.toString(), "test-slot-$start", start.toString(), SchedulingStep.CONFIRM,
        if (pending) "00000000-0000-4000-8000-000000000099" else null)
    @After fun close() { directory.deleteRecursively() }
    private fun open(emptyCatalog: Boolean = false) {
        val auth = object : AuthRepository by FakeAuthRepository() {
            override fun observeSession() = source
            override suspend fun restoreSession() = Result.success(user)
        }
        val appointments = object : AppointmentRepository by FakeAppointmentRepository() {
            override suspend fun createAppointmentForOwner(ownerId: String, timeSlotId: String, idempotencyKey: String): Result<Appointment> {
                if (ownerId != (source.value as? SessionPresence.Authenticated)?.user?.id) return Result.failure(DataFailure(DataError.SESSION_UNAVAILABLE))
                return createAppointment(timeSlotId, idempotencyKey)
            }
            override suspend fun createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment> {
                keys += idempotencyKey
                return Result.success(Appointment("synthetic-t02", (source.value as SessionPresence.Authenticated).user.id,
                    timeSlotId, if (cancelled) AppointmentStatus.CANCELLED else AppointmentStatus.SCHEDULED, Instant.now()))
            }
        }
        val centers = if (emptyCatalog) object : DonationCenterRepository {
            override suspend fun getCenters() = Result.success(emptyList<DonationCenter>())
        } else FakeDonationCenterRepository()
        val container = AppContainer(authRepositoryOverride = auth, centerRepositoryOverride = centers,
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(), appointmentRepositoryOverride = appointments,
            donorProfileRepositoryOverride = FakeDonorProfileRepository(), campaignRepositoryOverride = FakeCampaignRepository(),
            bloodStockRepositoryOverride = FakeBloodStockRepository(), donationHistoryRepositoryOverride = FakeDonationHistoryRepository(),
            schedulingDraftStore = store)
        compose.setContent {
            nav = rememberNavController()
            val monitor: SessionMonitorViewModel = viewModel(factory = container.sessionMonitorViewModelFactory)
            DoeSangueTheme { FeatureTestHost(container, nav, sessionState = monitor.uiState) }
        }
        compose.mainClock.advanceTimeBy(2_000); route<AppRoute.Home>()
    }
    private inline fun <reified T : Any> route() = compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<T>() == true }
    private fun tap(text: String) {
        val node = compose.onNode(hasText(text) and hasClickAction())
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.performClick(); compose.waitForIdle()
    }
    private fun offer() { tap("Agendar"); compose.waitUntil(10_000) { compose.onAllNodesWithText("Retomar").fetchSemanticsNodes().isNotEmpty() } }
    private fun capture(name: String) {
        File(instrumentation.targetContext.getExternalFilesDir(null), "t02-$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun unsentDraftRequiresConsentAndConfirmationBeforeCreatingAndClearsOnFinish() = runBlocking<Unit> {
        store.save(draft()).getOrThrow(); open(); offer()
        capture("retomada")
        assertTrue(keys.isEmpty()); tap("Retomar"); route<AppRoute.ScheduleConfirm>()
        compose.onNodeWithText("Rascunho recuperado. Confira as escolhas antes de confirmar.").assertIsDisplayed()
        assertTrue(keys.isEmpty()); tap("Confirmar agendamento"); route<AppRoute.ScheduleSuccess>()
        assertNotNull(store.read(user.id).getOrThrow()?.requestKey)
        tap("Ir para o início"); route<AppRoute.Home>()
        assertNull(store.read(user.id).getOrThrow()); assertEquals(1, keys.size)
    }
    @Test fun preparedPastHiddenOfferCanRecoverCancelledResultWithoutNewKey() = runBlocking<Unit> {
        val past = date.minusDays(20)
        val draft = draft(true).copy(dateIso = past.toString(), startsAtIso = past.atTime(8,0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant().toString())
        store.save(draft).getOrThrow(); cancelled = true
        open(emptyCatalog = true); offer()
        compose.onNodeWithText("Descartar rascunho").assertDoesNotExist()
        tap("Retomar"); route<AppRoute.ScheduleConfirm>()
        assertTrue(keys.isEmpty()); capture("pedido-anterior")
        tap("Confirmar agendamento")
        compose.onNodeWithText("Este pedido está cancelado. Ele não representa uma reserva ativa.").assertIsDisplayed()
        assertEquals(listOf(draft.requestKey), keys)
        tap("Concluir"); route<AppRoute.Home>(); assertNull(store.read(user.id).getOrThrow())
    }
    @Test fun discardingUnsentDraftPreservesChosenOriginFlow() = runBlocking<Unit> {
        store.save(draft()).getOrThrow(); open()
        compose.runOnIdle { nav.navigate(AppRoute.ScheduleFromCenter("test-center-1")) }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Descartar rascunho").fetchSemanticsNodes().isNotEmpty() }
        tap("Descartar rascunho"); route<AppRoute.ScheduleDate>()
        compose.onNodeWithText("Retomar").assertDoesNotExist(); assertTrue(keys.isEmpty())
    }
    @Test fun leavingAndReenteringUsesANewGraphAndOffersTheDurableDraftAgain() = runBlocking<Unit> {
        store.save(draft()).getOrThrow(); open(); offer(); tap("Voltar ao início"); route<AppRoute.Home>()
        assertNotNull(store.read(user.id).getOrThrow()); offer()
        tap("Retomar"); route<AppRoute.ScheduleConfirm>()
        assertTrue(keys.isEmpty())
    }
    @Test fun corruptFileIsExplainedAndDiscardDoesNotCreateAReservation() = runBlocking<Unit> {
        store.save(draft()).getOrThrow(); directory.listFiles()!!.single().writeText("broken synthetic fixture")
        open(); tap("Agendar")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Descartar rascunho").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("broken synthetic fixture").assertDoesNotExist()
        tap("Descartar rascunho"); route<AppRoute.ScheduleCenter>()
        compose.onNodeWithText("Escolha uma unidade").assertIsDisplayed(); assertTrue(keys.isEmpty())
    }
    @Test fun lossOfSessionClearsPrivateHistoryAndNewAccountCannotSeePreviousDraft() = runBlocking<Unit> {
        store.save(draft()).getOrThrow(); open(); offer()
        compose.runOnIdle { source.value = SessionPresence.NotAuthenticated() }
        route<AppRoute.Login>(); tap("Entendi")
        compose.runOnIdle { source.value = SessionPresence.Authenticated(AuthenticatedUser("synthetic-b")); nav.navigate(AppGraph.Main) }
        route<AppRoute.Home>(); tap("Agendar"); route<AppRoute.ScheduleCenter>()
        compose.onNodeWithText("Retomar").assertDoesNotExist()
        compose.onNodeWithText("Unidade Sintética do Rascunho").assertDoesNotExist()
        assertNotNull(store.read(user.id).getOrThrow()); assertTrue(keys.isEmpty())
    }
    @Test fun resumedUnsentConfirmationCanGoBackToReviewDateAndUnitWithoutSending() = runBlocking<Unit> {
        store.save(draft()).getOrThrow(); open(); offer(); tap("Retomar"); route<AppRoute.ScheduleConfirm>()
        compose.runOnIdle { assertTrue(nav.popBackStack()) }; route<AppRoute.ScheduleRecurrence>()
        compose.runOnIdle { assertTrue(nav.popBackStack()) }; route<AppRoute.ScheduleDate>()
        compose.runOnIdle {
            val model = androidx.lifecycle.ViewModelProvider(nav.getBackStackEntry<AppGraph.Scheduling>())[
                br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel::class.java]
            assertEquals(date, model.uiState.value.selectedDate)
            assertEquals(draft().slotId, model.uiState.value.selectedTimeSlotId)
            assertTrue(nav.popBackStack())
        }
        route<AppRoute.ScheduleCenter>(); assertTrue(keys.isEmpty())
    }
}
