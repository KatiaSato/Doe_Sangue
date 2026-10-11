package br.edu.fatec.doesangue.presentation.auth

// Representa o andamento da saída da conta.
data class LogoutUiState(
    val isLoading: Boolean = false,
    val isSignedOut: Boolean = false,
    val errorMessage: String? = null,
)
