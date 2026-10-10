package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.SupabaseAuthDataSource
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

// Implementa o contrato de autenticação usando a fonte Supabase.
class SupabaseAuthRepository(
    private val dataSource: SupabaseAuthDataSource,
) : AuthRepository {

    override suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthenticatedUser> {
        return try {
            val user = dataSource.signIn(email, password)
            Result.success(user)
        } catch (exception: CancellationException) {
            // Preserva o cancelamento da corrotina.
            throw exception
        } catch (exception: Exception) {
            // Devolve a falha para quem solicitou o login.
            Result.failure(exception)
        }
    }
}