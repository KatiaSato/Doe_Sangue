package br.edu.fatec.doesangue.presentation.auth

data class PasswordRecoveryUiState(
    val email: String = "",
    val isSending: Boolean = false,
    val requestAccepted: Boolean = false,
    val errorMessage: String? = null,
)