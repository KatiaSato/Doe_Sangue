package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository

// Fornece um perfil fictício sem acessar o Supabase.
class FakeDonorProfileRepository(
    private var result: Result<DonorProfile?> = Result.success(
        DonorProfile(
            id = "test-user-1",
            displayName = "Doador de teste",
        ),
    ),
) : DonorProfileRepository {

    override suspend fun getCurrentProfile(): Result<DonorProfile?> {
        return result
    }

    override suspend fun createCurrentProfile(
        displayName: String,
    ): Result<DonorProfile> {
        val normalizedName = displayName.trim()

        if (normalizedName.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Informe o nome de exibição."),
            )
        }

        if (result.getOrNull() != null) {
            return Result.failure(
                IllegalStateException("O perfil já existe."),
            )
        }

        val profile = DonorProfile(
            id = "test-user-1",
            displayName = normalizedName,
        )

        result = Result.success(profile)
        return Result.success(profile)
    }
}