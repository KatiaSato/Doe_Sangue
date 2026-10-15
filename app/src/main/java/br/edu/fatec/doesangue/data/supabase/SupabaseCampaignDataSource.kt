package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.CampaignDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.Campaign
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseCampaignDataSource(private val client: SupabaseClient) {
    suspend fun getCampaigns(bloodType: BloodType?): List<Campaign> {
        require(bloodType != BloodType.UNKNOWN) { "Selecione Todos quando não souber o tipo." }
        val parameters = buildJsonObject {
            if (bloodType != null) put("p_tipo_sanguineo", bloodType.databaseValue)
        }
        return client.postgrest.rpc("consultar_campanhas", parameters)
            .decodeList<CampaignDto>().map { it.toDomain() }
    }
}
