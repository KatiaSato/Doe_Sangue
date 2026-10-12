package br.edu.fatec.doesangue.presentation.scheduling

import br.edu.fatec.doesangue.domain.model.Appointment

data class AppointmentCreationUiState(
    val isLoading: Boolean = false,
    val appointment: Appointment? = null,
    val errorMessage: String? = null,
)