package br.edu.fatec.doesangue.presentation.appointments

import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import br.edu.fatec.doesangue.domain.model.DonationCenter

sealed interface AppointmentsUiState {
    data object Loading : AppointmentsUiState

    data class Loaded(
        val appointments: List<AppointmentSummary>,
        val centers: List<DonationCenter> = emptyList(),
        val centersLoading: Boolean = false,
        val centersError: String? = null,
    ) : AppointmentsUiState

    data class Error(
        val message: String,
    ) : AppointmentsUiState
}
