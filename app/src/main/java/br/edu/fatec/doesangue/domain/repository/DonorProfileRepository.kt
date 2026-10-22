package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.domain.model.BloodType
import java.time.LocalDate

// Define a consulta ao perfil da conta autenticada
interface DonorProfileRepository {
    suspend fun updateCurrentProfile(
        displayName: String,
        bloodType: BloodType,
        phone: String?,
        birthDate: LocalDate?,
    ): Result<DonorProfile>

    suspend fun updateCurrentBloodType(
        bloodType: BloodType,
    ): Result<DonorProfile>

    suspend fun getCurrentProfile(): Result<DonorProfile?>

    suspend fun createCurrentProfile(
        displayName: String,
    ): Result<DonorProfile>
}
