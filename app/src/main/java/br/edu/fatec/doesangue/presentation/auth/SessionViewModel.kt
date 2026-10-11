package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Coordena a consulta da sessão na abertura do aplicativo.
class SessionViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<SessionUiState>(SessionUiState.Loading)

    val uiState: StateFlow<SessionUiState> =
        _uiState.asStateFlow()

    private var restoreJob: Job? = null

    init {
        restoreSession()
    }

    fun restoreSession() {
        // Evita consultas simultâneas.
        if (restoreJob?.isActive == true) return

        _uiState.value = SessionUiState.Loading

        restoreJob = viewModelScope.launch {
            val result = authRepository.restoreSession()

            _uiState.value = result.fold(
                onSuccess = { user ->
                    if (user == null) {
                        SessionUiState.Unauthenticated
                    } else {
                        SessionUiState.Authenticated(user)
                    }
                },
                onFailure = {
                    SessionUiState.Error(
                        message = "Não foi possível verificar sua sessão. Tente novamente.",
                    )
                },
            )
        }
    }
}