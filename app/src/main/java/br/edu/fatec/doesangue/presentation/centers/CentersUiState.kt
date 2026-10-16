package br.edu.fatec.doesangue.presentation.centers

import br.edu.fatec.doesangue.domain.model.DonationCenter

data class CentersUiState(
    val isLoading: Boolean = true,
    val centers: List<DonationCenter> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
)