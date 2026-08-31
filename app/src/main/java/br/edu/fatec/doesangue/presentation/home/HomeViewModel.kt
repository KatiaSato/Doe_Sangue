package br.edu.fatec.doesangue.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.usecase.GetDonationOverviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getDonationOverview: GetDonationOverviewUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            getDonationOverview().fold(
                onSuccess = { _uiState.value = HomeUiState.Content(it) },
                onFailure = {
                    _uiState.value = HomeUiState.Error(
                        message = "Não foi possível carregar os dados de demonstração.",
                    )
                },
            )
        }
    }
}

