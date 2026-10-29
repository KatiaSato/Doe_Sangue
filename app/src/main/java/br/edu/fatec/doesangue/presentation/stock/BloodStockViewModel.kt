package br.edu.fatec.doesangue.presentation.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.BloodStock
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import br.edu.fatec.doesangue.domain.repository.BloodStockRepository
import br.edu.fatec.doesangue.presentation.common.ReadUiState
import br.edu.fatec.doesangue.presentation.common.toReadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BloodStockViewModel(private val repository: BloodStockRepository,
    private val centersRepository: DonationCenterRepository? = null) : ViewModel() {
    private val _uiState = MutableStateFlow<ReadUiState<List<BloodStock>>>(ReadUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var centersJob: Job? = null
    private val _centers = MutableStateFlow<ReadUiState<List<DonationCenter>>>(ReadUiState.Loaded(emptyList()))
    val centers = _centers.asStateFlow()

    init { load(); loadCities() }

    fun loadCities() {
        val source = centersRepository ?: return
        if (centersJob?.isActive == true) return
        _centers.value = ReadUiState.Loading
        centersJob = viewModelScope.launch {
            val result = source.getCenters()
            ensureActive()
            _centers.value = result.toReadState("Busca por cidade indisponível. Consulte pelo nome da unidade ou tente novamente.")
        }
    }

    fun load() {
        if (loadJob?.isActive == true) return
        _uiState.value = ReadUiState.Loading
        loadJob = viewModelScope.launch {
            val result = repository.getBloodStocks()
            ensureActive()
            _uiState.value = result.toReadState("Não foi possível carregar o estoque demonstrativo. Tente novamente.")
        }
    }
}
