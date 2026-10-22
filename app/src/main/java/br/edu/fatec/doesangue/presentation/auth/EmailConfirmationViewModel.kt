package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.model.EmailConfirmationCredential
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EmailConfirmationViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(EmailConfirmationUiState())
    val uiState = _uiState.asStateFlow()
    private var linkJob: Job? = null
    private var credential: EmailConfirmationCredential? = null
    private var linkSequence = 0L

    fun updateEmail(value: String) {
        if (!_uiState.value.isSending) _uiState.update {
            it.copy(email = value, emailError = null, sendError = null, sendMessage = null)
        }
    }

    fun resend() {
        val current = _uiState.value
        if (current.isSending || current.waitSeconds > 0) return
        val email = current.email.trim()
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) {
            _uiState.update { it.copy(emailError = "Informe um e-mail válido.", sendMessage = null, sendError = null) }
            return
        }
        _uiState.update { it.copy(isSending = true, emailError = null, sendMessage = null, sendError = null) }
        viewModelScope.launch {
            val result = repository.resendEmailConfirmation(email)
            ensureActive()
            if (result.isSuccess) {
                _uiState.update { it.copy(isSending = false, waitSeconds = 60,
                    sendMessage = "Se houver um cadastro pendente para este e-mail, você receberá um novo link. Confira também o spam.") }
                repeat(60) {
                    delay(1_000)
                    _uiState.update { it.copy(waitSeconds = (it.waitSeconds - 1).coerceAtLeast(0)) }
                }
            } else _uiState.update { it.copy(isSending = false,
                sendError = result.exceptionOrNull()!!.toAuthErrorMessage("Não foi possível reenviar a confirmação. Tente novamente.")) }
        }
    }

    fun handleLink(value: EmailConfirmationCredential?) {
        if (value != null && value == credential && _uiState.value.link in
            setOf(ConfirmationLinkState.Checking, ConfirmationLinkState.Confirmed)) return
        val sequence = ++linkSequence
        linkJob?.cancel()
        credential = value
        if (value == null) {
            _uiState.update { it.copy(link = ConfirmationLinkState.Error(INVALID_LINK, false)) }
            return
        }
        _uiState.update { it.copy(link = ConfirmationLinkState.Checking) }
        linkJob = viewModelScope.launch {
            val result = repository.verifyEmailConfirmation(value)
            ensureActive()
            if (sequence != linkSequence) return@launch
            _uiState.update { state -> state.copy(link = result.fold(
                onSuccess = { ConfirmationLinkState.Confirmed },
                onFailure = { failure ->
                    val reason = (failure as? AuthFailure)?.reason
                    ConfirmationLinkState.Error(
                        if (reason in setOf(AuthError.SESSION_UNAVAILABLE, AuthError.EMAIL_NOT_CONFIRMED)) INVALID_LINK
                        else failure.toAuthErrorMessage("Não foi possível conferir a confirmação. Tente novamente."),
                        canRetry = reason in setOf(AuthError.NETWORK, AuthError.RATE_LIMITED),
                    )
                },
            )) }
        }
    }

    fun retryLink() {
        if ((_uiState.value.link as? ConfirmationLinkState.Error)?.canRetry == true) credential?.let(::handleLink)
    }

    fun dismissLink() {
        linkSequence++
        linkJob?.cancel()
        credential = null
        _uiState.update { it.copy(link = ConfirmationLinkState.Idle) }
    }

    companion object {
        const val INVALID_LINK = "Link inválido, expirado ou aberto em outra instalação. Solicite um novo link ou tente entrar se já confirmou seu e-mail."
    }
}
