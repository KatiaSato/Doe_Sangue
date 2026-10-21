package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.domain.validation.PasswordValidator
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResetPasswordViewModel(
    private val updatePassword: suspend (String) -> Result<Unit>,
) : ViewModel() {
    constructor(authRepository: AuthRepository) : this(authRepository::updatePassword)

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState = _uiState.asStateFlow()
    private var saveJob: Job? = null

    fun savePassword(password: String, confirmation: String) {
        val current = _uiState.value

        if (current.isSaving || current.passwordChanged ||
            saveJob?.isActive == true
        ) return

        val validationError = validatePassword(password, confirmation)
        if (validationError != null) {
            _uiState.value = current.copy(errorMessage = validationError)
            return
        }

        _uiState.value = current.copy(
            isSaving = true,
            errorMessage = null,
        )

        saveJob = viewModelScope.launch {
            val result = updatePassword(password)
            ensureActive()

            _uiState.value = result.fold(
                onSuccess = {
                    _uiState.value.copy(
                        isSaving = false,
                        passwordChanged = true,
                    )
                },
                onFailure = { failure ->
                    _uiState.value.copy(
                        isSaving = false,
                        errorMessage = failure.toResetPasswordErrorMessage(),
                    )
                },
            )
        }
    }

    private fun validatePassword(
        password: String,
        confirmation: String,
    ): String? {
        return when {
            !PasswordValidator.isValid(password) ->
                "Use ao menos 8 caracteres com maiúscula, minúscula, número e símbolo."

            password != confirmation ->
                "As senhas devem ser iguais."

            else -> null
        }
    }
}
