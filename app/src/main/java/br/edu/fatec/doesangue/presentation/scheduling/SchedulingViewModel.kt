package br.edu.fatec.doesangue.presentation.scheduling

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.usecase.GetDonationOverviewUseCase

/*
 * Recebe pelo construtor o caso de uso responsável por consultar
 * o resumo de doação, que inclui a lista de unidades.
 *
 * Isso é injeção de dependência: quem cria o ViewModel fornece
 * o caso de uso de que ele precisa.
 *
 * O ViewModel solicita os dados sem conhecer sua origem
 * (fonte de demonstração ou futuro Supabase).
 * O caso de uso consulta o repository.
 *
 * private mantém essa dependência acessível apenas nesta classe.
 * val impede que a referência seja substituída após a criação.
 */
class SchedulingViewModel(
    private val getDonationOverview: GetDonationOverviewUseCase,
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
    fun selectCenter(center: DonationCenter) {
        _uiState.update { currentState ->
            currentState.copy(selectedCenter = center)
        }
    }

    /*
 * Consulta as unidades pelo caso de uso.
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
            getDonationOverview().fold(
                onSuccess = { overview ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            centers = overview.centers,
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