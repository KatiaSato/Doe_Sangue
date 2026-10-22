package br.edu.fatec.doesangue.presentation.profile

import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.domain.validation.ProfileValidator

sealed interface EditProfileUiState {
    data object Loading : EditProfileUiState
    data object Missing : EditProfileUiState
    data class Error(val message: String) : EditProfileUiState
    data class Ready(
        val profile: DonorProfile,
        val displayName: String = profile.displayName,
        val bloodType: BloodType = profile.bloodType,
        val phone: String = profile.phone.orEmpty(),
        val birthDate: String = profile.birthDate?.format(ProfileValidator.birthDateFormat).orEmpty(),
        val isSaving: Boolean = false,
        val saved: Boolean = false,
        val nameError: String? = null,
        val phoneError: String? = null,
        val birthDateError: String? = null,
        val errorMessage: String? = null,
    ) : EditProfileUiState {
        val hasChanges: Boolean get() = displayName.trim() != profile.displayName ||
            bloodType != profile.bloodType ||
            phone.filter { it in '0'..'9' } != profile.phone.orEmpty() ||
            !ProfileValidator.isValidPhone(phone) ||
            birthDate.trim() != profile.birthDate?.format(ProfileValidator.birthDateFormat).orEmpty()
    }
}
