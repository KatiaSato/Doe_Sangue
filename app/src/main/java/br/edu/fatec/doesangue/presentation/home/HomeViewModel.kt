package br.edu.fatec.doesangue.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import br.edu.fatec.doesangue.domain.repository.BloodStockRepository
import br.edu.fatec.doesangue.domain.repository.CampaignRepository
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import br.edu.fatec.doesangue.presentation.common.toReadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

class HomeViewModel(
    private val profileRepository: DonorProfileRepository,
    private val appointmentRepository: AppointmentRepository,
    private val campaignRepository: CampaignRepository,
    private val stockRepository: BloodStockRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        if (loadJob?.isActive == true) return
        _uiState.value = HomeUiState()
        // Cada seção mantém o próprio resultado, mesmo se outra consulta falhar.
        loadJob = viewModelScope.launch {
            coroutineScope {
                launch {
                    val result = profileRepository.getCurrentProfile()
                    ensureActive()
                    _uiState.update { it.copy(profile = result.toReadState("Não foi possível carregar seu nome.")) }
                }
                launch {
                    val result = appointmentRepository.getMyAppointments().map { items ->
                        items.filter { it.appointment.status == AppointmentStatus.SCHEDULED && it.startsAt > clock.instant() }
                            .minWithOrNull(compareBy({ it.startsAt }, { it.appointment.id }))
                    }
                    ensureActive()
                    _uiState.update { it.copy(appointment = result.toReadState("Não foi possível consultar o próximo agendamento.")) }
                }
                launch {
                    val result = campaignRepository.getCampaigns(null)
                    ensureActive()
                    _uiState.update { it.copy(campaigns = result.toReadState("Não foi possível carregar as campanhas.")) }
                }
                launch {
                    val result = stockRepository.getBloodStocks()
                    ensureActive()
                    _uiState.update { it.copy(stocks = result.toReadState("Não foi possível carregar o estoque demonstrativo.")) }
                }
            }
        }
    }
}

