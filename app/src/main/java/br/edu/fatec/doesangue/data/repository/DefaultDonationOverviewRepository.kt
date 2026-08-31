package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.source.DonationOverviewDataSource
import br.edu.fatec.doesangue.domain.model.DonationOverview
import br.edu.fatec.doesangue.domain.repository.DonationOverviewRepository

class DefaultDonationOverviewRepository(
    private val dataSource: DonationOverviewDataSource,
) : DonationOverviewRepository {
    override suspend fun getOverview(): Result<DonationOverview> =
        runCatching { dataSource.getOverview() }
}

