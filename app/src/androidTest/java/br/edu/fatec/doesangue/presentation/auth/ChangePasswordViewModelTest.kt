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

/** ViewModel real; senhas fictícias e repository em memória, sem rede. */
@RunWith(AndroidJUnit4::class)
class ChangePasswordViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private lateinit var model: ChangePasswordViewModel
    private val requests = mutableListOf<Pair<String, String>>()
    private var answer: suspend () -> Result<Unit> = { Result.success(Unit) }
    private val oldPassword = "SyntheticOld1!"
    private val newPassword = "SyntheticNew2!"

    @Before fun createModel() {
        val repository = object : AuthRepository by FakeAuthRepository() {
            override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
                requests += currentPassword to newPassword
                return answer()
            }
        }
        val factory = viewModelFactory { initializer { ChangePasswordViewModel(repository) } }
        onMain { model = ViewModelProvider(store, factory)[ChangePasswordViewModel::class.java] }
    }

    @After fun clearModel() = onMain { store.clear() }

    @Test fun invalidFieldsNeverCallRepository() {
        val cases = listOf(
            Triple("", newPassword, newPassword) to "Informe sua senha atual.",
            Triple(oldPassword, "curta", "curta") to
                "Use ao menos 8 caracteres com maiúscula, minúscula, número e símbolo.",
            Triple(oldPassword, newPassword, "OutraSintetica3!") to "As senhas devem ser iguais.",
            Triple(oldPassword, oldPassword, oldPassword) to "Escolha uma senha diferente da atual.",
        )
        for ((values, expected) in cases) {
            onMain { model.savePassword(values.first, values.second, values.third) }
            assertEquals(expected, model.uiState.value.errorMessage)
            assertFalse(model.uiState.value.isSaving)
            assertFalse(model.uiState.value.passwordChanged)
        }
        assertTrue(requests.isEmpty())
    }

    @Test fun successPreservesBothValuesAcceptsLegacyCurrentPasswordAndBlocksFurtherRequests() {
        onMain { model.savePassword("", "", "") }
        val current = " x "
        val replacement = " NewSynthetic2! "
        onMain {
            model.savePassword(current, replacement, replacement)
            model.savePassword(oldPassword, newPassword, newPassword)
        }
        assertEquals(listOf(current to replacement), requests)
        assertTrue(model.uiState.value.passwordChanged)
        assertFalse(model.uiState.value.isSaving)
        assertNull(model.uiState.value.errorMessage)
        val succeeded = model.uiState.value
        onMain { model.savePassword("", "", "") }
        assertEquals(succeeded, model.uiState.value)
    }

    @Test fun pendingRequestBlocksDuplicatesAndFailureAllowsRetry() {
        val pending = CompletableDeferred<Result<Unit>>()
        answer = { pending.await() }
        try {
            onMain {
                model.savePassword(oldPassword, newPassword, newPassword)
                model.savePassword("", "", "")
                model.savePassword("DifferentOld3!", "DifferentNew4!", "DifferentNew4!")
            }
            assertEquals(listOf(oldPassword to newPassword), requests)
            assertTrue(model.uiState.value.isSaving)
            assertNull(model.uiState.value.errorMessage)
            onMain { pending.complete(Result.failure(AuthFailure(AuthError.CURRENT_PASSWORD_INCORRECT))) }
            assertFalse(model.uiState.value.isSaving)
            assertFalse(model.uiState.value.passwordChanged)
            assertEquals("Senha atual incorreta.", model.uiState.value.errorMessage)
            answer = { Result.success(Unit) }
            onMain { model.savePassword(oldPassword, newPassword, newPassword) }
            assertEquals(2, requests.size)
            assertTrue(model.uiState.value.passwordChanged)
            assertNull(model.uiState.value.errorMessage)
        } finally {
            pending.complete(Result.success(Unit))
        }
    }

    @Test fun knownFailuresHaveSpecificMessagesAndUnknownFailureDoesNotExposeDetails() {
        val cases = listOf(
            AuthError.CURRENT_PASSWORD_INCORRECT to "Senha atual incorreta.",
            AuthError.SESSION_UNAVAILABLE to "Sua sessão não está disponível. Entre novamente para continuar.",
            AuthError.WEAK_PASSWORD to "Escolha uma senha mais forte e confira os requisitos.",
            AuthError.SAME_PASSWORD to "Escolha uma senha diferente da atual.",
            AuthError.RATE_LIMITED to "Muitas tentativas. Aguarde um pouco e tente novamente.",
            AuthError.NETWORK to "Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.",
        )
        for ((reason, expected) in cases) {
            answer = { Result.failure(AuthFailure(reason)) }
            onMain { model.savePassword(oldPassword, newPassword, newPassword) }
            assertEquals(expected, model.uiState.value.errorMessage)
            assertFalse(model.uiState.value.passwordChanged)
            assertFalse(model.uiState.value.isSaving)
        }
        answer = { Result.failure(IllegalStateException("synthetic-private-detail")) }
        onMain { model.savePassword(oldPassword, newPassword, newPassword) }
        assertEquals("Não foi possível alterar sua senha. Tente novamente.", model.uiState.value.errorMessage)
        assertFalse(model.uiState.value.passwordChanged)
    }

    @Test fun clearingViewModelIgnoresLateSuccess() = assertLateResultIgnored(Result.success(Unit))

    @Test fun clearingViewModelIgnoresLateFailure() =
        assertLateResultIgnored(Result.failure(AuthFailure(AuthError.NETWORK)))

    private fun assertLateResultIgnored(result: Result<Unit>) {
        val pending = CompletableDeferred<Result<Unit>>()
        answer = { withContext(NonCancellable) { pending.await() } }
        try {
            onMain { model.savePassword(oldPassword, newPassword, newPassword) }
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
