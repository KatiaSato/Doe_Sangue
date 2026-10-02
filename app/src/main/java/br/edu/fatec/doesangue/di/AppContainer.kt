package br.edu.fatec.doesangue.di

import br.edu.fatec.doesangue.data.demo.DemoDonationOverviewDataSource
import br.edu.fatec.doesangue.data.repository.DefaultDonationOverviewRepository
import br.edu.fatec.doesangue.domain.repository.DonationOverviewRepository
import br.edu.fatec.doesangue.domain.usecase.GetDonationOverviewUseCase
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel

class AppContainer {
    private val demoDataSource = DemoDonationOverviewDataSource()

    val donationOverviewRepository: DonationOverviewRepository =
        DefaultDonationOverviewRepository(demoDataSource)

    val getDonationOverview = GetDonationOverviewUseCase(donationOverviewRepository)

    /*
 * Define como criar o ViewModel de agendamento,
 * fornecendo o caso de uso montado neste container.
 *
 * A navegação usará esta fábrica para obter um ViewModel
 * gerenciado pelo Android.
 */
    val schedulingViewModelFactory = viewModelFactory {
        initializer {
            SchedulingViewModel(
                getDonationOverview = getDonationOverview,
            )
        }
    }
}

