package br.edu.fatec.doesangue.presentation.appointments

import br.edu.fatec.doesangue.presentation.common.toDataMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppointmentsViewModel(
    private val repository: AppointmentRepository,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<AppointmentsUiState>(AppointmentsUiState.Loading)

    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadAppointments()
    }

    fun loadAppointments() {
        if (loadJob?.isActive == true) return

        _uiState.value = AppointmentsUiState.Loading

        loadJob = viewModelScope.launch {
            val result = repository.getMyAppointments()
            ensureActive()

            _uiState.value = result.fold(
                onSuccess = { appointments ->
                    AppointmentsUiState.Loaded(appointments)
                },
                onFailure = {
                    AppointmentsUiState.Error(
                        message = it.toDataMessage("Não foi possível carregar os agendamentos. Tente novamente."),
                    )
                },
            )
        }
    }
}