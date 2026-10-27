package br.edu.fatec.doesangue.presentation.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.domain.repository.ReminderRoutineRepository
import br.edu.fatec.doesangue.domain.validation.ProfileValidator
import br.edu.fatec.doesangue.presentation.common.toDataMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

data class ReminderRoutineUiState(
    val loading: Boolean = true, val saving: Boolean = false, val routine: ReminderRoutine? = null,
    val dateText: String = "", val intervalText: String = "", val timeZoneId: String = "America/Sao_Paulo",
    val dateError: String? = null, val intervalError: String? = null,
    val error: String? = null, val message: String? = null, val loaded: Boolean = false,
)

class ReminderRoutineViewModel(private val repository: ReminderRoutineRepository,
    private val auth: AuthRepository, private val clock: Clock = Clock.systemUTC()) : ViewModel() {
    private val _state = MutableStateFlow(ReminderRoutineUiState())
    val state = _state.asStateFlow()
    private var owner: String? = null
    private var work: Job? = null
    private var prefilled: RoutineChoice? = null

    init {
        viewModelScope.launch {
            auth.observeSession().collect { presence ->
                when (presence) {
                    is SessionPresence.Authenticated -> if (owner != presence.user.id) {
                        work?.cancel(); prefilled=null; owner = presence.user.id; _state.value = ReminderRoutineUiState(); load()
                    }
                    is SessionPresence.NotAuthenticated -> {
                        work?.cancel(); prefilled=null; owner = null; _state.value = ReminderRoutineUiState()
                    }
                    else -> Unit
                }
            }
        }
    }
    fun load() {
        val account = owner ?: return
        if (_state.value.saving) return
        work?.cancel()
        _state.update { it.copy(loading = true,error = null,message = null) }
        work = viewModelScope.launch {
            val result = repository.get(account)
            ensureActive()
            if (owner != account) return@launch
            result.fold(onSuccess = { show(it) }, onFailure = { failure ->
                _state.update { it.copy(loading = false,loaded = false,error = failure.toDataMessage("Não foi possível consultar sua rotina. Tente novamente.")) }
            })
        }
    }
    private fun show(routine: ReminderRoutine?, message: String? = null) {
        _state.value = ReminderRoutineUiState(loading = false,loaded = true,routine = routine,
            dateText = routine?.startsOn?.format(ProfileValidator.birthDateFormat).orEmpty(),
            intervalText = routine?.intervalDays?.toString().orEmpty(),timeZoneId = routine?.timeZoneId ?: "America/Sao_Paulo",message = message)
    }
    fun prefill(choice: RoutineChoice) {
        val current = _state.value
        if (!current.loaded || current.saving || prefilled == choice) return
        prefilled=choice
        _state.value=current.copy(dateText=choice.startsOn.format(ProfileValidator.birthDateFormat),intervalText=choice.intervalDays.toString(),
            message=if ((current.routine?.version ?: 0L) != choice.expectedVersion) "A rotina foi alterada desde o rascunho. Confira os dados antes de continuar." else current.message)
    }
    fun updateDate(value: String) { if (!_state.value.saving) _state.update { it.copy(dateText = value,dateError = null,message = null) } }
    fun updateInterval(value: String) { if (!_state.value.saving) _state.update { it.copy(intervalText = value.filter { c -> c in '0'..'9' }.take(4),intervalError = null,message = null) } }

    /** Validação usada tanto pelo editor quanto pela opção de um novo agendamento. */
    fun choice(timeZoneId: String = _state.value.timeZoneId): RoutineChoice? {
        val current = _state.value
        if (!current.loaded || current.loading || current.saving || owner == null) return null
        val date = ProfileValidator.parseBirthDate(current.dateText)
        val interval = current.intervalText.toIntOrNull()
        val today = LocalDate.now(clock.withZone(ZoneId.of(timeZoneId)))
        val dateError = when {
            date == null || date.year !in 1900..9999 -> "Informe uma data válida no formato DD/MM/AAAA."
            date < today && date != current.routine?.startsOn -> "Escolha uma data inicial a partir de hoje."
            else -> null
        }
        val intervalError = if (interval == null || interval !in 1..3650) "Informe um intervalo de 1 a 3650 dias." else null
        _state.update { it.copy(dateError = dateError,intervalError = intervalError) }
        if (dateError != null || intervalError != null) return null
        return RoutineChoice(checkNotNull(date),checkNotNull(interval),timeZoneId,current.routine?.version ?: 0).validated()
    }
    fun save() { val choice = choice() ?: return; mutate("Rotina salva.") { repository.save(it,choice) } }
    fun setActive(active: Boolean) {
        val routine = _state.value.routine ?: return
        mutate(if (active) "Rotina retomada." else "Rotina pausada.") { repository.setActive(it,active,routine.version) }
    }
    private fun mutate(message: String, action: suspend (String) -> Result<ReminderRoutine>) {
        val account = owner ?: return
        if (_state.value.saving || !_state.value.loaded) return
        _state.update { it.copy(saving = true,error = null,message = null) }
        work = viewModelScope.launch {
            val result = action(account)
            ensureActive()
            if (owner != account) return@launch
            result.fold(onSuccess = { show(it,message) },onFailure = { failure ->
                _state.update { it.copy(saving = false,error = failure.toDataMessage("Não foi possível salvar sua rotina. Tente novamente.")) }
            })
        }
    }
    fun delete() {
        val account = owner ?: return
        val routine = _state.value.routine ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true,error = null,message = null) }
        work = viewModelScope.launch {
            val result = repository.delete(account,routine.version)
            ensureActive()
            if (owner != account) return@launch
            result.fold(onSuccess = { show(null,"Rotina excluída.") },onFailure = { failure ->
                _state.update { it.copy(saving = false,error = failure.toDataMessage("Não foi possível excluir sua rotina. Tente novamente.")) }
            })
        }
    }
}
