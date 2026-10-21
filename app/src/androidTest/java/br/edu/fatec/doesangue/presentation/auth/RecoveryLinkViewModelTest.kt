package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.ui.navigation.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Coordenação real com repository fictício, sem Activity, SDK ou rede. */
@RunWith(AndroidJUnit4::class)
class RecoveryLinkViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private lateinit var model: RecoveryLinkViewModel
    private val requests = mutableListOf<String>()
    private val user = AuthenticatedUser("synthetic-user")
    private var inFlight = 0
    private var maxInFlight = 0
    private var updates = 0
    private var exits = 0
    private var answer: suspend (String) -> Result<AuthenticatedUser> = { Result.success(user) }

    @Before fun createModel() {
        val repository = object : AuthRepository by FakeAuthRepository() {
            override suspend fun restoreSession() = Result.success(user)
            override suspend fun updatePassword(password: String): Result<Unit> {
                updates++
                return Result.success(Unit)
            }
            override suspend fun signOut(): Result<Unit> {
                exits++
                return Result.success(Unit)
            }
            override suspend fun exchangeRecoveryCode(code: String): Result<AuthenticatedUser> {
                requests += code
                inFlight++
                maxInFlight = maxOf(maxInFlight, inFlight)
                return try { answer(code) } finally { inFlight-- }
            }
        }
        val factory = viewModelFactory { initializer { RecoveryLinkViewModel(repository) } }
        onMain { model = ViewModelProvider(store, factory)[RecoveryLinkViewModel::class.java] }
    }

    @After fun clearModel() = onMain { store.clear() }

    @Test fun newCodeForSameUserHasNewValidationAndRejectsOldForm() = runBlocking {
        onMain { model.validateCode("synthetic-code-a") }
        val first = model.uiState.value as RecoveryLinkUiState.Validated
        onMain { model.validateCode("synthetic-code-b") }
        val second = model.uiState.value as RecoveryLinkUiState.Validated
        assertEquals(first.user, second.user)
        assertTrue(second.validationId > first.validationId)
        val rejected = withContext(Dispatchers.Main) {
            model.updatePassword(first.validationId, "Synthetic123!")
        }
        assertEquals(AuthError.SESSION_UNAVAILABLE, (rejected.exceptionOrNull() as AuthFailure).reason)
        assertEquals(0, updates)
        assertTrue(withContext(Dispatchers.Main) {
            model.updatePassword(second.validationId, "Synthetic123!")
        }.isSuccess)
        assertEquals(1, updates)
    }

    @Test fun exitWaitsForLateExchangeThenSignsOutAndCanBeAcknowledged() {
        val pending = CompletableDeferred<Result<AuthenticatedUser>>()
        answer = { withContext(NonCancellable) { pending.await() } }
        try {
            onMain { model.validateCode("synthetic-code-a") }
            onMain { model.finishRecovery(); model.finishRecovery() }
            assertEquals(RecoveryLinkUiState.Leaving(), model.uiState.value)
            assertEquals(0, exits)
            onMain { pending.complete(Result.success(user)) }
            assertEquals(RecoveryLinkUiState.Finished, model.uiState.value)
            assertEquals(1, exits)
            onMain { model.acknowledgeFinished() }
            assertEquals(RecoveryLinkUiState.Idle, model.uiState.value)
        } finally { pending.complete(Result.success(user)) }
    }

    @Test fun startsIdleWithoutRequestAndRejectsBlankLocally() {
        assertEquals(RecoveryLinkUiState.Idle, model.uiState.value)
        assertTrue(requests.isEmpty())
        onMain { model.validateCode(" ") }
        assertEquals(
            RecoveryLinkUiState.Error("Não foi possível validar seu acesso. Solicite um novo link de recuperação."),
            model.uiState.value,
        )
        assertTrue(requests.isEmpty())
    }

    @Test fun validatedCodeIsNotExchangedAgainButDifferentCodeCanValidateAnotherUser() {
        onMain {
            model.validateCode("synthetic-code-a")
            model.validateCode("synthetic-code-a")
        }
        assertEquals(listOf("synthetic-code-a"), requests)
        assertEquals(user, (model.uiState.value as RecoveryLinkUiState.Validated).user)
        val another = AuthenticatedUser("another-synthetic-user")
        answer = { Result.success(another) }
        onMain { model.validateCode("synthetic-code-b") }
        assertEquals(listOf("synthetic-code-a", "synthetic-code-b"), requests)
        assertEquals(another, (model.uiState.value as RecoveryLinkUiState.Validated).user)
    }

    @Test fun pendingDuplicateIsIgnoredAndSameCodeCanRetryAfterNetworkFailure() {
        val pending = CompletableDeferred<Result<AuthenticatedUser>>()
        answer = { pending.await() }
        try {
            onMain {
                model.validateCode("synthetic-code-a")
                model.validateCode("synthetic-code-a")
            }
            assertEquals(RecoveryLinkUiState.Processing, model.uiState.value)
            assertEquals(1, requests.size)
            onMain { pending.complete(Result.failure(AuthFailure(AuthError.NETWORK))) }
            assertEquals(
                RecoveryLinkUiState.Error("Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.", canRetry = true),
                model.uiState.value,
            )
            answer = { Result.success(user) }
            onMain { model.validateCode("synthetic-code-a") }
            assertEquals(2, requests.size)
            assertEquals(user, (model.uiState.value as RecoveryLinkUiState.Validated).user)
        } finally { pending.complete(Result.success(user)) }
    }

    @Test fun newestCodeWaitsForCancelledExchangeAndSupersedesQueuedIntermediateCode() {
        val pending = CompletableDeferred<Result<AuthenticatedUser>>()
        val lastUser = AuthenticatedUser("last-synthetic-user")
        answer = { code ->
            if (code == "synthetic-code-a") {
                withContext(NonCancellable) { pending.await() }
            } else Result.success(lastUser)
        }
        try {
            onMain { model.validateCode("synthetic-code-a") }
            onMain {
                model.validateCode("synthetic-code-b")
                model.validateCode("synthetic-code-c")
                model.validateCode("synthetic-code-c")
            }
            assertEquals(listOf("synthetic-code-a"), requests)
            assertEquals(RecoveryLinkUiState.Processing, model.uiState.value)
            assertEquals(1, maxInFlight)
            onMain { pending.complete(Result.success(user)) }
            assertEquals(listOf("synthetic-code-a", "synthetic-code-c"), requests)
            assertEquals(lastUser, (model.uiState.value as RecoveryLinkUiState.Validated).user)
            assertEquals(1, maxInFlight)
            assertEquals(0, inFlight)
        } finally { pending.complete(Result.success(user)) }
    }

    @Test fun blankReplacementCancelsPendingCodeAndKeepsErrorAfterLateSuccess() {
        val pending = CompletableDeferred<Result<AuthenticatedUser>>()
        answer = { withContext(NonCancellable) { pending.await() } }
        try {
            onMain { model.validateCode("synthetic-code-a") }
            onMain { model.validateCode("") }
            val rejected = model.uiState.value
            assertTrue(rejected is RecoveryLinkUiState.Error)
            onMain { pending.complete(Result.success(user)) }
            assertEquals(rejected, model.uiState.value)
            assertEquals(1, requests.size)
        } finally { pending.complete(Result.success(user)) }
    }

    @Test fun clearingViewModelIgnoresLateSuccess() =
        assertResultIgnoredAfterClear(Result.success(user))

    @Test fun clearingViewModelIgnoresLateFailure() =
        assertResultIgnoredAfterClear(Result.failure(AuthFailure(AuthError.NETWORK)))

    private fun assertResultIgnoredAfterClear(result: Result<AuthenticatedUser>) {
        val pending = CompletableDeferred<Result<AuthenticatedUser>>()
        answer = { withContext(NonCancellable) { pending.await() } }
        try {
            onMain { model.validateCode("synthetic-code-a") }
            val beforeClear = model.uiState.value
            onMain { store.clear() }
            onMain { pending.complete(result) }
            assertEquals(beforeClear, model.uiState.value)
            assertEquals(1, requests.size)
        } finally { pending.complete(result) }
    }

    private fun onMain(action: () -> Unit) {
        instrumentation.runOnMainSync { action() }
        instrumentation.waitForIdleSync()
    }
}
