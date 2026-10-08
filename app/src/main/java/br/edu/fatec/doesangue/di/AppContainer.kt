package br.edu.fatec.doesangue.di

import br.edu.fatec.doesangue.data.demo.DemoDonationOverviewDataSource
import br.edu.fatec.doesangue.data.repository.DefaultDonationOverviewRepository
import br.edu.fatec.doesangue.domain.repository.DonationOverviewRepository
import br.edu.fatec.doesangue.domain.usecase.GetDonationOverviewUseCase
import br.edu.fatec.doesangue.data.repository.SupabaseDonationTimeSlotRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseDonationTimeSlotDataSource
import br.edu.fatec.doesangue.domain.repository.DonationTimeSlotRepository
import br.edu.fatec.doesangue.domain.usecase.GetDonationTimeSlotsUseCase
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel
import br.edu.fatec.doesangue.BuildConfig
import br.edu.fatec.doesangue.data.repository.SupabaseDonationCenterRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseDonationCenterDataSource
import br.edu.fatec.doesangue.data.supabase.createDonationSupabaseClient
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository

// Ele é responsável por criar os objetos e fornecer suas dependências
class AppContainer(
    private val centerRepositoryOverride: DonationCenterRepository? = null,
    private val timeSlotRepositoryOverride: DonationTimeSlotRepository? = null,
) {
    // Cria o cliente com as configurações geradas pelo Gradle.
    private val supabaseClient by lazy {
        createDonationSupabaseClient(
            url = BuildConfig.SUPABASE_URL,
            publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        )
    }

    // Fornece o cliente à fonte que consulta as unidades.
    private val supabaseCenterDataSource by lazy {
        SupabaseDonationCenterDataSource(
            client = supabaseClient,
        )
    }

    // Usa o repository fornecido ou cria a implementação Supabase.
    val donationCenterRepository: DonationCenterRepository by lazy {
        centerRepositoryOverride ?: SupabaseDonationCenterRepository(
            dataSource = supabaseCenterDataSource,
        )
    }

    // Reutiliza o cliente Supabase para consultar os horários.
    private val supabaseTimeSlotDataSource by lazy {
        SupabaseDonationTimeSlotDataSource(
            client = supabaseClient,
        )
    }

    // Permite substituir a implementação nos testes.
    val donationTimeSlotRepository: DonationTimeSlotRepository by lazy {
        timeSlotRepositoryOverride ?: SupabaseDonationTimeSlotRepository(
            dataSource = supabaseTimeSlotDataSource,
        )
    }

    // Fornece ao caso de uso o repository que fará a consulta.
    val getDonationTimeSlots by lazy {
        GetDonationTimeSlotsUseCase(
            repository = donationTimeSlotRepository,
        )
    }
    private val demoDataSource = DemoDonationOverviewDataSource()

    val donationOverviewRepository: DonationOverviewRepository =
        DefaultDonationOverviewRepository(demoDataSource)

    val getDonationOverview = GetDonationOverviewUseCase(donationOverviewRepository)

    /*
 * Define como criar o ViewModel de agendamento,
 * fornecendo o repository de unidades montado neste container.
 *
 * A navegação usará esta fábrica para obter um ViewModel
 * gerenciado pelo Android.
 */
    val schedulingViewModelFactory = viewModelFactory {
        initializer {
            SchedulingViewModel(
                donationCenterRepository = donationCenterRepository,
                getDonationTimeSlots = getDonationTimeSlots,
            )
        }
    }
}
