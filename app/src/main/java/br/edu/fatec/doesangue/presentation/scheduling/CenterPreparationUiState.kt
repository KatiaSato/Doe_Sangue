package br.edu.fatec.doesangue.presentation.scheduling

sealed interface CenterPreparationUiState {

    data object Idle : CenterPreparationUiState

    data class Loading(
        val centerId: String,
    ) : CenterPreparationUiState

    data class Ready(
        val centerId: String,
    ) : CenterPreparationUiState

    data class Error(
        val centerId: String,
        val message: String,
    ) : CenterPreparationUiState
}