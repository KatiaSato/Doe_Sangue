package br.edu.fatec.doesangue.domain.usecase

import br.edu.fatec.doesangue.domain.model.DonationOverview
import br.edu.fatec.doesangue.domain.repository.DonationOverviewRepository

class GetDonationOverviewUseCase(
    private val repository: DonationOverviewRepository,
) {
    suspend operator fun invoke(): Result<DonationOverview> = repository.getOverview()
}

