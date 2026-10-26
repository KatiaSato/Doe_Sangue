package br.edu.fatec.doesangue.ui.navigation

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.view.WindowManager
import android.os.Looper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
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
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.Assert.*

/** Activity/manifest/navegação reais; identidade e operações somente em memória. */
class EmailConfirmationActivityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val presence = kotlinx.coroutines.flow.MutableStateFlow<SessionPresence>(SessionPresence.NotAuthenticated())
    private var session: AuthenticatedUser? = null
        set(value) {
            field = value
            presence.value = value?.let(SessionPresence::Authenticated) ?: SessionPresence.NotAuthenticated()
        }
    private var requireConfirmation = false
    private val verified = mutableListOf<EmailConfirmationCredential>()
    private val sends = mutableListOf<String>()
    private var recoveryExchanges = 0
    private var signOuts = 0
    private var verifyAnswer: suspend () -> Result<Unit> = { Result.success(Unit) }
    private var sendAnswer: suspend () -> Result<Unit> = { Result.success(Unit) }
    private fun main(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else instrumentation.runOnMainSync(block)
    }
    @Before fun setup() {
        RecoveryTestApplication.pending = false
        val repo = object : AuthRepository by FakeAuthRepository() {
            override fun observeSession() = presence
            override suspend fun restoreSession() = Result.success(session)
            override suspend fun signIn(email: String, password: String): Result<AuthenticatedUser> =
                if (requireConfirmation) Result.failure(AuthFailure(AuthError.EMAIL_NOT_CONFIRMED))
                else Result.success(AuthenticatedUser("synthetic-current-user")).onSuccess { session = it }
            override suspend fun resendEmailConfirmation(email: String): Result<Unit> { sends += email; return sendAnswer() }
            override suspend fun verifyEmailConfirmation(credential: EmailConfirmationCredential): Result<Unit> {
                verified += credential; return verifyAnswer()
            }
            override suspend fun exchangeRecoveryCode(code: String): Result<AuthenticatedUser> {
                recoveryExchanges++
                return Result.success(AuthenticatedUser("synthetic-recovery-user")).onSuccess { session = it }
            }
            override suspend fun signOut(): Result<Unit> { signOuts++; session = null; return Result.success(Unit) }
        }
        RecoveryTestApplication.container = AppContainer(authRepositoryOverride = repo,
            centerRepositoryOverride = FakeDonationCenterRepository(), timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            donorProfileRepositoryOverride = FakeDonorProfileRepository(), appointmentRepositoryOverride = FakeAppointmentRepository(),
            campaignRepositoryOverride = FakeCampaignRepository(), bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository())
    }
    private fun activity(): MainActivity? {
        var result: MainActivity? = null
        main { result = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
            .filterIsInstance<MainActivity>().singleOrNull() }
        return result
    }
    @After fun close() {
        main { Stage.entries.flatMap { ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(it) }
            .filterIsInstance<MainActivity>().distinct().forEach { it.finish() } }
        instrumentation.waitForIdleSync()
        compose.waitUntil(10_000) { activity() == null }
        RecoveryTestApplication.container = null
        RecoveryTestApplication.pending = false
    }
    private fun link(suffix: String = "?code=synthetic-code") = Intent(Intent.ACTION_VIEW,
        Uri.parse("br.edu.fatec.doesangue://auth/confirmation$suffix"))
        .setPackage(ApplicationProvider.getApplicationContext<RecoveryTestApplication>().packageName)
    private fun launch(intent: Intent = link()) {
        val context = ApplicationProvider.getApplicationContext<RecoveryTestApplication>()
        main { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) }
        compose.waitUntil(10_000) { activity() != null }
        main { activity()!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        compose.waitUntil(10_000) { compose.onAllNodes(isRoot()).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() }
        if (intent.action != Intent.ACTION_VIEW) {
            compose.mainClock.advanceTimeBy(2_000)
            val expected = if (session == null) "Já tenho uma conta" else "Agendar doação"
            compose.waitUntil(10_000) { compose.onAllNodesWithText(expected).fetchSemanticsNodes().isNotEmpty() }
        }
        compose.waitForIdle()
    }
    private fun deliver(intent: Intent) {
        val current = activity()!!
        val oldIntent = current.intent
        main { current.startActivity(intent) }
        compose.waitUntil(10_000) { activity()?.intent?.let { it !== oldIntent && it.data == null } == true }
        compose.waitForIdle()
    }
    private fun tap(text: String) {
        val node = compose.onNodeWithText(text)
        val scrollable = generateSequence(node.fetchSemanticsNode().parent) { it.parent }
            .any { it.config.contains(SemanticsActions.ScrollBy) }
        if (scrollable) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }
    private fun capture(name: String) {
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "email-confirmation-validation").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    @Test fun coldSignupReturnConfirmsWithoutLoginAndRotationDoesNotReplayCredential() {
        launch()
        compose.onNodeWithText("E-mail confirmado").assertExists()
        capture("confirmado")
        assertEquals(1, verified.size)
        assertNull(session); assertFalse(RecoveryTestApplication.pending); assertEquals(0, recoveryExchanges)
        main { assertNull(activity()!!.intent.data) }
        val oldActivity = activity()!!
        main { oldActivity.recreate() }
        compose.waitUntil(10_000) { activity()?.let { it !== oldActivity } == true }
        compose.waitForIdle()
        compose.onNodeWithText("E-mail confirmado").assertExists()
        assertEquals(1, verified.size)
        tap("Continuar")
        compose.onNodeWithText("Entrar").assertExists()
        compose.onNodeWithText("Nova senha").assertDoesNotExist()
        assertEquals(0, signOuts)
    }
    @Test fun warmResendReturnPreservesExistingAccountAndDoesNotStartRecovery() {
        session = AuthenticatedUser("synthetic-existing-user")
        launch(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java))
        compose.onNodeWithText("Agendar doação").assertExists()
        deliver(link("#access_token=synthetic-token&refresh_token=ignored&token_type=bearer&type=signup&expires_in=3600"))
        compose.onNodeWithText("E-mail confirmado").assertExists()
        tap("Continuar")
        compose.onNodeWithText("Agendar doação").assertExists()
        assertEquals("synthetic-existing-user", session?.id)
        assertEquals(0, signOuts); assertEquals(0, recoveryExchanges)
        assertFalse(RecoveryTestApplication.pending)
    }
    @Test fun expiredMalformedAndNetworkReturnsOfferOnlyValidNextActions() {
        launch(link("#error=access_denied&error_code=otp_expired&error_description=private-detail"))
        assertTrue(verified.isEmpty())
        compose.onNodeWithText("Não foi possível confirmar").assertExists()
        compose.onNodeWithText("private-detail").assertDoesNotExist()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
        tap("Continuar")
        verifyAnswer = { Result.failure(AuthFailure(AuthError.NETWORK)) }
        deliver(link("?code=retry-code"))
        compose.onNodeWithText("Tentar novamente").assertExists()
        verifyAnswer = { Result.success(Unit) }
        tap("Tentar novamente")
        compose.onNodeWithText("E-mail confirmado").assertExists()
        assertEquals(2, verified.size)
    }
    @Test fun loginNotConfirmedOpensPrefilledResendWithValidationFailureRetryAndCooldown() {
        requireConfirmation = true
        launch(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java))
        tap("Já tenho uma conta")
        compose.onNodeWithText("E-mail").performTextInput("test@example.com")
        compose.onNodeWithText("Senha").performTextInput("Synthetic123!")
        closeSoftKeyboard(); tap("Entrar"); tap("Reenviar confirmação")
        compose.onNodeWithText("E-mail").assertTextContains("test@example.com")
        compose.onNodeWithText("E-mail").performTextReplacement("invalid")
        tap("Reenviar confirmação")
        compose.onNodeWithText("Informe um e-mail válido.").assertExists()
        assertTrue(sends.isEmpty())
        compose.onNodeWithText("E-mail").performTextReplacement("test@example.com")
        sendAnswer = { Result.failure(AuthFailure(AuthError.NETWORK)) }
        closeSoftKeyboard(); tap("Reenviar confirmação")
        compose.onNodeWithText("Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.").assertExists()
        sendAnswer = { Result.success(Unit) }
        tap("Reenviar confirmação")
        compose.onNodeWithText("Se houver um cadastro pendente", substring = true).assertExists()
        capture("reenvio")
        compose.onNodeWithText("Aguarde", substring = true).assertIsNotEnabled()
        assertEquals(listOf("test@example.com", "test@example.com"), sends)
        tap("Entendi")
        compose.onNodeWithText("Entrar").assertExists()
        assertNull(session)
    }
    @Test fun pendingRegistrationOpensConfirmationWithItsEmailWithoutSendingAgainAutomatically() {
        launch(Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java))
        tap("Começar")
        compose.onNodeWithText("Nome de exibição").performTextInput("Doador sintético D")
        compose.onNodeWithText("E-mail").performTextInput("signup@example.com")
        closeSoftKeyboard(); tap("Continuar"); tap("Não sei"); tap("Continuar"); tap("Continuar")
        compose.onNodeWithText("Senha").performTextInput("Synthetic123!")
        compose.onNodeWithText("Confirmar senha").performTextInput("Synthetic123!")
        closeSoftKeyboard(); tap("Criar minha conta")
        compose.onNodeWithText("Verifique seu e-mail").assertExists()
        compose.onNodeWithText("E-mail").assertTextContains("signup@example.com")
        assertTrue(sends.isEmpty())
        tap("Reenviar confirmação")
        assertEquals(listOf("signup@example.com"), sends)
        compose.onNodeWithText("Se houver um cadastro pendente", substring = true).assertExists()
        tap("Entendi")
        compose.onNodeWithText("Senha").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        assertNull(session)
    }

    @Test fun recoveryTakesPriorityAndLateConfirmationCannotOverlayPasswordForm() {
        val pending = CompletableDeferred<Result<Unit>>()
        verifyAnswer = { withContext(NonCancellable) { pending.await() } }
        launch()
        compose.onNodeWithText("Conferindo confirmação…").assertExists()
        val recovery = Intent(Intent.ACTION_VIEW, Uri.parse("br.edu.fatec.doesangue://auth/recovery?code=synthetic-recovery"),
            ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        deliver(recovery)
        compose.onNodeWithText("Nova senha").assertExists()
        main { pending.complete(Result.success(Unit)) }
        compose.waitForIdle()
        compose.onNodeWithText("E-mail confirmado").assertDoesNotExist()
        deliver(link("?code=ignored-during-recovery"))
        compose.onNodeWithText("Nova senha").assertExists()
        assertEquals(1, verified.size); assertEquals(1, recoveryExchanges)
        assertTrue(RecoveryTestApplication.pending)
    }
}
