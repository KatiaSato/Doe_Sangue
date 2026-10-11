package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Coordena a saída e disponibiliza o estado à tela.
class LogoutViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogoutUiState())

    val uiState: StateFlow<LogoutUiState> = _uiState.asStateFlow()

    fun signOut() {
        if (_uiState.value.isLoading || _uiState.value.isSignedOut) {
            return
        }

        _uiState.value = LogoutUiState(isLoading = true)

        viewModelScope.launch {
            try {
                val result = authRepository.signOut()

                _uiState.value = result.fold(
                    onSuccess = {
                        LogoutUiState(isSignedOut = true)
                    },
                    onFailure = {
                        LogoutUiState(
                            errorMessage = "Não foi possível sair. Tente novamente.",
                        )
                    },
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}