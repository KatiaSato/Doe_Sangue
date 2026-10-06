package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.DonationCenterDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.DonationCenter
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

// Busca unidades na API e converte a resposta para o domínio.
class SupabaseDonationCenterDataSource(
    private val client: SupabaseClient,
) {
    suspend fun getCenters(): List<DonationCenter> {
        val centersDto = client
            .from("unidade_coleta")
            // select() faz a consulta pela API
            .select()
            // decodeList<DonationCenterDto>() transforma o JSON recebido em uma lista de DTOs
            .decodeList<DonationCenterDto>()

        return centersDto.map { dto ->
            dto.toDomain()
        }
    }
}
