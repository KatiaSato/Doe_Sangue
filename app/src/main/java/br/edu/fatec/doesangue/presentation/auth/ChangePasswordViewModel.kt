package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.domain.validation.PasswordValidator
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChangePasswordViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun savePassword(
        currentPassword: String,
        newPassword: String,
        confirmation: String,
    ) {
        val current = _uiState.value
        if (current.isSaving || current.passwordChanged) return

        val validationError = validatePassword(
            currentPassword,
            newPassword,
            confirmation,
        )
        if (validationError != null) {
            _uiState.value = current.copy(errorMessage = validationError)
            return
        }

        _uiState.value = current.copy(
            isSaving = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            val result = authRepository.changePassword(
                currentPassword,
                newPassword,
            )
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
                        errorMessage = failure.toAuthErrorMessage(
                            "Não foi possível alterar sua senha. Tente novamente.",
                        ),
                    )
                },
            )
        }
    }

    private fun validatePassword(
        currentPassword: String,
        newPassword: String,
        confirmation: String,
    ): String? =
        when {
            currentPassword.isEmpty() ->
                "Informe sua senha atual."

            // Chama a função de validação existente
            !PasswordValidator.isValid(newPassword) ->
                "Use ao menos 8 caracteres com maiúscula, minúscula, número e símbolo."

            newPassword != confirmation ->
                "As senhas devem ser iguais."

            newPassword == currentPassword ->
                "Escolha uma senha diferente da atual."

            else -> null
        }
}