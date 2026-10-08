package br.edu.fatec.doesangue.presentation.scheduling

import java.time.LocalDate
import java.time.ZoneId
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import br.edu.fatec.doesangue.domain.usecase.GetDonationTimeSlotsUseCase

/*
* Recebe o contrato de consulta às unidades.
* O AppContainer fornece a implementação.
* O ViewModel não precisa conhecer o Supabase.
*/
class SchedulingViewModel(
    private val donationCenterRepository: DonationCenterRepository,
    private val getDonationTimeSlots: GetDonationTimeSlotsUseCase,
) : ViewModel() {
    /*
    SchedulingUiState() cria o estado inicial: nenhuma unidade selecionada.
    MutableStateFlow guarda esse estado e comunica suas atualizações.
    private restringe o acesso ao fluxo mutável a este ViewModel.
    */
    private val _uiState = MutableStateFlow(SchedulingUiState())

    val uiState: StateFlow<SchedulingUiState> = _uiState.asStateFlow()

    // Guarda a consulta em andamento para permitir seu cancelamento.
    private var timeSlotsJob: Job? = null
    // Inicia a consulta uma vez para cada nova instância do ViewModel.
    init {
        loadCenters()
    }

    // Recebe a unidade escolhida e atualiza o estado do preenchimento.
    // O copy cria um novo estado com a unidade escolhida. O StateFlow comunica essa atualização
    // Ao trocar de unidade, cancela a consulta e limpa as escolhas anteriores.
    fun selectCenter(center: DonationCenter) {
        if (_uiState.value.selectedCenter?.id == center.id) return

        timeSlotsJob?.cancel()

        _uiState.update { current ->
            current.copy(
                selectedCenter = center,
                selectedDate = null,
                selectedTime = null,
                selectedTimeSlotId = null,
                timeSlots = emptyList(),
                isLoadingTimeSlots = false,
                timeSlotsErrorMessage = null,
            )
        }
    }

    // Guarda a data escolhida e limpa o horário quando a data muda.
    // Guarda uma nova data e consulta os horários correspondentes.
    fun selectDate(date: LocalDate) {
        val state = _uiState.value

        if (
            state.selectedCenter == null ||
            state.selectedDate == date
        ) {
            return
        }

        _uiState.update { current ->
            current.copy(
                selectedDate = date,
                selectedTime = null,
                selectedTimeSlotId = null,
            )
        }

        loadTimeSlots()
    }

    // Localiza e valida o registro de horário escolhido pelo seu ID.
    fun selectTimeSlot(slotId: String) {
        _uiState.update { current ->
            val center = current.selectedCenter ?: return@update current
            val date = current.selectedDate ?: return@update current

            if (
                current.isLoadingTimeSlots ||
                current.timeSlotsErrorMessage != null
            ) {
                return@update current
            }

            val slot = current.timeSlots.firstOrNull { item ->
                item.id == slotId && item.centerId == center.id
            } ?: return@update current

            val localStart = slot.startsAt.atZone(
                ZoneId.of(center.timeZoneId)
            )

            if (localStart.toLocalDate() != date) {
                return@update current
            }

            current.copy(
                selectedTimeSlotId = slot.id,
                selectedTime = localStart.toLocalTime(),
            )
        }
    }


    // Consulta os horários da unidade e da data selecionadas.
    /* Essa função:
        Exige unidade e data selecionadas.
        Cancela a consulta anterior e informa que começou a carregar.
        Chama o caso de uso e guarda a lista recebida ou uma mensagem de erro.
        Confere se o resultado ainda corresponde à unidade e à data atuais.
     */
    fun loadTimeSlots() {
        val state = _uiState.value
        val center = state.selectedCenter ?: return
        val date = state.selectedDate ?: return

        timeSlotsJob?.cancel()

        _uiState.update { current ->
            current.copy(
                timeSlots = emptyList(),
                selectedTime = null,
                selectedTimeSlotId = null,
                isLoadingTimeSlots = true,
                timeSlotsErrorMessage = null,
            )
        }

        timeSlotsJob = viewModelScope.launch {
            val result = getDonationTimeSlots(center, date)

            // Não publica o resultado de uma consulta cancelada.
            ensureActive()

            _uiState.update { current ->
                if (
                    current.selectedCenter?.id != center.id ||
                    current.selectedDate != date
                ) {
                    return@update current
                }

                result.fold(
                    onSuccess = { slots ->
                        current.copy(
                            timeSlots = slots,
                            isLoadingTimeSlots = false,
                        )
                    },
                    onFailure = {
                        current.copy(
                            isLoadingTimeSlots = false,
                            timeSlotsErrorMessage =
                                "Não foi possível carregar os horários.",
                        )
                    },
                )
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
