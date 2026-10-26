package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.SessionPresence
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

/** Permanece no escopo raiz, inclusive depois de a Splash sair da pilha. */
class SessionMonitorViewModel(repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<SessionMonitorUiState>(SessionMonitorUiState.Checking)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            var lastVerifiedUser: AuthenticatedUser? = null
            repository.observeSession().retryWhen { failure, _ ->
                if (failure is CancellationException) throw failure
                _uiState.value = SessionMonitorUiState.Reconnecting(lastVerifiedUser)
                delay(2_000)
                true
            }.collect { presence ->
                ensureActive()
                _uiState.value = when (presence) {
                    SessionPresence.Initializing -> SessionMonitorUiState.Checking
                    is SessionPresence.Authenticated -> {
                        lastVerifiedUser = presence.user
                        SessionMonitorUiState.Available(presence.user)
                    }
                    SessionPresence.RefreshPending -> SessionMonitorUiState.Reconnecting(lastVerifiedUser)
                    is SessionPresence.NotAuthenticated -> {
                        lastVerifiedUser = null
                        SessionMonitorUiState.Ended(presence.explicitSignOut)
                    }
                }
            }
        }
    }
}
