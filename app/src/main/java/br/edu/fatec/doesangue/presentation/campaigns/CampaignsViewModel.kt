package br.edu.fatec.doesangue.presentation.campaigns

import br.edu.fatec.doesangue.presentation.common.toDataMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.repository.CampaignRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CampaignsViewModel(private val repository: CampaignRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CampaignsUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { loadCampaigns() }

    fun selectBloodType(value: BloodType?) {
        if (value == BloodType.UNKNOWN || value == _uiState.value.selectedBloodType) return
        loadJob?.cancel()
        _uiState.value = CampaignsUiState(selectedBloodType = value)
        loadCampaigns()
    }

    fun loadCampaigns() {
        if (loadJob?.isActive == true) return
        val selected = _uiState.value.selectedBloodType
        _uiState.value = CampaignsUiState(selectedBloodType = selected)
        loadJob = viewModelScope.launch {
            val result = repository.getCampaigns(selected)
            ensureActive()
            if (_uiState.value.selectedBloodType != selected) return@launch
            _uiState.value = result.fold(
                onSuccess = { CampaignsUiState(selected, isLoading = false, campaigns = it) },
                onFailure = { CampaignsUiState(selected, isLoading = false,
                    errorMessage = it.toDataMessage("Não foi possível carregar as campanhas. Tente novamente.")) },
            )
        }
    }
}
