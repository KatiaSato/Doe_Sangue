package br.edu.fatec.doesangue.presentation.auth

import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.RegistrationResult

// A senha não faz parte do estado persistido/compartilhado do cadastro.
data class RegistrationUiState(
    val displayName: String = "",
    val email: String = "",
    val bloodType: BloodType? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val result: RegistrationResult? = null,
)
