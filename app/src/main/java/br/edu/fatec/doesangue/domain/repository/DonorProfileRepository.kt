package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.DonorProfile

// Define a consulta ao perfil da conta autenticada
interface DonorProfileRepository {
    suspend fun getCurrentProfile(): Result<DonorProfile?>

    suspend fun createCurrentProfile(
        displayName: String,
    ): Result<DonorProfile>
}