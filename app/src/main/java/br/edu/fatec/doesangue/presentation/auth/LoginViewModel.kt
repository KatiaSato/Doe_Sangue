package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Coordena a tentativa de login e disponibiliza o estado à tela.
class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        // Evita repetir uma tentativa em andamento ou já concluída.
        if (_uiState.value.isLoading ||
            _uiState.value.authenticatedUser != null
        ) {
            return
        }

        val emailForLogin = email.trim()

        if (emailForLogin.isBlank() || password.isEmpty()) {
            _uiState.value = LoginUiState(
                errorMessage = "Preencha o e-mail e a senha.",
            )
            return
        }

        _uiState.value = LoginUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val result = authRepository.signIn(emailForLogin, password)

                _uiState.value = result.fold(
                    onSuccess = { user ->
                        LoginUiState(authenticatedUser = user)
                    },
                    onFailure = {
                        LoginUiState(
                            errorMessage = "Não foi possível entrar. Confira seus dados e tente novamente.",
                        )
                    },
                )
            } finally {
                // Encerra o carregamento inclusive se houver cancelamento.
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}