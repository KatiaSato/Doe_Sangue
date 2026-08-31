package br.edu.fatec.doesangue.presentation.home

import br.edu.fatec.doesangue.domain.model.DonationOverview

sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Loading : HomeUiState
    data class Content(val overview: DonationOverview) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

