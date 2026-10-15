package br.edu.fatec.doesangue.presentation.home

import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import br.edu.fatec.doesangue.domain.model.BloodStock
import br.edu.fatec.doesangue.domain.model.Campaign
import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.presentation.common.ReadUiState

data class HomeUiState(
    val profile: ReadUiState<DonorProfile?> = ReadUiState.Loading,
    val appointment: ReadUiState<AppointmentSummary?> = ReadUiState.Loading,
    val campaigns: ReadUiState<List<Campaign>> = ReadUiState.Loading,
    val stocks: ReadUiState<List<BloodStock>> = ReadUiState.Loading,
)

