package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.BloodStock
import br.edu.fatec.doesangue.domain.model.DonationRecord
import br.edu.fatec.doesangue.domain.repository.BloodStockRepository
import br.edu.fatec.doesangue.domain.repository.DonationHistoryRepository
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.edu.fatec.doesangue.presentation.home.HomeViewModel

// Os testes antigos de outras telas mantêm seus contadores e respostas isolados.
// HomeReadFlowTest exercita a integração completa da Home com os repositories do cenário.
internal fun isolatedHomeFactory() = viewModelFactory {
    initializer {
        HomeViewModel(FakeDonorProfileRepository(), FakeAppointmentRepository(),
            FakeCampaignRepository(), FakeBloodStockRepository())
    }
}

class FakeBloodStockRepository : BloodStockRepository {
    override suspend fun getBloodStocks(): Result<List<BloodStock>> = Result.success(emptyList())
}

class FakeDonationHistoryRepository : DonationHistoryRepository {
    override suspend fun getMyDonations(): Result<List<DonationRecord>> = Result.success(emptyList())
}
