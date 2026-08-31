package br.edu.fatec.doesangue.data.source

import br.edu.fatec.doesangue.domain.model.DonationOverview

interface DonationOverviewDataSource {
    suspend fun getOverview(): DonationOverview
}

