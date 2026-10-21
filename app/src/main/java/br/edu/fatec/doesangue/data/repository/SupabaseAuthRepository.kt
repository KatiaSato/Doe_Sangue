package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.SupabaseAuthDataSource
import br.edu.fatec.doesangue.data.supabase.mapper.toAuthFailure
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.RegistrationResult
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException

// Implementa o contrato de autenticação usando a fonte Supabase.
class SupabaseAuthRepository(
    private val dataSource: SupabaseAuthDataSource,
) : AuthRepository {

    override suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
        bloodType: BloodType,
    ): Result<RegistrationResult> = try {
        Result.success(dataSource.signUp(displayName, email, password, bloodType))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception.toAuthFailure())
    }

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
            Result.failure(exception.toAuthFailure())
        }
    }

    override suspend fun requestPasswordReset(
        email: String,
    ): Result<Unit> {
        return try {
            dataSource.requestPasswordReset(email)
            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception.toAuthFailure())
        }
    }

    override suspend fun exchangeRecoveryCode(
        code: String,
    ): Result<AuthenticatedUser> {
        return try {
            val user = dataSource.exchangeRecoveryCode(code)
            Result.success(user)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception.toAuthFailure())
        }
    }

    override suspend fun updatePassword(
        password: String,
    ): Result<Unit> {
        return try {
            dataSource.updatePassword(password)
            Result.success(Unit)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception.toAuthFailure())
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
