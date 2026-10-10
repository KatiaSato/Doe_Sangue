package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.SupabaseDonorProfileDataSource
import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import kotlinx.coroutines.CancellationException

// Implementa a consulta ao perfil usando a fonte Supabase.
/*
  Seguimos o mesmo padrão do repository de autenticação:
- O DataSource realiza a consulta.
- O repository entrega sucesso ou falha pelo contrato definido em domain.
- Se o perfil não existir, Result.success(null) mantém essa ausência como um resultado válido.
- O cancelamento é repassado para que a corrotina possa ser interrompida normalmente.*/
class SupabaseDonorProfileRepository(
    private val dataSource: SupabaseDonorProfileDataSource,
) : DonorProfileRepository {

    override suspend fun getCurrentProfile(): Result<DonorProfile?> {
        return try {
            val profile = dataSource.getCurrentProfile()
            Result.success(profile)
        } catch (exception: CancellationException) {
            // Preserva o cancelamento da corrotina.
            throw exception
        } catch (exception: Exception) {
            // Devolve a falha para quem solicitou o perfil.
            Result.failure(exception)
        }
    }

    override suspend fun createCurrentProfile(
        displayName: String,
    ): Result<DonorProfile> {
        return try {
            val profile = dataSource.createCurrentProfile(displayName)
            Result.success(profile)
        } catch (exception: CancellationException) {
            // Preserva o cancelamento da corrotina.
            throw exception
        } catch (exception: Exception) {
            // Devolve a falha para quem solicitou a criação.
            Result.failure(exception)
        }
    }
}