package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser

// Representa o andamento e o resultado da tentativa de login.
/*
isLoading: mostrar que o login está em andamento e impedir envios repetidos.
authenticatedUser: identificar o sucesso da autenticação.
errorMessage: mostrar uma mensagem quando a tentativa falhar.
*/
data class LoginUiState(
    val isLoading: Boolean = false,
    val authenticatedUser: AuthenticatedUser? = null,
    val errorMessage: String? = null,
)