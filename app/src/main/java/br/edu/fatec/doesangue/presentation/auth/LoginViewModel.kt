package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
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

    private var nextGoogleAttempt = 0L
    private var pendingGoogleSelection: Long? = null

    fun beginGoogleSignIn(): Long? {
        if (_uiState.value.isLoading || _uiState.value.authenticatedUser != null) return null

        val attempt = ++nextGoogleAttempt
        pendingGoogleSelection = attempt
        _uiState.value = LoginUiState(isLoading = true)
        return attempt
    }

    fun finishGoogleSelection(attempt: Long, result: GoogleSignInResult) {
        if (pendingGoogleSelection != attempt) return
        pendingGoogleSelection = null

        when (result) {
            GoogleSignInResult.Cancelled -> _uiState.value = LoginUiState()
            GoogleSignInResult.NoCredential -> _uiState.value = LoginUiState(
                errorMessage = "Nenhuma conta Google disponível. Confira as contas do dispositivo.",
            )
            GoogleSignInResult.Failed -> _uiState.value = LoginUiState(
                errorMessage = "Não foi possível selecionar a conta Google. Tente novamente.",
            )
            is GoogleSignInResult.Success -> viewModelScope.launch {
                try {
                    _uiState.value = authRepository.signInWithGoogle(result.credential).fold(
                        onSuccess = { user -> LoginUiState(authenticatedUser = user) },
                        onFailure = { failure ->
                            LoginUiState(
                                errorMessage = failure.toAuthErrorMessage(
                                    fallback = "Não foi possível entrar com Google. Tente novamente.",
                                ),
                            )
                        },
                    )
                } finally {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

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

        val emailHasValidFormat =
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                .matches(emailForLogin)

        if (!emailHasValidFormat) {
            _uiState.value = LoginUiState(
                errorMessage = "Informe um e-mail válido.",
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
                    onFailure = { failure ->
                        LoginUiState(
                            needsEmailConfirmation = (failure as? AuthFailure)?.reason == AuthError.EMAIL_NOT_CONFIRMED,
                            errorMessage = failure.toAuthErrorMessage(
                                fallback = "Não foi possível entrar. Tente novamente.",
                            ),
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
