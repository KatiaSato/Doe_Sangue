package br.edu.fatec.doesangue.ui.navigation

import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.test.espresso.Espresso.closeSoftKeyboard
import br.edu.fatec.doesangue.MainActivity
import br.edu.fatec.doesangue.RecoveryTestApplication
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Manifest, Activity, Compose, navegação e ViewModels reais; todas as fontes fictícias. */
@RunWith(AndroidJUnit4::class)
class RecoveryActivityFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val user = AuthenticatedUser("synthetic-recovery-user")
    private val presence = kotlinx.coroutines.flow.MutableStateFlow<SessionPresence>(SessionPresence.NotAuthenticated())
    private var session: AuthenticatedUser? = null
        set(value) {
            field = value
            presence.value = value?.let(SessionPresence::Authenticated) ?: SessionPresence.NotAuthenticated()
        }
    private val exchanges = mutableListOf<String>()
    private val saves = mutableListOf<String>()
    private var signOuts = 0
    private var exchangeAnswer: suspend () -> Result<AuthenticatedUser> = { Result.success(user) }
    private var saveAnswer: suspend () -> Result<Unit> = { Result.success(Unit) }
    private var signOutAnswer: Result<Unit> = Result.success(Unit)
    private val invalidLink = "Não foi possível validar seu acesso. Solicite um novo link de recuperação."
    private val password = "Synthetic123!"
    private val successToast = "Senha salva com sucesso."

    @Suppress("DEPRECATION") // Libera o evento também no Android anterior ao API 33.
    private fun saveAndObserveToast() {
        val event = InstrumentationRegistry.getInstrumentation().uiAutomation.executeAndWaitForEvent(
            { save(); compose.waitForIdle() },
            { it.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
                it.text.any { text -> text.toString() == successToast } },
            5_000,
        )
        event.recycle()
    }

    // KeyboardType.Password mantém a marca de segurança mesmo quando o texto está visível.
    private fun assertRenderedPassword(label: String, visible: Boolean) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        assertEquals(if (visible) password else "\u2022".repeat(password.length),
            layouts.single().layoutInput.text.text)
    }

    @Before fun configure() {
        RecoveryTestApplication.pending = false
        val repository = object : AuthRepository by FakeAuthRepository() {
            override fun observeSession() = presence
            override suspend fun restoreSession() = Result.success(session)
            override suspend fun exchangeRecoveryCode(code: String): Result<AuthenticatedUser> {
                exchanges += code
                return exchangeAnswer().also { if (it.isSuccess) session = it.getOrThrow() }
            }
            override suspend fun updatePassword(password: String): Result<Unit> {
                saves += password
                return saveAnswer()
            }
            override suspend fun signOut(): Result<Unit> {
                signOuts++
                return signOutAnswer.also { if (it.isSuccess) session = null }
            }
        }
        RecoveryTestApplication.container = AppContainer(routineRepositoryOverride = br.edu.fatec.doesangue.ui.navigation.FakeReminderRoutineRepository(),
            authRepositoryOverride = repository,
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            donorProfileRepositoryOverride = FakeDonorProfileRepository(),
            appointmentRepositoryOverride = FakeAppointmentRepository(),
            campaignRepositoryOverride = FakeCampaignRepository(),
            bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository(),
        )
    }

    @After fun close() {
        android.util.Log.i("RecoveryTestStep", "close-start")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            Stage.entries.flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
                .filterIsInstance<MainActivity>().distinct().forEach { it.finish() }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        android.util.Log.i("RecoveryTestStep", "close-platform-idle")
        compose.waitUntil(10_000) {
            var active = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                active = Stage.entries.filter { it != Stage.DESTROYED }.any {
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it)
                        .any { activity -> activity is MainActivity }
                }
            }
            !active
        }
        compose.waitForIdle()
        android.util.Log.i("RecoveryTestStep", "close-compose-idle")
        RecoveryTestApplication.container = null
        RecoveryTestApplication.pending = false
    }

    private fun recoveryIntent(code: String = "synthetic-code-a") = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("br.edu.fatec.doesangue://auth/recovery?code=$code"),
        ApplicationProvider.getApplicationContext(), MainActivity::class.java,
    )

    private fun launch(intent: Intent = recoveryIntent()) {
        val context = ApplicationProvider.getApplicationContext<RecoveryTestApplication>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
        compose.waitUntil(10_000) { resumedActivity() != null }
        compose.waitUntil(10_000) {
            compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        compose.waitForIdle()
    }

    // ActivityScenario compara o Intent original; a produção remove a URI após consumi-la.
    private fun resumedActivity(): MainActivity? {
        var activity: MainActivity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().singleOrNull()
        }
        return activity
    }

    private fun onActivity(action: (MainActivity) -> Unit) {
        val activity = checkNotNull(resumedActivity())
        InstrumentationRegistry.getInstrumentation().runOnMainSync { action(activity) }
    }

    private fun newLink(code: String) {
        val previousIntent = resumedActivity()!!.intent
        onActivity { it.startActivity(recoveryIntent(code)) }
        compose.waitUntil(10_000) { resumedActivity()?.intent?.let { it !== previousIntent && it.data == null } == true }
        compose.waitForIdle()
    }

    private fun enterPasswords(value: String = password, confirmation: String = value) {
        compose.onNodeWithText("Nova senha").performScrollTo().performTextReplacement(value)
        compose.onNodeWithText("Confirmar nova senha").performScrollTo().performTextReplacement(confirmation)
    }

    private fun save() = compose.onNodeWithText("Salvar nova senha").performScrollTo().performClick()

    @Test fun coldLinkWinsOverOldSessionAndRecreationDoesNotExchangeAgain() {
        session = AuthenticatedUser("synthetic-old-user")
        launch()
        android.util.Log.i("RecoveryTestStep", "cold-launched")
        compose.onNodeWithText("Nova senha").assertExists()
        compose.onNodeWithText("Olá, Doador de teste").assertDoesNotExist()
        assertEquals(listOf("synthetic-code-a"), exchanges)
        assertTrue(RecoveryTestApplication.pending)
        onActivity { assertNull(it.intent.data) }
        enterPasswords()
        compose.onAllNodesWithText("Mostrar")[0].performClick()
        assertRenderedPassword("Nova senha", visible = true)
        android.util.Log.i("RecoveryTestStep", "cold-entered")
        closeSoftKeyboard()
        android.util.Log.i("RecoveryTestStep", "cold-keyboard-closed")
        val previous = resumedActivity()
        onActivity { it.recreate() }
        android.util.Log.i("RecoveryTestStep", "cold-recreate-requested")
        compose.waitUntil(10_000) { resumedActivity()?.let { it !== previous } == true }
        compose.waitForIdle()
        android.util.Log.i("RecoveryTestStep", "cold-recreated-idle")
        assertEquals(1, exchanges.size)
        assertEquals("", compose.onNodeWithText("Nova senha").fetchSemanticsNode()
            .config[SemanticsProperties.EditableText].text)
        compose.onNodeWithText("Nova senha").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        assertTrue(saves.isEmpty())
        android.util.Log.i("RecoveryTestStep", "cold-assertions-finished")
    }

    @Test fun warmLinkReusesActivityAndNewValidationForSameUserClearsForm() {
        session = user
        val main = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        launch(main)
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Agendar doação").fetchSemanticsNodes().isNotEmpty() }
        var original: MainActivity? = null
        onActivity { original = it }
        newLink("synthetic-code-a")
        compose.onNodeWithText("Nova senha").assertExists()
        enterPasswords()
        newLink("synthetic-code-b")
        onActivity { assertSame(original, it) }
        assertEquals(listOf("synthetic-code-a", "synthetic-code-b"), exchanges)
        save()
        compose.onAllNodesWithText("Use ao menos 8 caracteres com maiúscula, minúscula, número e símbolo.")
            .assertCountEquals(2)
        assertTrue(saves.isEmpty())
    }

    @Test fun invalidLinkNeverOpensFormAndExitSignsOutOldSession() {
        session = user
        launch(recoveryIntent().setData(Uri.parse("br.edu.fatec.doesangue://auth/recovery?code=a&code=b")))
        compose.onNodeWithText(invalidLink).assertExists()
        compose.onNodeWithText("Nova senha").assertDoesNotExist()
        assertTrue(exchanges.isEmpty())
        compose.onNodeWithText("Voltar para o login").performClick()
        compose.onNodeWithText("Entrar").assertExists()
        assertEquals(1, signOuts)
        assertNull(session)
        assertFalse(RecoveryTestApplication.pending)
    }

    @Test fun interruptedRecoveryMarkerBlocksRestoredSessionWithoutReplayingCode() {
        RecoveryTestApplication.pending = true
        session = user
        launch(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java))
        compose.onNodeWithText(invalidLink).assertExists()
        compose.onNodeWithText("Nova senha").assertDoesNotExist()
        assertTrue(exchanges.isEmpty())
    }

    @Test fun retryAfterNetworkFailureThenValidationSaveAndExit() {
        exchangeAnswer = { Result.failure(AuthFailure(AuthError.NETWORK)) }
        launch()
        compose.onNodeWithText("Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.")
            .assertExists()
        exchangeAnswer = { Result.success(user) }
        compose.onNodeWithText("Tentar novamente").performClick()
        enterPasswords(confirmation = "Different123!")
        save()
        compose.onNodeWithText("As senhas devem ser iguais.").assertExists()
        assertTrue(saves.isEmpty())
        enterPasswords()
        saveAnswer = { Result.failure(AuthFailure(AuthError.SAME_PASSWORD)) }
        save()
        compose.onNodeWithText("Escolha uma senha diferente da atual.").assertExists()
        saveAnswer = { Result.success(Unit) }
        saveAndObserveToast()
        compose.onNodeWithText("Salvar nova senha").assertDoesNotExist()
        compose.onNodeWithText("Entrar").assertExists()
        assertEquals(2, exchanges.size)
        assertEquals(listOf(password, password), saves)
        assertEquals(1, signOuts)
        assertNull(session)
    }

    @Test fun replacedSessionCannotReceivePasswordEvenWithValidatedForm() {
        launch()
        session = AuthenticatedUser("synthetic-other-user")
        enterPasswords()
        save()
        compose.onNodeWithText(invalidLink).assertExists()
        assertTrue(saves.isEmpty())
    }

    @Test fun visibilityIsIndependentPreservesValuesAndIsDisabledWhileSaving() {
        val pending = CompletableDeferred<Result<Unit>>()
        saveAnswer = { pending.await() }
        try {
            launch()
            enterPasswords()
            assertRenderedPassword("Nova senha", visible = false)
            assertRenderedPassword("Confirmar nova senha", visible = false)
            compose.onAllNodesWithText("Mostrar")[0].performScrollTo().performClick()
            assertRenderedPassword("Nova senha", visible = true)
            assertRenderedPassword("Confirmar nova senha", visible = false)
            compose.onNodeWithText("Mostrar").performScrollTo().performClick()
            assertRenderedPassword("Confirmar nova senha", visible = true)
            compose.onAllNodesWithText("Ocultar")[0].performScrollTo().performClick()
            assertRenderedPassword("Nova senha", visible = false)
            assertRenderedPassword("Confirmar nova senha", visible = true)
            compose.onNodeWithText("Ocultar").performScrollTo().performClick()
            save()
            compose.onNodeWithText("Salvando…").assertIsNotEnabled()
            compose.onAllNodesWithText("Mostrar").assertCountEquals(2)
            compose.onAllNodesWithText("Mostrar")[0].assertIsNotEnabled()
            compose.onAllNodesWithText("Mostrar")[1].assertIsNotEnabled()
            assertEquals(listOf(password), saves)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                pending.complete(Result.failure(AuthFailure(AuthError.NETWORK)))
            }
            compose.waitForIdle()
            compose.onNodeWithText("Nova senha").assertIsEnabled()
            compose.onNodeWithText("Entrar").assertDoesNotExist()
            assertEquals(0, signOuts)
        } finally { pending.complete(Result.failure(AuthFailure(AuthError.NETWORK))) }
    }

    @Test fun savedPasswordNotifiesAndAutomaticallyTriesLogoutWithoutRepeatingSaveOnRetry() {
        launch()
        signOutAnswer = Result.failure(AuthFailure(AuthError.NETWORK))
        enterPasswords()
        saveAndObserveToast()
        compose.onNodeWithText("Não foi possível encerrar a sessão. Tente novamente.").assertExists()
        compose.onNodeWithText("Entrar").assertDoesNotExist()
        assertEquals(1, signOuts)
        assertEquals(listOf(password), saves)
        signOutAnswer = Result.success(Unit)
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.onNodeWithText("Entrar").assertExists()
        assertEquals(2, signOuts)
        assertEquals(listOf(password), saves)
    }

    @Test fun secondLinkWaitsForNonCancellableSaveAndCannotReuseItsResult() {
        val pending = CompletableDeferred<Result<Unit>>()
        saveAnswer = { withContext(NonCancellable) { pending.await() } }
        try {
            launch()
            enterPasswords()
            save()
            compose.onNodeWithText("Salvando…").assertIsNotEnabled()
            newLink("synthetic-code-b")
            compose.onNodeWithText("Validando seu link…").assertExists()
            assertEquals(listOf("synthetic-code-a"), exchanges)
            InstrumentationRegistry.getInstrumentation().runOnMainSync { pending.complete(Result.success(Unit)) }
            compose.waitForIdle()
            compose.onNodeWithText("Nova senha").assertExists()
            compose.onNodeWithText("Senha alterada com sucesso.").assertDoesNotExist()
            assertEquals(listOf("synthetic-code-a", "synthetic-code-b"), exchanges)
            assertEquals(listOf(password), saves)
        } finally { pending.complete(Result.success(Unit)) }
    }

    @Test fun signOutFailureKeepsRecoveryGateAndCanRetry() {
        launch()
        signOutAnswer = Result.failure(AuthFailure(AuthError.NETWORK))
        compose.onNodeWithText("Cancelar recuperação").performScrollTo().performClick()
        compose.onNodeWithText("Não foi possível encerrar a sessão. Tente novamente.").assertExists()
        compose.onNodeWithText("Entrar").assertDoesNotExist()
        assertTrue(RecoveryTestApplication.pending)
        signOutAnswer = Result.success(Unit)
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.onNodeWithText("Entrar").assertExists()
        assertEquals(2, signOuts)
        assertFalse(RecoveryTestApplication.pending)
    }
}
