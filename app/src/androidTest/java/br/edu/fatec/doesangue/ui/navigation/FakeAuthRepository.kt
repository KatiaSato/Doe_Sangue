package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.RegistrationResult

import br.edu.fatec.doesangue.domain.model.BloodType

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.repository.AuthRepository

// Fornece um resultado previsível, sem acessar o Supabase.
class FakeAuthRepository(
    private val result: Result<AuthenticatedUser> =
        Result.success(AuthenticatedUser(id = "test-user-1")),
) : AuthRepository {
        override suspend fun signUp(displayName: String, email: String, password: String, bloodType: BloodType): Result<RegistrationResult> =
            Result.success(RegistrationResult(null))


    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthenticatedUser> {
        return result
    }

    override suspend fun signOut(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun restoreSession(): Result<AuthenticatedUser?> {
        return Result.success(null)
    }
}
