package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.BloodStockDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.BloodStock
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SupabaseBloodStockDataSource(private val client: SupabaseClient) {
    suspend fun getBloodStocks(): List<BloodStock> =
        client.postgrest.rpc("consultar_estoques").decodeList<BloodStockDto>().map { it.toDomain() }
}
