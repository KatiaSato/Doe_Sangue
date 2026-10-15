package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.DonationRecordDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.DonationRecord
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SupabaseDonationHistoryDataSource(private val client: SupabaseClient) {
    suspend fun getMyDonations(): List<DonationRecord> =
        client.postgrest.rpc("consultar_minhas_doacoes").decodeList<DonationRecordDto>().map { it.toDomain() }
}
