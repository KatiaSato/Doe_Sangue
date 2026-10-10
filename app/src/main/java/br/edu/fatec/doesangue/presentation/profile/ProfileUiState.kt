package br.edu.fatec.doesangue.presentation.profile

import br.edu.fatec.doesangue.domain.model.DonorProfile

// Representa cada situação da consulta ao perfil.
//A sealed interface define um conjunto controlado de possibilidades. Aqui ela ajuda a evitar estados contraditórios,
// como indicar simultaneamente “perfil encontrado” e “perfil ausente”.
sealed interface ProfileUiState {
    data object Loading : ProfileUiState        //consultando o perfil

    data class Loaded(                          //perfil encontrado, com dados para exibir
        val profile: DonorProfile,
    ) : ProfileUiState

    data class Missing(                         //consulta concluida, mas a conta ainda nao possui perfil
        val displayName: String = "",
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
    ) : ProfileUiState

    data class Error(                           //nao foi possivel concluir a consulta
        val message: String,
    ) : ProfileUiState
}