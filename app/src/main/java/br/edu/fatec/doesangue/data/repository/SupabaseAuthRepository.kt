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

    override suspend fun signOut(): Result<Unit> {
        return try {
            dataSource.signOut()
            Result.success(Unit)
        } catch (exception: CancellationException) {
            // Preserva o cancelamento da corrotina.
            throw exception
        } catch (exception: Exception) {
            // Devolve a falha para quem solicitou o logout.
            Result.failure(exception)
        }
    }

    override suspend fun restoreSession(): Result<AuthenticatedUser?> {
        return try {
            val user = dataSource.restoreSession()
            Result.success(user)
        } catch (exception: CancellationException) {
            // Preserva o cancelamento da corrotina.
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }


}