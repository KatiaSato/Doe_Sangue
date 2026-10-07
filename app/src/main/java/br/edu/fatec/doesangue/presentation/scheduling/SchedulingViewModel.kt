package br.edu.fatec.doesangue.presentation.scheduling

import java.time.LocalDate
import java.time.LocalTime
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository

/*
* Recebe o contrato de consulta às unidades.
* O AppContainer fornece a implementação.
* O ViewModel não precisa conhecer o Supabase.
*/
class SchedulingViewModel(
    private val donationCenterRepository: DonationCenterRepository,
) : ViewModel() {
    /*
    SchedulingUiState() cria o estado inicial: nenhuma unidade selecionada.
    MutableStateFlow guarda esse estado e comunica suas atualizações.
    private restringe o acesso ao fluxo mutável a este ViewModel.
    */
    private val _uiState = MutableStateFlow(SchedulingUiState())

    val uiState: StateFlow<SchedulingUiState> = _uiState.asStateFlow()

    // Inicia a consulta uma vez para cada nova instância do ViewModel.
    init {
        loadCenters()
    }

    // Recebe a unidade escolhida e atualiza o estado do preenchimento.
    // O copy cria um novo estado com a unidade escolhida. O StateFlow comunica essa atualização
    // Ao trocar de unidade, reinicia as escolhas de data e horário.
    fun selectCenter(center: DonationCenter) {
        _uiState.update { currentState ->
            val changedCenter = currentState.selectedCenter?.id != center.id

            currentState.copy(
                selectedCenter = center,
                selectedDate = if (changedCenter) null else currentState.selectedDate,
                selectedTime = if (changedCenter) null else currentState.selectedTime,
            )
        }
    }

    // Guarda a data escolhida e limpa o horário quando a data muda.
    fun selectDate(date: LocalDate) {
        _uiState.update { currentState ->
            if (
                currentState.selectedCenter == null ||
                currentState.selectedDate == date
            ) {
                currentState
            } else {
                currentState.copy(
                    selectedDate = date,
                    selectedTime = null,
                )
            }
        }
    }

    // Guarda o horário escolhido após a seleção da unidade e da data.
    fun selectTime(time: LocalTime) {
        _uiState.update { currentState ->
            if (
                currentState.selectedCenter == null ||
                currentState.selectedDate == null
            ) {
                currentState
            } else {
                currentState.copy(selectedTime = time)
            }
        }
    }

    /*
 * Consulta as unidades pelo repository.
 * Informa o início do carregamento e atualiza o estado
 * conforme o resultado: lista recebida ou mensagem de erro.
 */
    fun loadCenters() {
        if (_uiState.value.isLoading) return

        _uiState.update { currentState ->
            currentState.copy(
                isLoading = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            donationCenterRepository.getCenters().fold(
                onSuccess = { centers ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            centers = centers,
                            isLoading = false,
                        )
                    }
                },
                onFailure = {
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = "Não foi possível carregar as unidades.",
                        )
                    }
                },
            )
        }
    }

}
