package br.edu.fatec.doesangue.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppointmentDetailViewModel(
    private val repository: AppointmentRepository,
    private val appointmentId: String,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<AppointmentDetailUiState>(
            AppointmentDetailUiState.Loading,
        )

    val uiState: StateFlow<AppointmentDetailUiState> =
        _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadAppointment()
    }

    fun loadAppointment() {
        if (loadJob?.isActive == true) return

        _uiState.value = AppointmentDetailUiState.Loading

        loadJob = viewModelScope.launch {
            val result = repository.getMyAppointments()
            ensureActive()

            _uiState.value = result.fold(
                onSuccess = { appointments ->
                    val summary = appointments.firstOrNull {     // firstOrNull procura a reserva pelo ID e retorna null quando não encontra
                        it.appointment.id == appointmentId
                    }

                    if (summary == null) {
                        AppointmentDetailUiState.NotFound
                    } else {
                        AppointmentDetailUiState.Loaded(summary)
                    }
                },
                onFailure = {
                    AppointmentDetailUiState.Error(
                        message = "Não foi possível carregar o agendamento. Tente novamente.",
                    )
                },
            )
        }
    }
}