package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.mapper.toDataFailure

import br.edu.fatec.doesangue.data.supabase.SupabaseCampaignDataSource
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.Campaign
import br.edu.fatec.doesangue.domain.repository.CampaignRepository
import kotlinx.coroutines.CancellationException

class SupabaseCampaignRepository(private val dataSource: SupabaseCampaignDataSource) : CampaignRepository {
    override suspend fun getCampaigns(bloodType: BloodType?): Result<List<Campaign>> = try {
        Result.success(dataSource.getCampaigns(bloodType))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception.toDataFailure())
    }
}
