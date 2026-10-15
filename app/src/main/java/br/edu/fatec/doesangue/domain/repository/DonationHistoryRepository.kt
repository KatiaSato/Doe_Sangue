package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.DonationRecord

interface DonationHistoryRepository {
    suspend fun getMyDonations(): Result<List<DonationRecord>>
}
