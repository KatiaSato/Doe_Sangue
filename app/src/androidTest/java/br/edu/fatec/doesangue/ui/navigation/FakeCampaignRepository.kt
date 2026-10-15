package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.Campaign
import br.edu.fatec.doesangue.domain.repository.CampaignRepository
import java.time.Instant

// Não consulta a rede nos testes de navegação existentes.
class FakeCampaignRepository : CampaignRepository {
    override suspend fun getCampaigns(bloodType: BloodType?): Result<List<Campaign>> = Result.success(
        listOf(Campaign("test-campaign-1", "test-center-1", "Doe sangue, doe vida!",
            "Campanha sintética para navegação.", Instant.parse("2026-10-15T03:00:00Z"),
            Instant.parse("2026-12-01T03:00:00Z"), "Teste sintético", "Unidade Sintética A",
            "America/Sao_Paulo", emptyList())),
    )
}
