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

/** ViewModel real com repository fictício; nenhuma senha real ou chamada de rede. */
@RunWith(AndroidJUnit4::class)
class ResetPasswordViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private lateinit var model: ResetPasswordViewModel
    private val requests = mutableListOf<String>()
    private var answer: suspend () -> Result<Unit> = { Result.success(Unit) }
    private val syntheticPassword = "Abcdef1!"

    @Before fun createModel() {
        val repository = object : AuthRepository by FakeAuthRepository() {
            override suspend fun updatePassword(password: String): Result<Unit> {
                requests += password
                return answer()
            }
        }
        val factory = viewModelFactory {
            initializer { ResetPasswordViewModel(repository) }
        }
        onMain { model = ViewModelProvider(store, factory)[ResetPasswordViewModel::class.java] }
    }

    @After fun clearModel() = onMain { store.clear() }

    @Test fun invalidPasswordAndMismatchedConfirmationNeverCallRepository() {
        assertTrue(requests.isEmpty())
        onMain { model.savePassword("", "") }
        assertEquals(
            "Use ao menos 8 caracteres com maiúscula, minúscula, número e símbolo.",
            model.uiState.value.errorMessage,
        )
        onMain { model.savePassword(syntheticPassword, "OutraFicticia2!") }
        assertEquals("As senhas devem ser iguais.", model.uiState.value.errorMessage)
        assertFalse(model.uiState.value.isSaving)
        assertFalse(model.uiState.value.passwordChanged)
        assertTrue(requests.isEmpty())
    }

    @Test fun successfulSavePreservesSpacesClearsValidationErrorAndBlocksFurtherSaves() {
        onMain { model.savePassword("curta", "curta") }
        val withSpace = " Abcde1!"
        onMain {
            model.savePassword(withSpace, withSpace)
            model.savePassword(withSpace, withSpace)
        }
        assertEquals(listOf(withSpace), requests)
        assertTrue(model.uiState.value.passwordChanged)
        assertFalse(model.uiState.value.isSaving)
        assertNull(model.uiState.value.errorMessage)
        val succeeded = model.uiState.value
        onMain { model.savePassword("curta", "outra") }
        assertEquals(succeeded, model.uiState.value)
        assertEquals(1, requests.size)
    }

    @Test fun pendingSaveBlocksDuplicatesAndFailureAllowsRetry() {
        val pending = CompletableDeferred<Result<Unit>>()
        answer = { pending.await() }
        try {
            onMain {
                model.savePassword(syntheticPassword, syntheticPassword)
                model.savePassword("OutraFicticia2!", "OutraFicticia2!")
                model.savePassword("curta", "curta")
            }
            assertEquals(listOf(syntheticPassword), requests)
            assertTrue(model.uiState.value.isSaving)
            assertFalse(model.uiState.value.passwordChanged)
            assertNull(model.uiState.value.errorMessage)
            onMain { pending.complete(Result.failure(AuthFailure(AuthError.NETWORK))) }
            assertFalse(model.uiState.value.isSaving)
            assertFalse(model.uiState.value.passwordChanged)
            assertEquals(
                "Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.",
                model.uiState.value.errorMessage,
            )
            answer = { Result.success(Unit) }
            onMain { model.savePassword(syntheticPassword, syntheticPassword) }
            assertEquals(2, requests.size)
            assertTrue(model.uiState.value.passwordChanged)
            assertNull(model.uiState.value.errorMessage)
        } finally {
            pending.complete(Result.success(Unit))
        }
    }

    @Test fun knownFailuresHaveSpecificMessagesAndUnknownFailureHidesRawDetails() {
        val cases = listOf(
            AuthError.WEAK_PASSWORD to "Escolha uma senha mais forte e confira os requisitos.",
            AuthError.SAME_PASSWORD to "Escolha uma senha diferente da atual.",
            AuthError.RATE_LIMITED to "Muitas tentativas. Aguarde um pouco e tente novamente.",
            AuthError.SESSION_UNAVAILABLE to
                "Não foi possível validar seu acesso. Solicite um novo link de recuperação.",
        )
        for ((reason, message) in cases) {
            answer = { Result.failure(AuthFailure(reason)) }
            onMain { model.savePassword(syntheticPassword, syntheticPassword) }
            assertEquals(message, model.uiState.value.errorMessage)
            assertFalse(model.uiState.value.isSaving)
            assertFalse(model.uiState.value.passwordChanged)
        }
        answer = { Result.failure(IllegalStateException("detalhe-interno-ficticio")) }
        onMain { model.savePassword(syntheticPassword, syntheticPassword) }
        assertEquals("Não foi possível alterar sua senha. Tente novamente.", model.uiState.value.errorMessage)
        assertFalse(model.uiState.value.passwordChanged)
        assertFalse(model.uiState.value.isSaving)
    }

    @Test fun clearingViewModelIgnoresLateSuccess() =
        assertLateResultIgnored(Result.success(Unit))

    @Test fun clearingViewModelIgnoresLateFailure() =
        assertLateResultIgnored(Result.failure(AuthFailure(AuthError.NETWORK)))

    private fun assertLateResultIgnored(result: Result<Unit>) {
        val pending = CompletableDeferred<Result<Unit>>()
        answer = { withContext(NonCancellable) { pending.await() } }
        try {
            onMain { model.savePassword(syntheticPassword, syntheticPassword) }
            val beforeClear = model.uiState.value
            onMain { store.clear() }
            onMain { pending.complete(result) }
            assertEquals(beforeClear, model.uiState.value)
            assertFalse(model.uiState.value.passwordChanged)
            assertNull(model.uiState.value.errorMessage)
            assertEquals(1, requests.size)
        } finally {
            pending.complete(result)
        }
    }

    private fun onMain(action: () -> Unit) {
        instrumentation.runOnMainSync { action() }
        instrumentation.waitForIdleSync()
    }
}
