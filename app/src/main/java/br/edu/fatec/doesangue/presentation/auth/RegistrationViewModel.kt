package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState = _uiState.asStateFlow()
    private var registrationJob: Job? = null

    fun reset() {
        if (registrationJob?.isActive != true) _uiState.value = RegistrationUiState()
    }

    fun updateName(value: String) {
        if (!_uiState.value.isLoading) _uiState.value = _uiState.value.copy(displayName = value, errorMessage = null)
    }

    fun updateEmail(value: String) {
        if (!_uiState.value.isLoading) _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun selectBloodType(value: BloodType) {
        if (!_uiState.value.isLoading) _uiState.value = _uiState.value.copy(bloodType = value, errorMessage = null)
    }

    fun validateIdentity(): Boolean {
        val state = _uiState.value
        val message = when {
            state.displayName.isBlank() -> "Informe seu nome."
            !EMAIL.matches(state.email.trim()) -> "Informe um e-mail válido."
            else -> null
        }
        _uiState.value = state.copy(errorMessage = message)
        return message == null
    }

    fun register(password: String, confirmation: String) {
        val current = _uiState.value
        if (current.isLoading || current.result != null || registrationJob?.isActive == true) return
        if (!validateIdentity()) return
        val type = current.bloodType
        val message = when {
            type == null -> "Escolha seu tipo sanguíneo ou Não sei."
            !isValidPassword(password) -> PASSWORD_RULE
            password != confirmation -> "As senhas devem ser iguais."
            else -> null
        }
        if (message != null) {
            _uiState.value = current.copy(errorMessage = message)
            return
        }
        _uiState.value = current.copy(isLoading = true, errorMessage = null)
        registrationJob = viewModelScope.launch {
            val result = repository.signUp(
                current.displayName.trim(), current.email.trim(), password, checkNotNull(type),
            )
            ensureActive()
            _uiState.value = result.fold(
                onSuccess = { current.copy(isLoading = false, errorMessage = null, result = it) },
                onFailure = { current.copy(isLoading = false,
                    errorMessage = "Não foi possível concluir o cadastro. Tente novamente ou entre na sua conta.") },
            )
        }
    }

    companion object {
        const val PASSWORD_RULE = "Use ao menos 8 caracteres com maiúscula, minúscula, número e símbolo."
        private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        fun isValidPassword(value: String): Boolean = value.length >= 8 &&
            value.any { it in 'A'..'Z' } && value.any { it in 'a'..'z' } &&
            value.any { it in '0'..'9' } && value.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    }
}
