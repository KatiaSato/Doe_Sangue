package br.edu.fatec.doesangue.presentation.centers

import br.edu.fatec.doesangue.domain.model.DonationCenter

sealed interface CenterDetailUiState {
    data object Loading : CenterDetailUiState

    data class Loaded(
        val center: DonationCenter,
    ) : CenterDetailUiState

    data object NotFound : CenterDetailUiState

    data class Error(
        val message: String,
    ) : CenterDetailUiState
}