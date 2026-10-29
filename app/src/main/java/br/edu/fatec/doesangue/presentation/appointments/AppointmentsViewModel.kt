package br.edu.fatec.doesangue.presentation.appointments

import br.edu.fatec.doesangue.presentation.common.toDataMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppointmentsViewModel(
    private val repository: AppointmentRepository,
    private val centersRepository: DonationCenterRepository? = null,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<AppointmentsUiState>(AppointmentsUiState.Loading)

    val uiState: StateFlow<AppointmentsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var centersJob: Job? = null
    private var generation = 0

    init {
        loadAppointments()
    }

    fun loadAppointments() {
        if (loadJob?.isActive == true) return

        generation++
        centersJob?.cancel()

        _uiState.value = AppointmentsUiState.Loading

        loadJob = viewModelScope.launch {
            val result = repository.getMyAppointments()
            ensureActive()

            _uiState.value = result.fold(
                onSuccess = { appointments ->
                    AppointmentsUiState.Loaded(appointments, centersLoading = centersRepository != null)
                },
                onFailure = {
                    AppointmentsUiState.Error(
                        message = it.toDataMessage("Não foi possível carregar os agendamentos. Tente novamente."),
                    )
                },
            )
            if (result.isSuccess) loadCities()
        }
    }

    fun loadCities() {
        val source = centersRepository ?: return
        if (_uiState.value !is AppointmentsUiState.Loaded || centersJob?.isActive == true) return
        val expectedGeneration = generation
        val loaded = _uiState.value as AppointmentsUiState.Loaded
        _uiState.value = loaded.copy(centersLoading = true, centersError = null)
        centersJob = viewModelScope.launch {
            val result = source.getCenters()
            ensureActive()
            if (generation != expectedGeneration) return@launch
            val current = _uiState.value as? AppointmentsUiState.Loaded ?: return@launch
            _uiState.value = result.fold(
                onSuccess = { current.copy(centers = it, centersLoading = false, centersError = null) },
                onFailure = { current.copy(centersLoading = false,
                    centersError = "Busca por cidade indisponível. Seus agendamentos e a busca por nome continuam disponíveis.") },
            )
        }
    }
}
