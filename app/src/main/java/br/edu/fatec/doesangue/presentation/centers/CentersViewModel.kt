package br.edu.fatec.doesangue.presentation.centers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

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
                            "Não foi possível carregar as unidades. Tente novamente.",
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

    // A normalização permite que PAULINIA encontre Paulínia, por exemplo.
    // O || aceita correspondência no nome ou na cidade;
    // uma busca vazia devolve a lista recebida.
    private fun normalizeSearchText(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    private fun filterCenters(
        centers: List<DonationCenter>,
        query: String,
    ): List<DonationCenter> {
        val normalizedQuery = normalizeSearchText(query)

        if (normalizedQuery.isEmpty()) return centers

        return centers.filter { center ->
            normalizeSearchText(center.name).contains(normalizedQuery) ||
                    normalizeSearchText(center.city).contains(normalizedQuery)
        }
    }
}