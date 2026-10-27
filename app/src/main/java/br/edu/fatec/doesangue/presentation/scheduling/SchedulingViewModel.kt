package br.edu.fatec.doesangue.presentation.scheduling

import br.edu.fatec.doesangue.presentation.common.toDataMessage

import java.time.LocalDate
import java.time.YearMonth
import java.time.Clock
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
import br.edu.fatec.doesangue.domain.model.DataError
import br.edu.fatec.doesangue.domain.model.DataFailure
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import br.edu.fatec.doesangue.domain.usecase.GetDonationTimeSlotsUseCase
import br.edu.fatec.doesangue.domain.usecase.GetAvailableDonationDatesUseCase
import java.util.UUID
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.domain.repository.SchedulingDraftStore
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CancellationException

/*
* Recebe o contrato de consulta às unidades.
* O AppContainer fornece a implementação.
* O ViewModel não precisa conhecer o Supabase.
*/
class SchedulingViewModel(
    private val donationCenterRepository: DonationCenterRepository,
    private val getDonationTimeSlots: GetDonationTimeSlotsUseCase,
    private val appointmentRepository: AppointmentRepository,
    private val getAvailableDates: GetAvailableDonationDatesUseCase,
    private val clock: Clock = Clock.systemUTC(),
    private val draftStore: SchedulingDraftStore? = null,
    private val authRepository: AuthRepository? = null,
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
    private var availableDatesJob: Job? = null
    // Inicia a consulta uma vez para cada nova instância do ViewModel.

    private data class AppointmentRequest(
        val timeSlotId: String,
        val idempotencyKey: String,
        val prepared: Boolean = false,
    )

    private var appointmentRequest: AppointmentRequest? = null

    private val _draftRecovery = MutableStateFlow(DraftRecoveryUiState(
        phase = if (draftStore == null) DraftRecoveryPhase.READY else DraftRecoveryPhase.LOADING,
    ))
    val draftRecovery = _draftRecovery.asStateFlow()
    private var draftOwner: String? = null
    private var draftStep = SchedulingStep.CENTER
    private var draftSaveJob: Job? = null
    private var draftReadJob: Job? = null
    private var draftRestoreJob: Job? = null
    private var creationJob: Job? = null
    private var sourceJob: Job? = null
    private var requestSnapshot: SchedulingDraft? = null
    private var finishingDraft = false
    private val requestProtected get() = draftStore != null && appointmentRequest?.prepared == true &&
        !_uiState.value.canReviewRejectedRequest

    init {
        loadCenters()
        if (draftStore != null) {
            requireNotNull(authRepository)
            viewModelScope.launch {
                authRepository.observeSession().collect { presence ->
                    when (presence) {
                        is SessionPresence.Authenticated -> if (draftOwner != presence.user.id) {
                            clearOwnedWork()
                            draftOwner = presence.user.id
                            readDraft()
                        }
                        is SessionPresence.NotAuthenticated -> {
                            clearOwnedWork()
                            draftOwner = null
                        }
                        else -> Unit // Refresh pendente da mesma conta não descarta o rascunho.
                    }
                }
            }
        }
    }

    // Essa função limpa a identidade do pedido e reinicia o estado de gravação
    // Ela não cancela nem apaga uma reserva no banco; apenas limpa os dados locais
    private fun resetAppointmentRequest() {
        appointmentRequest = null
        requestSnapshot = null

        _uiState.update { current ->
            current.copy(
                appointmentCreation = AppointmentCreationUiState(),
                requestLocked = false,
                canReviewRejectedRequest = false,
                draftNotice = null,
            )
        }
    }

    // Recebe a unidade escolhida e atualiza o estado do preenchimento.
    // O copy cria um novo estado com a unidade escolhida. O StateFlow comunica essa atualização
    // Ao trocar de unidade, cancela a consulta e limpa as escolhas anteriores.
    fun prepareReschedule(appointmentId: String) {
        if (requestProtected || _uiState.value.appointmentCreation.isLoading || _draftRecovery.value.phase != DraftRecoveryPhase.READY) return
        if (_uiState.value.rescheduleOrigin?.appointmentId == appointmentId || _uiState.value.rescheduleLoading) return
        val owner = draftOwner
        sourceJob?.cancel()
        resetAppointmentRequest()
        timeSlotsJob?.cancel(); availableDatesJob?.cancel()
        _uiState.update { SchedulingUiState(centers=it.centers,isLoading=it.isLoading,rescheduleLoading=true) }
        sourceJob = viewModelScope.launch {
            val result = appointmentRepository.getMyAppointments()
            ensureActive()
            if (draftStore != null && owner != draftOwner) return@launch
            result.fold(onSuccess = { summaries ->
                val summary = summaries.firstOrNull { it.appointment.id == appointmentId && (owner == null || it.appointment.donorId == owner) }
                val error = when {
                    summary == null -> DataError.APPOINTMENT_UNAVAILABLE
                    summary.appointment.status != AppointmentStatus.SCHEDULED || summary.startsAt <= clock.instant() -> DataError.RESCHEDULING_CLOSED
                    else -> null
                }
                val origin = if (error == null && summary != null) runCatching { RescheduleOrigin.from(summary) }.getOrNull() else null
                val reason = error ?: if (origin == null) DataError.INVALID_DATA else null
                if (reason != null) _uiState.update { it.copy(rescheduleLoading=false,rescheduleError=DataFailure(reason).toDataMessage("Não foi possível preparar a remarcação.")) }
                else _uiState.update { it.copy(rescheduleLoading=false,rescheduleOrigin=origin,routine=null) }
            },onFailure = { failure -> _uiState.update { it.copy(rescheduleLoading=false,rescheduleError=failure.toDataMessage("Não foi possível consultar o agendamento. Tente novamente.")) } })
        }
    }

    fun selectRoutine(choice: RoutineChoice?) {
        if (_uiState.value.appointmentCreation.isLoading || requestProtected || _uiState.value.rescheduleOrigin != null) return
        choice?.validated()
        if (_uiState.value.routine == choice) return
        resetAppointmentRequest()
        _uiState.update { it.copy(routine=choice) }
        persistDraft()
    }

    fun selectCenter(center: DonationCenter) {
        val state = _uiState.value
        if ((state.appointmentCreation.isLoading || requestProtected)) return
        if (state.selectedCenter?.id == center.id) return

        resetAppointmentRequest()
        timeSlotsJob?.cancel()
        availableDatesJob?.cancel()

        _uiState.update { current ->
            current.copy(
                selectedCenter = center,
                selectedDate = null,
                selectedTime = null,
                selectedTimeSlotId = null,
                timeSlots = emptyList(),
                isLoadingTimeSlots = false,
                timeSlotsErrorMessage = null,
                calendarAvailability = CalendarAvailabilityUiState(),
            )
        }
        persistDraft()
    }

    // Essa função recebe a intenção de agendar naquela unidade, evita repetir uma
    // preparação já em andamento ou concluída e solicita o catálogo.
    // Se a consulta inicial já estiver acontecendo, a guarda existente em loadCenters()
    // evita outra consulta simultânea
    fun prepareCenterFromOrigin(centerId: String) {
        val state = _uiState.value

        if (
            (state.appointmentCreation.isLoading || requestProtected) ||
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
        availableDatesJob?.cancel()

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
            (state.appointmentCreation.isLoading || requestProtected)
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
            (state.appointmentCreation.isLoading || requestProtected) ||
            state.selectedCenter == null ||
            state.selectedDate == date
        ) {
            return
        }

        if (date.isBefore(LocalDate.now(clock.withZone(ZoneId.of(state.selectedCenter.timeZoneId))))) return
        resetAppointmentRequest()

        _uiState.update { current ->
            current.copy(
                selectedDate = date,
                selectedTime = null,
                selectedTimeSlotId = null,
            )
        }

        loadTimeSlots()
        persistDraft()
    }

    // Localiza e valida o registro de horário escolhido pelo seu ID.
    fun selectTimeSlot(slotId: String) {
        val state = _uiState.value
        if ((state.appointmentCreation.isLoading || requestProtected)) return

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

            if (slot.availableSpots <= 0 || slot.startsAt <= clock.instant()) return@update current

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
            persistDraft()
        }
    }

    fun confirmAppointment() {
        val state = _uiState.value
        if (state.appointmentCreation.isLoading || state.appointmentCreation.appointment != null ||
            _draftRecovery.value.phase != DraftRecoveryPhase.READY) return
        val slotId = state.selectedTimeSlotId ?: return
        val center = state.selectedCenter ?: return
        val date = state.selectedDate ?: return
        val existing = appointmentRequest
        if (!(draftStore != null && existing?.prepared == true)) {
            val origin = state.rescheduleOrigin
            val reason = when {
                origin != null && origin.startsAt <= clock.instant() -> DataError.RESCHEDULING_CLOSED
                origin != null && origin.timeSlotId == slotId -> DataError.SLOT_SAME
                else -> null
            }
            if (reason != null) {
                _uiState.update { it.copy(appointmentCreation=AppointmentCreationUiState(errorMessage=DataFailure(reason).toDataMessage("Não foi possível remarcar."))) }
                return
            }
        }
        // Um pedido durável pode recuperar o resultado sem depender de oferta pública atual.
        val durable = draftStore != null && existing?.prepared == true
        if (!durable) {
            if (state.isLoadingTimeSlots || state.timeSlotsErrorMessage != null) return
            val slot = state.timeSlots.firstOrNull { it.id == slotId && it.centerId == center.id } ?: return
            if ((existing == null || (draftStore != null && !existing.prepared)) &&
                (slot.availableSpots <= 0 || slot.startsAt <= clock.instant())) {
                val reason = if (slot.startsAt <= clock.instant()) DataError.SLOT_STARTED else DataError.SLOT_FULL
                _uiState.update { it.copy(appointmentCreation = AppointmentCreationUiState(
                    errorMessage = DataFailure(reason).toDataMessage("Não foi possível confirmar o agendamento. Tente novamente."))) }
                return
            }
            val localStart = slot.startsAt.atZone(ZoneId.of(center.timeZoneId))
            if (localStart.toLocalDate() != date || localStart.toLocalTime() != state.selectedTime) return
        }
        val owner = draftOwner
        if (draftStore != null && owner == null) return
        val request = existing ?: AppointmentRequest(slotId, UUID.randomUUID().toString()).also { appointmentRequest = it }
        if (request.timeSlotId != slotId) return
        _uiState.update { it.copy(appointmentCreation = AppointmentCreationUiState(isLoading = true)) }
        creationJob = viewModelScope.launch {
            if (draftStore != null) {
                draftSaveJob?.cancelAndJoin()
                val snapshot = runCatching { snapshot(request) }.getOrNull() ?: run {
                    _uiState.update { it.copy(appointmentCreation = AppointmentCreationUiState(errorMessage = SAVE_ERROR)) }
                    return@launch
                }
                val saved = draftStore.save(snapshot)
                ensureActive()
                if (draftOwner != owner) return@launch
                if (saved.isFailure) {
                    _uiState.update { it.copy(appointmentCreation = AppointmentCreationUiState(errorMessage = SAVE_ERROR)) }
                    return@launch // Nenhum pedido sai antes de a chave estar durável.
                }
                appointmentRequest = request.copy(prepared = true)
                requestSnapshot = snapshot
                _uiState.update { it.copy(requestLocked = true, draftNotice = null) }
            }
            val current = _uiState.value
            val result = when {
                current.rescheduleOrigin != null -> if (owner == null) Result.failure(DataFailure(DataError.SESSION_UNAVAILABLE))
                    else appointmentRepository.rescheduleForOwner(owner,current.rescheduleOrigin.appointmentId,request.timeSlotId,request.idempotencyKey)
                current.routine != null -> if (owner == null) Result.failure(DataFailure(DataError.SESSION_UNAVAILABLE))
                    else appointmentRepository.createWithRoutineForOwner(owner,request.timeSlotId,request.idempotencyKey,current.routine)
                draftStore != null -> appointmentRepository.createAppointmentForOwner(checkNotNull(owner),request.timeSlotId,request.idempotencyKey)
                else -> appointmentRepository.createAppointment(request.timeSlotId,request.idempotencyKey)
            }
            ensureActive()
            if (draftStore != null && owner != draftOwner) return@launch
            val checked = result.mapCatching { appointment ->
                check(draftStore == null || (appointment.donorId == owner && appointment.timeSlotId == request.timeSlotId))
                appointment
            }
            val rejected = (checked.exceptionOrNull() as? DataFailure)?.reason in DEFINITE_REJECTIONS
            _uiState.update { it.copy(
                canReviewRejectedRequest = draftStore != null && rejected,
                appointmentCreation = checked.fold(
                    onSuccess = { appointment -> AppointmentCreationUiState(appointment = appointment) },
                    onFailure = { failure -> AppointmentCreationUiState(errorMessage = failure.toDataMessage(
                        "Não foi possível confirmar o agendamento. Tente novamente.")) },
                ),
            ) }
        }
    }

    // Consulta os horários da unidade e da data selecionadas.
    /* Essa função:
        Exige unidade e data selecionadas.
        Cancela a consulta anterior e informa que começou a carregar.
        Chama o caso de uso e guarda a lista recebida ou uma mensagem de erro.
        Confere se o resultado ainda corresponde à unidade e à data atuais.
     */
    fun loadAvailableDates(month: YearMonth) {
        val state = _uiState.value
        if ((state.appointmentCreation.isLoading || requestProtected)) return
        val center = state.selectedCenter ?: return
        if (state.calendarAvailability.month == month && state.calendarAvailability.isLoading) return
        availableDatesJob?.cancel()
        _uiState.update { it.copy(calendarAvailability = CalendarAvailabilityUiState(month = month, isLoading = true)) }
        availableDatesJob = viewModelScope.launch {
            val result = getAvailableDates(center, month)
            ensureActive()
            _uiState.update { current ->
                if (current.selectedCenter?.id != center.id || current.calendarAvailability.month != month) {
                    return@update current
                }
                current.copy(calendarAvailability = result.fold(
                    onSuccess = { dates -> CalendarAvailabilityUiState(month = month, dates = dates) },
                    onFailure = { CalendarAvailabilityUiState(month = month,
                        errorMessage = it.toDataMessage("Não foi possível consultar os dias disponíveis. Tente novamente.")) },
                ))
            }
        }
    }

    fun loadTimeSlots() {
        val state = _uiState.value
        if ((state.appointmentCreation.isLoading || requestProtected)) return

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

        persistDraft()
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
                                it.toDataMessage("Não foi possível carregar os horários."),
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
                    val message = it.toDataMessage("Não foi possível carregar as unidades.")

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

    // Persistência do percurso: separada das credenciais e sempre associada ao dono.
    private fun clearOwnedWork() {
        draftReadJob?.cancel(); draftRestoreJob?.cancel(); draftSaveJob?.cancel(); creationJob?.cancel(); sourceJob?.cancel()
        timeSlotsJob?.cancel(); availableDatesJob?.cancel()
        appointmentRequest = null; requestSnapshot = null; draftStep = SchedulingStep.CENTER; finishingDraft = false
        _uiState.update { SchedulingUiState(centers = it.centers, isLoading = it.isLoading, errorMessage = it.errorMessage) }
        _draftRecovery.value = DraftRecoveryUiState(phase = DraftRecoveryPhase.LOADING)
    }

    private fun readDraft() {
        val store = draftStore ?: return
        val owner = draftOwner ?: return
        draftReadJob?.cancel()
        _draftRecovery.value = DraftRecoveryUiState(phase = DraftRecoveryPhase.LOADING)
        draftReadJob = viewModelScope.launch {
            val result = store.read(owner)
            ensureActive()
            if (owner != draftOwner) return@launch
            _draftRecovery.value = result.fold(
                onSuccess = { draft ->
                    if (draft == null) DraftRecoveryUiState()
                    else if (draft.ownerId != owner || runCatching { draft.validated() }.isFailure)
                        DraftRecoveryUiState(phase = DraftRecoveryPhase.ERROR, message = READ_ERROR, canDiscard = true)
                    else DraftRecoveryUiState(phase = DraftRecoveryPhase.OFFER, draft = draft,
                        canDiscard = draft.requestKey == null)
                },
                onFailure = { DraftRecoveryUiState(phase = DraftRecoveryPhase.ERROR, message = READ_ERROR,
                    canDiscard = it is InvalidSchedulingDraft) },
            )
        }
    }

    fun retryDraftRecovery() {
        if (_draftRecovery.value.phase != DraftRecoveryPhase.ERROR) return
        if (_draftRecovery.value.draft == null) readDraft() else resumeDraft()
    }

    fun resumeDraft() {
        val store = draftStore ?: return
        val owner = draftOwner ?: return
        val draft = _draftRecovery.value.draft ?: return
        if (_draftRecovery.value.phase !in setOf(DraftRecoveryPhase.OFFER, DraftRecoveryPhase.ERROR)) return
        draftRestoreJob?.cancel()
        _draftRecovery.value = DraftRecoveryUiState(phase = DraftRecoveryPhase.RESTORING, draft = draft)
        draftRestoreJob = viewModelScope.launch {
            try {
                if (draft.ownerId != owner) throw InvalidSchedulingDraft()
                draft.validated()
                if (draft.requestKey != null) {
                    // Estes são os dados do pedido anterior, não uma oferta atual.
                    val center = DonationCenter(draft.centerId, draft.centerName, "", "", "", "", draft.timeZoneId)
                    val start = java.time.Instant.parse(draft.startsAtIso).atZone(ZoneId.of(draft.timeZoneId))
                    appointmentRequest = AppointmentRequest(checkNotNull(draft.slotId), draft.requestKey, prepared = true)
                    requestSnapshot = draft
                    draftStep = SchedulingStep.CONFIRM
                    _uiState.value = SchedulingUiState(centers = _uiState.value.centers, selectedCenter = center,
                        selectedDate = start.toLocalDate(), selectedTime = start.toLocalTime(),
                        selectedTimeSlotId = draft.slotId, requestLocked = true, rescheduleOrigin = draft.rescheduleOrigin, routine = draft.routine,
                        draftNotice = "Pedido anterior recuperado. A confirmação usará a mesma identificação; nenhuma reserva foi enviada automaticamente.")
                    _draftRecovery.value = DraftRecoveryUiState(resumeStep = SchedulingStep.CONFIRM)
                    return@launch
                }
                val restoredOrigin = draft.rescheduleOrigin?.let { saved ->
                    val summary = appointmentRepository.getMyAppointments().getOrThrow().firstOrNull {
                        it.appointment.id == saved.appointmentId && it.appointment.donorId == owner
                    } ?: throw DataFailure(DataError.APPOINTMENT_UNAVAILABLE)
                    if (summary.appointment.status != AppointmentStatus.SCHEDULED || summary.startsAt <= clock.instant())
                        throw DataFailure(DataError.RESCHEDULING_CLOSED)
                    RescheduleOrigin.from(summary)
                }
                ensureActive()
                val centers = donationCenterRepository.getCenters().getOrThrow()
                ensureActive()
                if (owner != draftOwner) return@launch
                val center = centers.firstOrNull { it.id == draft.centerId }
                if (center == null) {
                    if (store.clear(owner).isFailure) throw DraftWriteException()
                    ensureActive()
                    _uiState.value = SchedulingUiState(centers = centers,
                        rescheduleOrigin = restoredOrigin, routine = draft.routine, draftNotice = "A unidade do rascunho não está mais disponível. Escolha outra unidade.")
                    _draftRecovery.value = DraftRecoveryUiState(resumeStep = SchedulingStep.CENTER)
                    return@launch
                }
                val savedDate = draft.dateIso?.let(LocalDate::parse)
                val date = savedDate?.takeUnless { it.isBefore(LocalDate.now(clock.withZone(ZoneId.of(center.timeZoneId)))) }
                val slots = if (date != null) getDonationTimeSlots(center, date).getOrThrow() else emptyList()
                ensureActive()
                if (owner != draftOwner) return@launch
                val slot = slots.firstOrNull { it.id == draft.slotId }
                val target = if (slot != null && draft.step in setOf(SchedulingStep.RECURRENCE, SchedulingStep.CONFIRM))
                    draft.step else SchedulingStep.DATE
                draftStep = target
                val notice = when {
                    savedDate != null && date == null -> "A data do rascunho já passou. Escolha uma nova data e horário."
                    draft.slotId != null && slot == null -> "O horário do rascunho não está mais disponível. Escolha outro horário."
                    else -> "Rascunho recuperado. Confira as escolhas antes de confirmar."
                }
                _uiState.value = SchedulingUiState(centers = centers, selectedCenter = center, selectedDate = date,
                    selectedTimeSlotId = slot?.id, selectedTime = slot?.startsAt?.atZone(ZoneId.of(center.timeZoneId))?.toLocalTime(),
                    timeSlots = slots, draftNotice = notice, rescheduleOrigin = restoredOrigin, routine = draft.routine)
                val updated = snapshot() ?: throw InvalidSchedulingDraft()
                if (store.save(updated).isFailure) throw DraftWriteException()
                ensureActive()
                if (owner == draftOwner) _draftRecovery.value = DraftRecoveryUiState(resumeStep = target)
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (failure: Exception) {
                if (owner == draftOwner) _draftRecovery.value = DraftRecoveryUiState(phase = DraftRecoveryPhase.ERROR,
                    draft = draft, message = if (failure is DraftWriteException) SAVE_ERROR else
                        failure.toDataMessage("Não foi possível recuperar o rascunho. Verifique a conexão e tente novamente."),
                    canDiscard = draft.requestKey == null)
            }
        }
    }

    fun discardDraft() {
        val store = draftStore ?: return
        val owner = draftOwner ?: return
        if (!_draftRecovery.value.canDiscard) return
        draftReadJob?.cancel(); draftRestoreJob?.cancel()
        _draftRecovery.value = _draftRecovery.value.copy(phase = DraftRecoveryPhase.RESTORING)
        draftRestoreJob = viewModelScope.launch {
            val result = store.clear(owner)
            ensureActive()
            if (owner != draftOwner) return@launch
            if (result.isSuccess) {
                appointmentRequest = null; requestSnapshot = null
                _uiState.update { SchedulingUiState(centers = it.centers, isLoading = it.isLoading, errorMessage = it.errorMessage) }
                _draftRecovery.value = DraftRecoveryUiState(resumeStep = SchedulingStep.CENTER, preserveEntryIntent = true)
            } else _draftRecovery.value = DraftRecoveryUiState(phase = DraftRecoveryPhase.ERROR,
                message = "Não foi possível descartar o rascunho neste aparelho. Tente novamente.", canDiscard = true)
        }
    }

    fun consumeDraftResumeStep() {
        _draftRecovery.value = _draftRecovery.value.copy(resumeStep = null, preserveEntryIntent = false)
    }

    fun onStepVisible(step: SchedulingStep) {
        if (_draftRecovery.value.phase != DraftRecoveryPhase.READY || _draftRecovery.value.resumeStep != null) return
        if (draftStep == step) return
        draftStep = step
        persistDraft()
    }

    private fun snapshot(request: AppointmentRequest? = appointmentRequest): SchedulingDraft? {
        val owner = draftOwner ?: return null
        val state = _uiState.value
        val center = state.selectedCenter ?: return null
        if (request?.prepared == true) return requestSnapshot
        val slot = state.timeSlots.firstOrNull { it.id == state.selectedTimeSlotId }
        return SchedulingDraft(owner, center.id, center.name.take(512), center.timeZoneId,
            state.selectedDate?.toString(), slot?.id, slot?.startsAt?.toString(),
            if (request != null) SchedulingStep.CONFIRM else draftStep, request?.idempotencyKey, state.rescheduleOrigin, state.routine).validated()
    }

    fun retrySavingDraft() = persistDraft()

    private fun persistDraft() {
        val store = draftStore ?: return
        val owner = draftOwner ?: return
        if (_draftRecovery.value.phase != DraftRecoveryPhase.READY || finishingDraft) return
        val draftResult = runCatching { snapshot() }
        if (draftResult.isFailure) {
            _uiState.update { it.copy(draftSaveFailed = true, draftNotice = SAVE_ERROR) }
            return
        }
        val draft = draftResult.getOrNull()
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            val result = if (draft == null) store.clear(owner) else store.save(draft)
            ensureActive()
            if (owner == draftOwner) _uiState.update { it.copy(draftSaveFailed = result.isFailure, draftNotice = if (result.isFailure) SAVE_ERROR else it.draftNotice?.takeUnless { text -> text == SAVE_ERROR }) }
        }
    }

    fun reviewRejectedRequest() {
        val store = draftStore ?: return
        val owner = draftOwner ?: return
        if (!_uiState.value.canReviewRejectedRequest || _uiState.value.appointmentCreation.isLoading) return
        val draft = requestSnapshot?.copy(requestKey = null, step = SchedulingStep.DATE) ?: return
        _draftRecovery.value = DraftRecoveryUiState(phase = DraftRecoveryPhase.RESTORING, draft = draft)
        draftRestoreJob = viewModelScope.launch {
            draftSaveJob?.cancelAndJoin()
            val result = store.save(draft)
            ensureActive()
            if (owner != draftOwner) return@launch
            if (result.isSuccess) {
                appointmentRequest = null; requestSnapshot = null
                _uiState.update { it.copy(requestLocked = false, canReviewRejectedRequest = false) }
                _draftRecovery.value = DraftRecoveryUiState(phase = DraftRecoveryPhase.OFFER, draft = draft, canDiscard = true)
            } else _draftRecovery.value = DraftRecoveryUiState(phase = DraftRecoveryPhase.ERROR, draft = requestSnapshot,
                message = SAVE_ERROR)
        }
    }

    fun finishDraft() {
        if (finishingDraft || _uiState.value.draftFinished) return
        if (draftStore == null) { _uiState.update { it.copy(draftFinished = true) }; return }
        // Limpar um pedido exige um resultado confirmado, inclusive quando já cancelado.
        if (_uiState.value.appointmentCreation.appointment == null) {
            // Destino antigo sem resultado: sair sem apagar um eventual pedido pendente.
            _uiState.update { it.copy(draftFinished = true) }
            return
        }
        val owner = draftOwner ?: return
        finishingDraft = true
        viewModelScope.launch {
            draftSaveJob?.cancelAndJoin()
            val result = draftStore.clear(owner)
            ensureActive()
            if (owner != draftOwner) return@launch
            finishingDraft = false
            _uiState.update { it.copy(draftFinished = result.isSuccess,
                draftNotice = if (result.isFailure) "Seu pedido foi conferido, mas não foi possível concluir a limpeza local. Tente novamente." else null) }
        }
    }

    private class DraftWriteException : Exception()
    companion object {
        private const val SAVE_ERROR = "Não foi possível guardar o rascunho neste aparelho. Tente novamente antes de confirmar."
        private const val READ_ERROR = "Não foi possível ler o rascunho neste aparelho. Tente novamente. Se descartá-lo, confira seus agendamentos antes de fazer outro pedido."
        private val DEFINITE_REJECTIONS = setOf(DataError.SLOT_UNAVAILABLE, DataError.SLOT_STARTED,
            DataError.SLOT_FULL, DataError.ALREADY_BOOKED, DataError.REQUEST_CONFLICT,
            DataError.PROFILE_REQUIRED, DataError.INVALID_DATA, DataError.RESCHEDULING_CLOSED, DataError.SLOT_SAME,
            DataError.APPOINTMENT_UNAVAILABLE, DataError.ROUTINE_CHANGED, DataError.ROUTINE_DATE_PAST)
    }

}
