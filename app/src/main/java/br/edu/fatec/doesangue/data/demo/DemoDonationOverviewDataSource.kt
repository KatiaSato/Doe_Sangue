package br.edu.fatec.doesangue.data.demo

import br.edu.fatec.doesangue.data.source.DonationOverviewDataSource
import br.edu.fatec.doesangue.domain.model.BloodNeed
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.model.DonationOverview
import br.edu.fatec.doesangue.domain.model.NeedLevel

class DemoDonationOverviewDataSource : DonationOverviewDataSource {
    override suspend fun getOverview(): DonationOverview {
        val center = DonationCenter(
            id = "demo-center-1",
            name = "Unidade Acadêmica de Demonstração",
            city = "Americana",
            address = "Endereço fictício para demonstração",
            openingHours = "Horário fictício",
            phone = "Contato fictício",
            timeZoneId = "America/Sao_Paulo",
        )

        return DonationOverview(
            centers = listOf(center),
            needs = listOf(
                BloodNeed(
                    donationCenterId = center.id,
                    bloodType = BloodType.O_NEGATIVE,
                    level = NeedLevel.HIGH,
                    measurementUnit = "nível demonstrativo",
                    source = "seed acadêmico sintético",
                    lastUpdatedLabel = "dado de demonstração",
                    isSynthetic = true,
                ),
            ),
        )
    }
}

