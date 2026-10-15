package br.edu.fatec.doesangue.presentation.campaigns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.Campaign
import br.edu.fatec.doesangue.domain.repository.CampaignRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CampaignDetailUiState {
    data object Loading : CampaignDetailUiState
    data class Loaded(val campaign: Campaign) : CampaignDetailUiState
    data object NotFound : CampaignDetailUiState
    data class Error(val message: String) : CampaignDetailUiState
}

class CampaignDetailViewModel(
    private val repository: CampaignRepository, private val campaignId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow<CampaignDetailUiState>(CampaignDetailUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null
    init { loadCampaign() }

    fun loadCampaign() {
        if (loadJob?.isActive == true) return
        _uiState.value = CampaignDetailUiState.Loading
        loadJob = viewModelScope.launch {
            val result = repository.getCampaigns()
            ensureActive()
            _uiState.value = result.fold(
                onSuccess = { rows -> rows.firstOrNull { it.id == campaignId }
                    ?.let { CampaignDetailUiState.Loaded(it) } ?: CampaignDetailUiState.NotFound },
                onFailure = { CampaignDetailUiState.Error("Não foi possível carregar a campanha. Tente novamente.") },
            )
        }
    }
}
