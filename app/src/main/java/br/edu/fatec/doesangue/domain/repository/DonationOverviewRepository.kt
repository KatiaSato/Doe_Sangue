package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.DonationOverview

interface DonationOverviewRepository {
    suspend fun getOverview(): Result<DonationOverview>
}

