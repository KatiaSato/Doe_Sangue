package br.edu.fatec.doesangue.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Consulta o perfil e disponibiliza o estado para a tela.
class ProfileViewModel(
    private val repository: DonorProfileRepository,
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)

    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadProfile()
    }

    fun loadProfile() {
        if ((_uiState.value as? ProfileUiState.Missing)?.isSaving == true) return
        // Evita consultas simultâneas.
        if (loadJob?.isActive == true) return

        _uiState.value = ProfileUiState.Loading

        loadJob = viewModelScope.launch {
            val result = repository.getCurrentProfile()

            _uiState.value = result.fold(
                onSuccess = { profile ->
                    if (profile == null) {
                        ProfileUiState.Missing()
                    } else {
                        ProfileUiState.Loaded(profile)
                    }
                },
                onFailure = {
                    ProfileUiState.Error(
                        message = "Não foi possível carregar o perfil. Tente novamente.",
                    )
                },
            )
        }
    }

    // Atualiza o nome digitado enquanto o perfil ainda não existe.
    // Essa função atualiza somente o estado em memória
    /*
      as? tenta obter o estado como Missing. Se for outro estado, ?: return encerra a função.
      isSaving impede mudanças durante a gravação.
      copy cria um novo estado com o nome atualizado, preservando os demais campos.
      Ao editar, limpamos a mensagem de erro anterior.
     */
    fun updateDisplayName(displayName: String) {
        val currentState =
            _uiState.value as? ProfileUiState.Missing ?: return

        if (currentState.isSaving) return

        _uiState.value = currentState.copy(
            displayName = displayName,
            errorMessage = null,
        )
    }

    fun createProfile() {
        val currentState =
            _uiState.value as? ProfileUiState.Missing ?: return

        if (currentState.isSaving) return

        val name = currentState.displayName.trim()

        if (name.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Informe o nome de exibição.",
            )
            return
        }

        _uiState.value = currentState.copy(
            isSaving = true,
            errorMessage = null,
        )

        viewModelScope.launch {
            try {
                val result = repository.createCurrentProfile(name)

                _uiState.value = result.fold(
                    onSuccess = { profile ->
                        ProfileUiState.Loaded(profile)
                    },
                    onFailure = {
                        currentState.copy(
                            isSaving = false,
                            errorMessage = "Não foi possível concluir a criação do perfil.",
                        )
                    },
                )
            } finally {
                val state = _uiState.value
                if (state is ProfileUiState.Missing && state.isSaving) {
                    _uiState.value = state.copy(isSaving = false)
                }
            }
        }
    }
}
