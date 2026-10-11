package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser

// Representa cada situação da consulta da sessão na abertura.
sealed interface SessionUiState {
    data object Loading : SessionUiState

    data class Authenticated(
        val user: AuthenticatedUser,
    ) : SessionUiState

    data object Unauthenticated : SessionUiState

    data class Error(
        val message: String,
    ) : SessionUiState
}