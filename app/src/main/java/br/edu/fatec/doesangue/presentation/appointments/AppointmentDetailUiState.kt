package br.edu.fatec.doesangue.presentation.appointments

import br.edu.fatec.doesangue.domain.model.AppointmentSummary

sealed interface AppointmentDetailUiState {
    data object Loading : AppointmentDetailUiState

    data class Loaded(
        val appointment: AppointmentSummary,
        val isCancelling: Boolean = false,
        val cancellationError: String? = null,
    ) : AppointmentDetailUiState

    data object NotFound : AppointmentDetailUiState

    data class Error(
        val message: String,
    ) : AppointmentDetailUiState
}