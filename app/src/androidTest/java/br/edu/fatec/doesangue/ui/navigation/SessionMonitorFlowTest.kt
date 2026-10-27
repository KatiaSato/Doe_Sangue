package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.presentation.auth.*
import br.edu.fatec.doesangue.presentation.profile.EditProfileViewModel
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.After
import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Monitor, grafo e VMs reais. Contas/fontes somente fictícias e sem rede. */
class SessionMonitorFlowTest {
    @get:Rule val compose = createComposeRule()
    private val restoration = StateRestorationTester(compose)
    private val releasePending = mutableListOf<() -> Unit>()
    @After fun release() { compose.runOnIdle { releasePending.forEach { it() } } }
    private lateinit var nav: NavHostController
    private val a = AuthenticatedUser("test-user-1")
    private val b = AuthenticatedUser("synthetic-b")
    private val source = MutableStateFlow<SessionPresence>(SessionPresence.Authenticated(a))
    private val recovery = MutableStateFlow<RecoveryLinkUiState>(RecoveryLinkUiState.Idle)
    private var logoutCalls = 0
    private var logoutFails = false
    private var restored: AuthenticatedUser? = a
    private var loginUser = a
    private fun open(profiles: DonorProfileRepository = FakeDonorProfileRepository(),
        appointments: AppointmentRepository = FakeAppointmentRepository()) {
        val auth = object : AuthRepository by FakeAuthRepository() {
            override fun observeSession() = source
            override suspend fun restoreSession() = Result.success(restored)
            override suspend fun signIn(email: String, password: String) = Result.success(loginUser)
            override suspend fun signOut(): Result<Unit> {
                logoutCalls++
                if (logoutFails) return Result.failure(AuthFailure(AuthError.NETWORK))
                source.value = SessionPresence.NotAuthenticated(true)
                return Result.success(Unit)
            }
        }
        val container = AppContainer(routineRepositoryOverride = br.edu.fatec.doesangue.ui.navigation.FakeReminderRoutineRepository(), authRepositoryOverride = auth,
            donorProfileRepositoryOverride = profiles, centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(), appointmentRepositoryOverride = appointments,
            campaignRepositoryOverride = FakeCampaignRepository(), bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository())
        restoration.setContent {
            nav = rememberNavController()
            val monitor: SessionMonitorViewModel = viewModel(factory = container.sessionMonitorViewModelFactory)
            DoeSangueTheme { FeatureTestHost(container, nav, recoveryLinkState = recovery, sessionState = monitor.uiState) }
        }
        compose.mainClock.advanceTimeBy(2_000); compose.waitForIdle()
    }
    private inline fun <reified T : Any> awaitRoute() = compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<T>() == true }
    private fun emit(presence: SessionPresence) { compose.runOnIdle { source.value = presence }; compose.waitForIdle() }
    private fun tap(text: String) {
        val node = compose.onNodeWithText(text)
        if (runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.performClick(); compose.waitForIdle()
    }
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "t01-$name.png")
        file.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun expire() = emit(SessionPresence.NotAuthenticated())
    private fun assertCleanLogin() {
        awaitRoute<AppRoute.Login>()
        compose.onNodeWithText("Sua sessão foi encerrada. Entre novamente para continuar.").assertIsDisplayed()
        capture("sessao-encerrada")
        compose.runOnIdle { assertNull(nav.previousBackStackEntry); assertEquals(0, logoutCalls) }
        tap("Entendi")
        compose.runOnIdle {
            assertTrue(runCatching { nav.getBackStackEntry<AppGraph.Main>() }.isFailure)
            assertTrue(runCatching { nav.getBackStackEntry<AppGraph.Scheduling>() }.isFailure)
        }
        compose.onNodeWithText("Doador de teste").assertDoesNotExist()
    }

    @Test fun lossOnProfileClearsPrivateHistoryAndExplainsWhy() {
        open(); awaitRoute<AppRoute.Home>(); compose.runOnIdle { nav.navigate(AppRoute.Profile) }
        compose.onNodeWithText("Doador de teste").assertExists()
        expire(); assertCleanLogin()
        compose.runOnIdle { assertFalse(nav.popBackStack()) }
    }
    @Test fun lossInSchedulingDisposesDraftAndNewEntryGetsNewModel() {
        open(); awaitRoute<AppRoute.Home>(); compose.runOnIdle { nav.navigate(AppGraph.Scheduling) }
        awaitRoute<AppRoute.ScheduleCenter>()
        lateinit var old: SchedulingViewModel
        compose.runOnIdle {
            old = ViewModelProvider(nav.getBackStackEntry<AppGraph.Scheduling>())[SchedulingViewModel::class.java]
            old.selectCenter(old.uiState.value.centers.first())
        }
        expire(); assertCleanLogin()
        compose.runOnIdle { source.value = SessionPresence.Authenticated(a); nav.navigate(AppGraph.Main); nav.navigate(AppGraph.Scheduling) }
        awaitRoute<AppRoute.ScheduleCenter>()
        compose.runOnIdle {
            val fresh = ViewModelProvider(nav.getBackStackEntry<AppGraph.Scheduling>())[SchedulingViewModel::class.java]
            assertNotSame(old, fresh); assertNull(fresh.uiState.value.selectedCenter)
        }
    }
    @Test fun temporaryRefreshPreservesEditorModelAndUnsavedFieldsThenRemovesNotice() {
        open(); awaitRoute<AppRoute.Home>(); compose.runOnIdle { nav.navigate(AppRoute.EditProfile) }
        compose.onNodeWithText("Nome de exibição").performTextReplacement("Nome sintético ainda não salvo")
        lateinit var old: EditProfileViewModel
        compose.runOnIdle { old = ViewModelProvider(nav.currentBackStackEntry!!)[EditProfileViewModel::class.java] }
        emit(SessionPresence.RefreshPending)
        compose.onNodeWithText("Estamos tentando renovar sua sessão. Aguarde e tente novamente.").assertIsDisplayed()
        val noticeBounds = compose.onNodeWithText("Estamos tentando renovar sua sessão. Aguarde e tente novamente.")
            .fetchSemanticsNode().boundsInWindow
        val fieldBounds = compose.onNodeWithText("Nome de exibição").fetchSemanticsNode().boundsInWindow
        assertTrue("Aviso deve permanecer acima do campo e do teclado", noticeBounds.bottom < fieldBounds.top)
        capture("renovacao-pendente")
        compose.onNodeWithText("Nome sintético ainda não salvo").assertExists()
        emit(SessionPresence.Authenticated(a))
        compose.onNodeWithText("Estamos tentando renovar sua sessão. Aguarde e tente novamente.").assertDoesNotExist()
        compose.runOnIdle {
            assertSame(old, ViewModelProvider(nav.currentBackStackEntry!!)[EditProfileViewModel::class.java])
            assertEquals(0, logoutCalls)
        }
    }
    @Test fun coldUnknownRefreshHidesPrivateContentWithoutLosingGraphAndWaitsForIdentity() {
        source.value = SessionPresence.RefreshPending
        open(); awaitRoute<AppRoute.Home>()
        compose.onNodeWithText("Aguardando a renovação da sessão. Tentaremos novamente.").assertIsDisplayed()
        compose.onNodeWithText("Agendar doação").assertDoesNotExist()
        emit(SessionPresence.Authenticated(a))
        compose.onNodeWithText("Agendar doação").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, logoutCalls) }
    }
    @Test fun coldUnknownRefreshFollowedByLossGoesToCleanLogin() {
        source.value = SessionPresence.RefreshPending
        open(); awaitRoute<AppRoute.Home>(); expire(); assertCleanLogin()
    }
    @Test fun changedAccountCannotReusePreviousPrivateState() {
        open(); awaitRoute<AppRoute.Home>(); compose.runOnIdle { nav.navigate(AppRoute.Profile) }
        compose.onNodeWithText("Doador de teste").assertExists()
        emit(SessionPresence.Authenticated(b)); awaitRoute<AppRoute.Login>()
        compose.onNodeWithText("A conta conectada mudou. Entre novamente para continuar.").assertIsDisplayed()
        compose.runOnIdle { assertNull(nav.previousBackStackEntry); assertEquals(0, logoutCalls) }
    }
    @Test fun successfulRequestedLogoutUsesWelcomeWithoutExpiredSessionDialog() {
        open(); awaitRoute<AppRoute.Home>(); compose.runOnIdle { nav.navigate(AppRoute.Settings) }
        tap("Sair da conta"); awaitRoute<AppRoute.Welcome>()
        compose.onNodeWithText("Entre novamente").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, logoutCalls); assertNull(nav.previousBackStackEntry) }
    }
    @Test fun failedLogoutStaysInSettingsAndCanRetry() {
        logoutFails = true
        open(); awaitRoute<AppRoute.Home>(); compose.runOnIdle { nav.navigate(AppRoute.Settings) }
        tap("Sair da conta"); awaitRoute<AppRoute.Settings>()
        compose.onNodeWithText("Entre novamente").assertDoesNotExist()
        compose.runOnIdle { logoutFails = false }
        tap("Sair da conta"); awaitRoute<AppRoute.Welcome>()
        assertEquals(2, logoutCalls)
    }
    @Test fun loginResultWaitsForMatchingObserverAndSessionAloneNeverEntersMain() {
        restored = null; source.value = SessionPresence.NotAuthenticated()
        open(); awaitRoute<AppRoute.Welcome>(); tap("Já tenho uma conta")
        emit(SessionPresence.Authenticated(b))
        awaitRoute<AppRoute.Login>()
        compose.onNodeWithText("E-mail").performTextInput("synthetic@example.test")
        compose.onNodeWithText("Senha").performTextInput("Synthetic-only-1!")
        tap("Entrar"); awaitRoute<AppRoute.Login>()
        emit(SessionPresence.Authenticated(a)); awaitRoute<AppRoute.Home>()
        compose.runOnIdle { assertNull(nav.previousBackStackEntry) }
    }
    @Test fun pendingPrivateReadCannotReopenItsScreenAfterDefiniteLoss() {
        val pending = CompletableDeferred<Result<DonorProfile?>>()
        releasePending += { pending.complete(Result.success(null)) }
        val profiles = object : DonorProfileRepository by FakeDonorProfileRepository() {
            override suspend fun getCurrentProfile(): Result<DonorProfile?> = withContext(NonCancellable) { pending.await() }
        }
        open(profiles); awaitRoute<AppRoute.Home>(); compose.runOnIdle { nav.navigate(AppRoute.Profile) }
        expire(); assertCleanLogin()
        compose.runOnIdle { pending.complete(Result.success(DonorProfile(a.id, "Resposta sintética atrasada"))) }
        compose.waitForIdle(); awaitRoute<AppRoute.Login>()
        compose.onNodeWithText("Resposta sintética atrasada").assertDoesNotExist()
    }
    @Test fun recoveryProcessingAndLeavingHavePriorityOverSessionLoss() {
        open(); awaitRoute<AppRoute.Home>()
        compose.runOnIdle { recovery.value = RecoveryLinkUiState.Processing; source.value = SessionPresence.NotAuthenticated() }
        awaitRoute<AppRoute.ResetPassword>()
        compose.onNodeWithText("Validando seu link…").assertIsDisplayed()
        compose.onNodeWithText("Entre novamente").assertDoesNotExist()
        compose.runOnIdle { recovery.value = RecoveryLinkUiState.Leaving() }
        compose.onNodeWithText("Encerrando a sessão…").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, logoutCalls) }
    }
    @Test fun restoredNavigationKeepsLossNoticeAndNeverRestoresPrivateHistory() {
        open(); awaitRoute<AppRoute.Home>(); expire(); awaitRoute<AppRoute.Login>()
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle(); assertCleanLogin()
    }
    @Test fun restoredPrivateEditorDuringRetryKeepsOwnerAndDraft() {
        open(); awaitRoute<AppRoute.Home>(); compose.runOnIdle { nav.navigate(AppRoute.EditProfile) }
        compose.onNodeWithText("Nome de exibição").performTextReplacement("Rascunho sintético preservado")
        emit(SessionPresence.RefreshPending)
        restoration.emulateSavedInstanceStateRestore(); compose.waitForIdle()
        awaitRoute<AppRoute.EditProfile>()
        compose.onNodeWithText("Rascunho sintético preservado").assertExists()
        emit(SessionPresence.Authenticated(b)); awaitRoute<AppRoute.Login>()
        compose.onNodeWithText("A conta conectada mudou. Entre novamente para continuar.").assertIsDisplayed()
    }
    @Test fun lateCreationReplyCannotNavigateBackIntoPrivateSuccessAfterSessionLoss() {
        val pending = CompletableDeferred<Result<Appointment>>()
        releasePending += { pending.complete(Result.failure(DataFailure(DataError.UNKNOWN))) }
        lateinit var created: Appointment
        var calls = 0
        val appointments = object : AppointmentRepository by FakeAppointmentRepository() {
            override suspend fun createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment> {
                calls++
                created = Appointment("synthetic-late", a.id, timeSlotId, AppointmentStatus.SCHEDULED, java.time.Instant.now())
                return withContext(NonCancellable) { pending.await() }
            }
        }
        open(appointments = appointments); awaitRoute<AppRoute.Home>()
        compose.runOnIdle { nav.navigate(AppGraph.Scheduling) }
        awaitRoute<AppRoute.ScheduleCenter>()
        lateinit var model: SchedulingViewModel
        compose.runOnIdle {
            model = ViewModelProvider(nav.getBackStackEntry<AppGraph.Scheduling>())[SchedulingViewModel::class.java]
            model.selectCenter(model.uiState.value.centers.first())
            model.selectDate(java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).plusDays(3))
        }
        compose.waitForIdle()
        compose.runOnIdle { model.selectTimeSlot(model.uiState.value.timeSlots.first().id); nav.navigate(AppRoute.ScheduleConfirm) }
        compose.onNode(hasText("Confirmar agendamento") and hasClickAction()).performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, calls)
        expire(); assertCleanLogin()
        compose.runOnIdle { pending.complete(Result.success(created)) }
        compose.waitForIdle(); awaitRoute<AppRoute.Login>()
        compose.runOnIdle { assertNull(model.uiState.value.appointmentCreation.appointment) }
    }
}
