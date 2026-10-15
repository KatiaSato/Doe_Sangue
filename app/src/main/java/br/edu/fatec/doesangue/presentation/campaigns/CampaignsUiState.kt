package br.edu.fatec.doesangue.presentation.campaigns

import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.Campaign

data class CampaignsUiState(
    val selectedBloodType: BloodType? = null,
    val isLoading: Boolean = true,
    val campaigns: List<Campaign> = emptyList(),
    val errorMessage: String? = null,
)
