package br.edu.fatec.doesangue.presentation.scheduling

import br.edu.fatec.doesangue.domain.model.DonationCenter

data class SchedulingUiState(
    val selectedCenter: DonationCenter? = null,
)