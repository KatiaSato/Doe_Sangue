package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.repository.AuthRepository

// Fornece um resultado previsível, sem acessar o Supabase.
class FakeAuthRepository(
    private val result: Result<AuthenticatedUser> =
        Result.success(AuthenticatedUser(id = "test-user-1")),
) : AuthRepository {

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthenticatedUser> {
        return result
    }
}