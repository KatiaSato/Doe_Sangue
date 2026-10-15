package br.edu.fatec.doesangue.presentation.donations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.DonationRecord
import br.edu.fatec.doesangue.domain.repository.DonationHistoryRepository
import br.edu.fatec.doesangue.presentation.common.ReadUiState
import br.edu.fatec.doesangue.presentation.common.toReadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DonationHistoryViewModel(private val repository: DonationHistoryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<ReadUiState<List<DonationRecord>>>(ReadUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        if (loadJob?.isActive == true) return
        _uiState.value = ReadUiState.Loading
        loadJob = viewModelScope.launch {
            val result = repository.getMyDonations()
            ensureActive()
            _uiState.value = result.toReadState("Não foi possível carregar seu histórico demonstrativo. Tente novamente.")
        }
    }
}
