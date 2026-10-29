package br.edu.fatec.doesangue.presentation.centers

import br.edu.fatec.doesangue.presentation.common.toDataMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import br.edu.fatec.doesangue.presentation.common.matchesNameOrCity

class CentersViewModel(
    private val repository: DonationCenterRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CentersUiState())

    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    private var allCenters: List<DonationCenter> = emptyList()

    init {
        loadCenters()
    }

    fun loadCenters() {
        if (loadJob?.isActive == true) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
        )

        // O Job evita consultas simultâneas;
        // ensureActive() impede aplicar uma resposta após o cancelamento da corrotina.
        loadJob = viewModelScope.launch {
            val result = repository.getCenters()
            ensureActive()

            _uiState.value = result.fold(
                onSuccess = { centers ->
                    allCenters = centers
                    _uiState.value.copy(
                        isLoading = false,
                        centers = filterCenters(
                            allCenters,
                            _uiState.value.searchQuery,
                        ),
                    )
                },
                onFailure = {
                    _uiState.value.copy(
                        isLoading = false,
                        centers = emptyList(),
                        errorMessage =
                            it.toDataMessage("Não foi possível carregar as unidades. Tente novamente."),
                    )
                },
            )
        }
    }

    fun updateSearchQuery(query: String) {
        val current = _uiState.value

        _uiState.value = current.copy(
            searchQuery = query,
            centers = if (current.isLoading || current.errorMessage != null) {
                emptyList()
            } else {
                filterCenters(allCenters, query)
            },
        )
    }

    private fun filterCenters(
        centers: List<DonationCenter>,
        query: String,
    ): List<DonationCenter> {
        return centers.filter { matchesNameOrCity(query, it.name, it.city) }
    }
}
