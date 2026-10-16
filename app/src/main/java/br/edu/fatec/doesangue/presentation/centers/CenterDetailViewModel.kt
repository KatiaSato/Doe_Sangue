package br.edu.fatec.doesangue.presentation.centers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CenterDetailViewModel(
    private val repository: DonationCenterRepository,
    private val centerId: String,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<CenterDetailUiState>(
            CenterDetailUiState.Loading,
        )

    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadCenter()
    }

    fun loadCenter() {
        if (loadJob?.isActive == true) return

        _uiState.value = CenterDetailUiState.Loading

        viewModelScope.launch {
            val result = repository.getCenters()
            ensureActive()

            _uiState.value = result.fold(
                onSuccess = { centers ->
                    val center = centers.firstOrNull {
                        it.id == centerId
                    }

                    if (center == null) {
                        CenterDetailUiState.NotFound
                    } else {
                        CenterDetailUiState.Loaded(center)
                    }
                },
                onFailure = {
                    CenterDetailUiState.Error(
                        message = "Não foi possível carregar a unidade. Tente novamente.",
                    )
                },
            )
        }.also { loadJob = it }
    }
}