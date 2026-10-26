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
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import br.edu.fatec.doesangue.domain.usecase.GetDonationTimeSlotsUseCase
import java.util.UUID

/*
* Recebe o contrato de consulta às unidades.
* O AppContainer fornece a implementação.
* O ViewModel não precisa conhecer o Supabase.
*/
class SchedulingViewModel(
    private val donationCenterRepository: DonationCenterRepository,
    private val getDonationTimeSlots: GetDonationTimeSlotsUseCase,
    private val appointmentRepository: AppointmentRepository,
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

    private data class AppointmentRequest(
        val timeSlotId: String,
        val idempotencyKey: String,
    )

    private var appointmentRequest: AppointmentRequest? = null

    init {
        loadCenters()
    }

    // Essa função limpa a identidade do pedido e reinicia o estado de gravação
    // Ela não cancela nem apaga uma reserva no banco; apenas limpa os dados locais
    private fun resetAppointmentRequest() {
        appointmentRequest = null

        _uiState.update { current ->
            current.copy(
                appointmentCreation = AppointmentCreationUiState(),
            )
        }
    }

    // Recebe a unidade escolhida e atualiza o estado do preenchimento.
    // O copy cria um novo estado com a unidade escolhida. O StateFlow comunica essa atualização
    // Ao trocar de unidade, cancela a consulta e limpa as escolhas anteriores.
    fun selectCenter(center: DonationCenter) {
        val state = _uiState.value
        if (state.appointmentCreation.isLoading) return
        if (state.selectedCenter?.id == center.id) return

        resetAppointmentRequest()
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

    // Essa função recebe a intenção de agendar naquela unidade, evita repetir uma
    // preparação já em andamento ou concluída e solicita o catálogo.
    // Se a consulta inicial já estiver acontecendo, a guarda existente em loadCenters()
    // evita outra consulta simultânea
    fun prepareCenterFromOrigin(centerId: String) {
        val state = _uiState.value

        if (
            state.appointmentCreation.isLoading ||
            state.centerPreparation == CenterPreparationUiState.Loading(centerId) ||
            state.centerPreparation == CenterPreparationUiState.Ready(centerId)
        ) {
            return
        }

        if (centerId.isBlank()) {
            _uiState.update { current ->
                current.copy(
                    centerPreparation = CenterPreparationUiState.Error(
                        centerId = centerId,
                        message = "Não foi possível identificar a unidade. Volte e tente novamente.",
                    ),
                )
            }
            return
        }

        // resetAppointmentRequest() limpa a chave do pedido local
        resetAppointmentRequest()
        timeSlotsJob?.cancel()

        // Criar um novo SchedulingUiState limpa a data e o horário anteriores.
        _uiState.update { current ->
            SchedulingUiState(
                centers = current.centers,
                isLoading = current.isLoading,
                centerPreparation = CenterPreparationUiState.Loading(centerId),
            )
        }

        loadCenters()
    }

    // Essa função procura a unidade no catálogo recebido. Se encontrar, reutiliza selectCenter()
    // e marca Ready; se não encontrar, apresenta uma causa específica.
    private fun completeCenterPreparation() {
        val state = _uiState.value
        // O as? tenta reconhecer o estado como Loading. Caso seja outro estado, ?: return encerra a função.
        val preparation = state.centerPreparation
                as? CenterPreparationUiState.Loading ?: return

        if (
            state.isLoading ||
            state.errorMessage != null ||
            state.appointmentCreation.isLoading
        ) {
            return
        }

        val center = state.centers.firstOrNull {
            it.id == preparation.centerId
        }

        if (center == null) {
            _uiState.update { current ->
                current.copy(
                    centerPreparation = CenterPreparationUiState.Error(
                        centerId = preparation.centerId,
                        message = "Esta unidade não está mais disponível para agendamento.",
                    ),
                )
            }
            return
        }

        selectCenter(center)

        _uiState.update { current ->
            current.copy(
                selectedCenter = center,
                centerPreparation = CenterPreparationUiState.Ready(
                    preparation.centerId,
                ),
            )
        }
    }

    // Guarda a data escolhida e limpa o horário quando a data muda.
    // Guarda uma nova data e consulta os horários correspondentes.
    fun selectDate(date: LocalDate) {
        val state = _uiState.value

        if (
            state.appointmentCreation.isLoading ||
            state.selectedCenter == null ||
            state.selectedDate == date
        ) {
            return
        }

        resetAppointmentRequest()

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
        val state = _uiState.value
        if (state.appointmentCreation.isLoading) return

        val previousSlotId = state.selectedTimeSlotId
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
        if (_uiState.value.selectedTimeSlotId != previousSlotId) {
            resetAppointmentRequest()
        }
    }

    fun confirmAppointment() {
        val state = _uiState.value

        if (
            state.appointmentCreation.isLoading ||
            state.appointmentCreation.appointment != null ||
            state.isLoadingTimeSlots ||
            state.timeSlotsErrorMessage != null
        ) {
            return
        }

        val slotId = state.selectedTimeSlotId ?: return

        val center = state.selectedCenter ?: return
        val date = state.selectedDate ?: return

        val slot = state.timeSlots.firstOrNull {
            it.id == slotId && it.centerId == center.id
        } ?: return

        val localStart = slot.startsAt.atZone(ZoneId.of(center.timeZoneId))
        if (localStart.toLocalDate() != date) return
        if (localStart.toLocalTime() != state.selectedTime) return

        /*
        Se já existe um pedido, reutilizamos sua chave. Caso contrário, criamos e guardamos um novo.
        Isso permitirá tentar novamente após uma falha sem gerar outra reserva.
         */
        val request = appointmentRequest ?: AppointmentRequest(
            timeSlotId = slotId,
            idempotencyKey = UUID.randomUUID().toString(),
        ).also {
            appointmentRequest = it
        }

        // marca o início da gravação e limpa a mensagem de erro anterio
        _uiState.update { current ->
            current.copy(
                appointmentCreation = AppointmentCreationUiState(
                    isLoading = true,
                ),
            )
        }

        // viewModelScope.launch inicia a corrotina para chamar a função suspensa.
        // O repository recebe o horário e a chave que guardamos.
        // ensureActive() impede continuar se a corrotina tiver sido cancelada
        viewModelScope.launch {
            val result = appointmentRepository.createAppointment(
                timeSlotId = request.timeSlotId,
                idempotencyKey = request.idempotencyKey,
            )

            ensureActive()

            _uiState.update { current ->
                current.copy(
                    appointmentCreation = result.fold(              // O fold escolhe entre os dois caminhos do Result
                        onSuccess = { appointment ->
                            AppointmentCreationUiState(
                                appointment = appointment,
                            )
                        },
                        onFailure = {
                            AppointmentCreationUiState(
                                errorMessage =
                                    "Não foi possível confirmar o agendamento. Tente novamente.",
                            )
                        },
                    ),
                )
            }
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
        if (state.appointmentCreation.isLoading) return

        val center = state.selectedCenter ?: return
        val date = state.selectedDate ?: return

        resetAppointmentRequest()
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
            val result = donationCenterRepository.getCenters()

            ensureActive()

            result.fold(
                onSuccess = { centers ->
                    _uiState.update { current ->
                        current.copy(
                            centers = centers,
                            isLoading = false,
                        )
                    }

                    completeCenterPreparation()
                },
                onFailure = {
                    val message = "Não foi possível carregar as unidades."

                    _uiState.update { current ->
                        val preparation = current.centerPreparation
                                as? CenterPreparationUiState.Loading

                        current.copy(
                            isLoading = false,
                            errorMessage = message,
                            centerPreparation = preparation?.let {
                                CenterPreparationUiState.Error(
                                    centerId = it.centerId,
                                    message = message,
                                )
                            } ?: current.centerPreparation,
                        )
                    }
                },
            )
        }
    }

}
