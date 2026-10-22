package br.edu.fatec.doesangue.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import br.edu.fatec.doesangue.domain.validation.ProfileValidator
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val repository: DonorProfileRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { load() }

    fun load() {
        val ready = _uiState.value as? EditProfileUiState.Ready
        if (loadJob?.isActive == true || ready?.isSaving == true || ready?.saved == true) return
        _uiState.value = EditProfileUiState.Loading
        loadJob = viewModelScope.launch {
            val result = repository.getCurrentProfile()
            ensureActive()
            _uiState.value = result.fold(
                onSuccess = { it?.let(EditProfileUiState::Ready) ?: EditProfileUiState.Missing },
                onFailure = { EditProfileUiState.Error(it.toProfileMessage("Não foi possível carregar seu perfil. Tente novamente.")) },
            )
        }
    }

    private fun edit(change: (EditProfileUiState.Ready) -> EditProfileUiState.Ready) {
        val current = _uiState.value as? EditProfileUiState.Ready ?: return
        if (!current.isSaving && !current.saved) _uiState.value = change(current)
    }

    fun updateName(value: String) = edit { it.copy(displayName = value, nameError = null, errorMessage = null) }
    fun updateBloodType(value: BloodType) = edit { it.copy(bloodType = value, errorMessage = null) }
    fun updatePhone(value: String) = edit { it.copy(phone = value, phoneError = null, errorMessage = null) }
    fun updateBirthDate(value: String) = edit { it.copy(birthDate = value, birthDateError = null, errorMessage = null) }

    fun save() {
        val current = _uiState.value as? EditProfileUiState.Ready ?: return
        if (current.isSaving || current.saved || !current.hasChanges) return
        val name = current.displayName.trim()
        val birthDate = ProfileValidator.parseBirthDate(current.birthDate)
        val nameError = if (name.isBlank()) "Informe o nome de exibição." else null
        val phoneError = if (!ProfileValidator.isValidPhone(current.phone))
            "Informe um telefone com DDD e 10 ou 11 dígitos, ou deixe vazio." else null
        val birthDateError = when {
            current.birthDate.isBlank() -> null
            birthDate == null -> "Informe uma data válida no formato DD/MM/AAAA."
            birthDate.isAfter(LocalDate.now()) -> "A data de nascimento não pode estar no futuro."
            else -> null
        }
        if (nameError != null || phoneError != null || birthDateError != null) {
            _uiState.value = current.copy(nameError = nameError, phoneError = phoneError,
                birthDateError = birthDateError, errorMessage = null)
            return
        }
        val phone = ProfileValidator.normalizePhone(current.phone)
        _uiState.value = current.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val result = repository.updateCurrentProfile(name, current.bloodType, phone, birthDate)
            ensureActive()
            _uiState.value = result.fold(
                onSuccess = { profile ->
                    if (profile.id == current.profile.id && profile.displayName == name &&
                        profile.bloodType == current.bloodType && profile.phone == phone && profile.birthDate == birthDate) {
                        EditProfileUiState.Ready(profile, saved = true)
                    } else current.copy(errorMessage = "Não foi possível confirmar a atualização. Tente novamente.")
                },
                onFailure = { current.copy(errorMessage = it.toProfileMessage("Não foi possível salvar seu perfil. Tente novamente.")) },
            )
        }
    }
}
