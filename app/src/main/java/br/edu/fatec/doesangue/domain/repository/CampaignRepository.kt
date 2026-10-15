package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.Campaign

interface CampaignRepository {
    // null: todos; uma campanha sem tipos associados é geral.
    suspend fun getCampaigns(bloodType: BloodType? = null): Result<List<Campaign>>
}
