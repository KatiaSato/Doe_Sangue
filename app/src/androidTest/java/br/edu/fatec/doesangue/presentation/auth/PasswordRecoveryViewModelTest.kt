package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.ui.navigation.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** ViewModel real com Auth fictício: não abre Activity nem usa rede/envia e-mail. */
@RunWith(AndroidJUnit4::class)
class PasswordRecoveryViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private lateinit var model: PasswordRecoveryViewModel
    private val requests = mutableListOf<String>()
    private var answer: suspend () -> Result<Unit> = { Result.success(Unit) }

    @Before fun createModel() {
        val repository = object : AuthRepository by FakeAuthRepository() {
            override suspend fun requestPasswordReset(email: String): Result<Unit> {
                requests += email
                return answer()
            }
        }
        val factory = viewModelFactory {
            initializer { PasswordRecoveryViewModel(repository) }
        }
        onMain {
            model = ViewModelProvider(store, factory)[PasswordRecoveryViewModel::class.java]
        }
    }

    @After fun clearModel() = onMain { store.clear() }

    @Test fun emptyAndInvalidEmailNeverCallRepository() {
        onMain { model.requestPasswordReset() }
        assertEquals("Informe seu e-mail.", model.uiState.value.errorMessage)
        onMain {
            model.updateEmail("endereco-sem-formato")
            model.requestPasswordReset()
        }
        assertEquals("Informe um e-mail válido.", model.uiState.value.errorMessage)
        assertEquals("endereco-sem-formato", model.uiState.value.email)
        assertFalse(model.uiState.value.requestAccepted)
        assertFalse(model.uiState.value.isSending)
        assertTrue(requests.isEmpty())
    }

    @Test fun requestUsesTrimmedEmailAndBlocksRepeatUntilEditing() {
        assertTrue(requests.isEmpty()) // Criar o ViewModel não envia nada.
        onMain {
            model.updateEmail("  doador.teste@example.com  ")
            model.requestPasswordReset()
            model.requestPasswordReset()
        }
        assertEquals(listOf("doador.teste@example.com"), requests)
        assertEquals("doador.teste@example.com", model.uiState.value.email)
        assertTrue(model.uiState.value.requestAccepted)
        assertFalse(model.uiState.value.isSending)
        assertNull(model.uiState.value.errorMessage)
        onMain { model.updateEmail("outro.teste@example.com") }
        assertFalse(model.uiState.value.requestAccepted)
        assertNull(model.uiState.value.errorMessage)
        onMain { model.requestPasswordReset() }
        assertEquals(listOf("doador.teste@example.com", "outro.teste@example.com"), requests)
    }

    @Test fun pendingRequestBlocksEditingAndDuplicatesAndAllowsRetryAfterNetworkFailure() {
        val pending = CompletableDeferred<Result<Unit>>()
        answer = { pending.await() }
        onMain {
            model.updateEmail("doador.teste@example.com")
            model.requestPasswordReset()
            model.requestPasswordReset()
            model.updateEmail("troca.teste@example.com")
        }
        assertEquals(listOf("doador.teste@example.com"), requests)
        assertEquals("doador.teste@example.com", model.uiState.value.email)
        assertTrue(model.uiState.value.isSending)
        assertFalse(model.uiState.value.requestAccepted)
        onMain { pending.complete(Result.failure(AuthFailure(AuthError.NETWORK))) }
        assertFalse(model.uiState.value.isSending)
        assertFalse(model.uiState.value.requestAccepted)
        assertEquals(
            "Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.",
            model.uiState.value.errorMessage,
        )
        answer = { Result.success(Unit) }
        onMain { model.requestPasswordReset() }
        assertEquals(2, requests.size)
        assertTrue(model.uiState.value.requestAccepted)
        assertNull(model.uiState.value.errorMessage)
    }

    @Test fun structuredFailuresUseRecoveryMessagesAndUnknownFailureHidesInternalDetails() {
        val cases = listOf(
            AuthError.INVALID_EMAIL to
                "Este e-mail não é aceito. Confira o endereço ou informe outro e-mail válido.",
            AuthError.RATE_LIMITED to
                "Muitas tentativas. Aguarde um pouco e tente novamente.",
            AuthError.EMAIL_NOT_AUTHORIZED to
                "O envio de recuperação não está permitido para este endereço. Tente outro e-mail.",
        )
        for ((reason, message) in cases) {
            answer = { Result.failure(AuthFailure(reason)) }
            onMain {
                model.updateEmail("doador.teste@example.com")
                model.requestPasswordReset()
            }
            assertEquals(message, model.uiState.value.errorMessage)
            assertFalse(model.uiState.value.isSending)
            assertFalse(model.uiState.value.requestAccepted)
        }
        answer = { Result.failure(IllegalStateException("detalhe-interno-ficticio")) }
        onMain { model.requestPasswordReset() }
        assertEquals(
            "Não foi possível solicitar a recuperação. Tente novamente.",
            model.uiState.value.errorMessage,
        )
        onMain { model.updateEmail("corrigido.teste@example.com") }
        assertNull(model.uiState.value.errorMessage)
        assertEquals(4, requests.size)
    }

    @Test fun clearingViewModelIgnoresLateSuccessInsteadOfPublishingAcceptanceOrError() {
        val pending = CompletableDeferred<Result<Unit>>()
        answer = { withContext(NonCancellable) { pending.await() } }
        try {
            onMain {
                model.updateEmail("doador.teste@example.com")
                model.requestPasswordReset()
            }
            val beforeClear = model.uiState.value
            onMain { store.clear() }
            onMain { pending.complete(Result.success(Unit)) }
            assertEquals(beforeClear, model.uiState.value)
            assertFalse(model.uiState.value.requestAccepted)
            assertNull(model.uiState.value.errorMessage)
            assertEquals(1, requests.size)
        } finally {
            pending.complete(Result.success(Unit))
        }
    }

    private fun onMain(action: () -> Unit) {
        instrumentation.runOnMainSync { action() }
        instrumentation.waitForIdleSync()
    }
}
