package br.edu.fatec.doesangue.presentation.auth

data class ResetPasswordUiState(
    val isSaving: Boolean = false,
    val passwordChanged: Boolean = false,
    val errorMessage: String? = null,
)