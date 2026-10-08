package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository

// Fornece dados previsíveis aos testes, sem acessar a rede.
class FakeDonationCenterRepository : DonationCenterRepository {

    override suspend fun getCenters(): Result<List<DonationCenter>> {
        val centers = listOf(
            DonationCenter(
                id = "test-center-1",
                name = "Unidade Acadêmica de Demonstração",
                city = "Cidade fictícia",
                address = "Endereço fictício",
                openingHours = "Horário fictício",
                phone = "Contato fictício",
                timeZoneId = "America/Sao_Paulo",
            )
        )

        return Result.success(centers)
    }
}
