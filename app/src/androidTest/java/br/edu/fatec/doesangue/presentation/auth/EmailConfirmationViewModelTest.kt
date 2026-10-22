package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.ui.navigation.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class EmailConfirmationViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private lateinit var model: EmailConfirmationViewModel
    private val sends = mutableListOf<String>()
    private val links = mutableListOf<EmailConfirmationCredential>()
    private var sendAnswer: suspend () -> Result<Unit> = { Result.success(Unit) }
    private var linkAnswer: suspend () -> Result<Unit> = { Result.success(Unit) }
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    @Before fun prepare() {
        val repo = object : AuthRepository by FakeAuthRepository() {
            override suspend fun resendEmailConfirmation(email: String): Result<Unit> { sends += email; return sendAnswer() }
            override suspend fun verifyEmailConfirmation(credential: EmailConfirmationCredential): Result<Unit> {
                links += credential; return linkAnswer()
            }
        }
        main { model = ViewModelProvider(store, viewModelFactory { initializer { EmailConfirmationViewModel(repo) } })[EmailConfirmationViewModel::class.java] }
    }
    @After fun close() = main { store.clear() }
    @Test fun validatesEmailAndSuccessUsesNeutralMessageAndPreventsRepeatedSends() {
        main { model.resend() }
        assertEquals("Informe um e-mail válido.", model.uiState.value.emailError)
        assertTrue(sends.isEmpty())
        main { model.updateEmail(" test@example.com "); model.resend(); model.resend() }
        assertEquals(listOf("test@example.com"), sends)
        assertEquals(60, model.uiState.value.waitSeconds)
        assertTrue(model.uiState.value.sendMessage!!.startsWith("Se houver um cadastro pendente"))
        main { model.updateEmail("another@example.com"); model.resend() }
        assertEquals(1, sends.size)
    }
    @Test fun pendingSendBlocksChangesAndNetworkErrorPreservesEmailForRetry() {
        val pending = CompletableDeferred<Result<Unit>>()
        sendAnswer = { pending.await() }
        main { model.updateEmail("test@example.com"); model.resend(); model.updateEmail("other@example.com"); model.resend() }
        assertEquals("test@example.com", model.uiState.value.email)
        assertTrue(model.uiState.value.isSending)
        main { pending.complete(Result.failure(AuthFailure(AuthError.NETWORK))) }
        assertFalse(model.uiState.value.isSending)
        assertTrue(model.uiState.value.sendError!!.contains("conexão"))
        sendAnswer = { Result.success(Unit) }
        main { model.resend() }
        assertEquals(2, sends.size)
    }
    @Test fun onlyKnownTransientLinkFailuresAllowRetryAndUnknownDetailsStayHidden() {
        main { model.handleLink(null) }
        assertTrue(links.isEmpty())
        assertFalse((model.uiState.value.link as ConfirmationLinkState.Error).canRetry)
        linkAnswer = { Result.failure(AuthFailure(AuthError.SESSION_UNAVAILABLE)) }
        main { model.handleLink(EmailConfirmationCredential.Code("synthetic")) }
        assertEquals(EmailConfirmationViewModel.INVALID_LINK, (model.uiState.value.link as ConfirmationLinkState.Error).message)
        main { model.retryLink() }
        assertEquals(1, links.size)
        linkAnswer = { Result.failure(AuthFailure(AuthError.NETWORK)) }
        main { model.handleLink(EmailConfirmationCredential.Code("another")) }
        assertTrue((model.uiState.value.link as ConfirmationLinkState.Error).canRetry)
        linkAnswer = { Result.success(Unit) }
        main { model.retryLink(); model.handleLink(EmailConfirmationCredential.Code("another")) }
        assertEquals(ConfirmationLinkState.Confirmed, model.uiState.value.link)
        assertEquals(3, links.size)
        linkAnswer = { Result.failure(IllegalStateException("secret-internal-detail")) }
        main { model.handleLink(EmailConfirmationCredential.Code("unknown")) }
        assertFalse((model.uiState.value.link as ConfirmationLinkState.Error).message.contains("secret-internal-detail"))
    }
    @Test fun replacingAndDismissingLinksRejectsLateNonCancellableAnswers() {
        val pending = CompletableDeferred<Result<Unit>>()
        linkAnswer = { withContext(NonCancellable) { pending.await() } }
        main { model.handleLink(EmailConfirmationCredential.Code("old")) }
        linkAnswer = { Result.success(Unit) }
        main { model.handleLink(EmailConfirmationCredential.Code("new")); pending.complete(Result.success(Unit)) }
        assertEquals(ConfirmationLinkState.Confirmed, model.uiState.value.link)
        main { model.dismissLink() }
        assertEquals(ConfirmationLinkState.Idle, model.uiState.value.link)
        val dismissed = CompletableDeferred<Result<Unit>>()
        linkAnswer = { withContext(NonCancellable) { dismissed.await() } }
        main { model.handleLink(EmailConfirmationCredential.Code("dismiss")); model.dismissLink(); dismissed.complete(Result.success(Unit)) }
        assertEquals(ConfirmationLinkState.Idle, model.uiState.value.link)
    }
    @Test fun clearingModelRejectsLateSendAndLinkResponses() {
        val pending = CompletableDeferred<Result<Unit>>()
        sendAnswer = { withContext(NonCancellable) { pending.await() } }
        linkAnswer = { withContext(NonCancellable) { pending.await() } }
        main { model.updateEmail("test@example.com"); model.resend(); model.handleLink(EmailConfirmationCredential.Code("late")); store.clear(); pending.complete(Result.success(Unit)) }
        assertTrue(model.uiState.value.isSending)
        assertEquals(ConfirmationLinkState.Checking, model.uiState.value.link)
    }
}
