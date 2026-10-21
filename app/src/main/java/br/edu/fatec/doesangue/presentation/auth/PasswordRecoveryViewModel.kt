package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PasswordRecoveryViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(PasswordRecoveryUiState())

    val uiState = _uiState.asStateFlow()

    private var requestJob: Job? = null

    /*
    Essa função guarda o texto digitado e limpa o resultado da tentativa
    anterior quando o e-mail muda. Durante o envio, impede alterações para
    manter o endereço associado à solicitação em andamento.
     */
    fun updateEmail(email: String) {
        if (_uiState.value.isSending) return

        _uiState.value = _uiState.value.copy(
            email = email,
            requestAccepted = false,
            errorMessage = null,
        )
    }

    /*
    A função valida o endereço, bloqueia pedidos simultâneos e atualiza o estado
    conforme o resultado. Após uma solicitação aceita, outro envio fica bloqueado
    até o e-mail ser editado.
     */
    fun requestPasswordReset() {
        val current = _uiState.value

        if (current.isSending || current.requestAccepted ||
            requestJob?.isActive == true
        ) return

        val email = current.email.trim()
        val validationError = validateEmail(email)

        if (validationError != null) {
            _uiState.value = current.copy(
                errorMessage = validationError,
            )
            return
        }

        _uiState.value = current.copy(
            email = email,
            isSending = true,
            requestAccepted = false,
            errorMessage = null,
        )

        requestJob = viewModelScope.launch {
            val result = authRepository.requestPasswordReset(email)
            ensureActive()

            _uiState.value = result.fold(
                onSuccess = {
                    _uiState.value.copy(
                        isSending = false,
                        requestAccepted = true,
                    )
                },
                onFailure = { failure ->
                    _uiState.value.copy(
                        isSending = false,
                        requestAccepted = false,
                        errorMessage =
                            failure.toPasswordRecoveryErrorMessage(),
                    )
                },
            )
        }
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) {
            return "Informe seu e-mail."
        }

        val hasValidFormat =
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                .matches(email)

        return if (hasValidFormat) {
            null
        } else {
            "Informe um e-mail válido."
        }
    }
}