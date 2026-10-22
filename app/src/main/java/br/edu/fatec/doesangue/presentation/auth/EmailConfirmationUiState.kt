package br.edu.fatec.doesangue.presentation.auth

data class EmailConfirmationUiState(
    val email: String = "",
    val isSending: Boolean = false,
    val emailError: String? = null,
    val sendMessage: String? = null,
    val sendError: String? = null,
    val waitSeconds: Int = 0,
    val link: ConfirmationLinkState = ConfirmationLinkState.Idle,
)

sealed interface ConfirmationLinkState {
    data object Idle : ConfirmationLinkState
    data object Checking : ConfirmationLinkState
    data object Confirmed : ConfirmationLinkState
    data class Error(val message: String, val canRetry: Boolean) : ConfirmationLinkState
}
