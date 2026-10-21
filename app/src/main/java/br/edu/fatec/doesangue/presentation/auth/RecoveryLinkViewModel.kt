package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecoveryLinkViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecoveryLinkUiState>(
        RecoveryLinkUiState.Idle,
    )
    val uiState = _uiState.asStateFlow()

    private var exchangeJob: Job? = null

    private var lastCode: String? = null
    private val exchangeMutex = Mutex()
    private var validationSequence = 0L
    private var finishJob: Job? = null

    fun validateCode(code: String) {
        val current = _uiState.value
        if (current is RecoveryLinkUiState.Leaving || current == RecoveryLinkUiState.Finished) return

        if (code == lastCode &&
            (current == RecoveryLinkUiState.Processing ||
                    current is RecoveryLinkUiState.Validated)
        ) return

        val validationId = ++validationSequence
        exchangeJob?.cancel()
        lastCode = code

        if (code.isBlank()) {
            _uiState.value = RecoveryLinkUiState.Error(
                AuthFailure(AuthError.SESSION_UNAVAILABLE)
                    .toRecoveryLinkErrorMessage(),
            )
            return
        }

        _uiState.value = RecoveryLinkUiState.Processing

        exchangeJob = viewModelScope.launch {
            exchangeMutex.withLock {
                ensureActive()
                val result = authRepository.exchangeRecoveryCode(code)
                ensureActive()

                _uiState.value = result.fold(
                    onSuccess = { RecoveryLinkUiState.Validated(it, validationId) },
                    onFailure = {
                        RecoveryLinkUiState.Error(
                            it.toRecoveryLinkErrorMessage(),
                            canRetry = (it as? AuthFailure)?.reason in
                                setOf(AuthError.NETWORK, AuthError.RATE_LIMITED),
                        )
                    },
                )
            }
        }
    }

    fun retry() {
        val state = _uiState.value as? RecoveryLinkUiState.Error ?: return
        if (state.canRetry) lastCode?.let(::validateCode)
    }

    // A troca e a gravação compartilham a trava: a senha nunca usa a sessão de outro link.
    suspend fun updatePassword(validationId: Long, password: String): Result<Unit> =
        exchangeMutex.withLock {
            val state = _uiState.value as? RecoveryLinkUiState.Validated
            if (state == null || state.validationId != validationId) {
                return@withLock Result.failure(AuthFailure(AuthError.SESSION_UNAVAILABLE))
            }
            val session = authRepository.restoreSession()
            currentCoroutineContext().ensureActive()
            if (session.isFailure) return@withLock Result.failure(session.exceptionOrNull()!!)
            if (session.getOrNull()?.id != state.user.id || _uiState.value != state) {
                return@withLock Result.failure(AuthFailure(AuthError.SESSION_UNAVAILABLE))
            }
            authRepository.updatePassword(password).also { currentCoroutineContext().ensureActive() }
        }

    fun finishRecovery() {
        if (_uiState.value == RecoveryLinkUiState.Idle ||
            _uiState.value == RecoveryLinkUiState.Finished || finishJob?.isActive == true
        ) return
        exchangeJob?.cancel()
        validationSequence++
        _uiState.value = RecoveryLinkUiState.Leaving()
        finishJob = viewModelScope.launch {
            exchangeMutex.withLock {
                ensureActive()
                val result = authRepository.signOut()
                ensureActive()
                _uiState.value = result.fold(
                    onSuccess = { lastCode = null; RecoveryLinkUiState.Finished },
                    onFailure = {
                        RecoveryLinkUiState.Leaving("Não foi possível encerrar a sessão. Tente novamente.")
                    },
                )
            }
        }
    }

    fun acknowledgeFinished() {
        if (_uiState.value == RecoveryLinkUiState.Finished) {
            _uiState.value = RecoveryLinkUiState.Idle
        }
    }
}
