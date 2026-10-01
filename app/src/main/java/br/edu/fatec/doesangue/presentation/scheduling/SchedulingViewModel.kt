package br.edu.fatec.doesangue.presentation.scheduling

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import br.edu.fatec.doesangue.domain.model.DonationCenter
class SchedulingViewModel : ViewModel() {
    /*
    SchedulingUiState() cria o estado inicial: nenhuma unidade selecionada.
    MutableStateFlow guarda esse estado e comunica suas atualizações.
    private restringe o acesso ao fluxo mutável a este ViewModel.
    */
    private val _uiState = MutableStateFlow(SchedulingUiState())

    val uiState: StateFlow<SchedulingUiState> = _uiState.asStateFlow()

    // Recebe a unidade escolhida e atualiza o estado do preenchimento.
    fun selectCenter(center: DonationCenter) {
        _uiState.update { currentState ->
            currentState.copy(selectedCenter = center)
        }
    }

}