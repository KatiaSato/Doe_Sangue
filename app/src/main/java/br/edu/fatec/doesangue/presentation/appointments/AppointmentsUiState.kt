package br.edu.fatec.doesangue.presentation.appointments

import br.edu.fatec.doesangue.domain.model.AppointmentSummary

sealed interface AppointmentsUiState {
    data object Loading : AppointmentsUiState

    data class Loaded(
        val appointments: List<AppointmentSummary>,
    ) : AppointmentsUiState

    data class Error(
        val message: String,
    ) : AppointmentsUiState
}
