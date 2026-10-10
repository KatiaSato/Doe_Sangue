package br.edu.fatec.doesangue.di

import br.edu.fatec.doesangue.data.demo.DemoDonationOverviewDataSource
import br.edu.fatec.doesangue.data.repository.DefaultDonationOverviewRepository
import br.edu.fatec.doesangue.domain.repository.DonationOverviewRepository
import br.edu.fatec.doesangue.domain.usecase.GetDonationOverviewUseCase
import br.edu.fatec.doesangue.data.repository.SupabaseDonationTimeSlotRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseDonationTimeSlotDataSource
import br.edu.fatec.doesangue.data.repository.SupabaseAuthRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseAuthDataSource
import br.edu.fatec.doesangue.domain.repository.AuthRepository
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
import br.edu.fatec.doesangue.presentation.auth.LoginViewModel
import br.edu.fatec.doesangue.data.repository.SupabaseDonorProfileRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseDonorProfileDataSource
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import br.edu.fatec.doesangue.presentation.profile.ProfileViewModel

// Ele é responsável por criar os objetos e fornecer suas dependências
class AppContainer(
    private val centerRepositoryOverride: DonationCenterRepository? = null,
    private val timeSlotRepositoryOverride: DonationTimeSlotRepository? = null,
    private val authRepositoryOverride: AuthRepository? = null,
    private val donorProfileRepositoryOverride: DonorProfileRepository? = null,
) {
    // Cria o cliente com as configurações geradas pelo Gradle.
    private val supabaseClient by lazy {
        createDonationSupabaseClient(
            url = BuildConfig.SUPABASE_URL,
            publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        )
    }

    // Reutiliza o cliente Supabase para autenticar o usuário.
    private val supabaseAuthDataSource by lazy {
        SupabaseAuthDataSource(
            client = supabaseClient,
        )
    }

    // Permite substituir a autenticação nos testes.
    val authRepository: AuthRepository by lazy {
        authRepositoryOverride ?: SupabaseAuthRepository(
            dataSource = supabaseAuthDataSource,
        )
    }

    // Usa o mesmo cliente que mantém a sessão autenticada.
    private val supabaseDonorProfileDataSource by lazy {
        SupabaseDonorProfileDataSource(
            client = supabaseClient,
        )
    }

    // Permite substituir a consulta ao perfil nos testes.
    val donorProfileRepository: DonorProfileRepository by lazy {
        donorProfileRepositoryOverride ?: SupabaseDonorProfileRepository(
            dataSource = supabaseDonorProfileDataSource,
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

    // Cria o ViewModel do perfil com o repository configurado.
    val profileViewModelFactory = viewModelFactory {
        initializer {
            ProfileViewModel(
                repository = donorProfileRepository,
            )
        }
    }

    // Cria o ViewModel de login com o repository de autenticação.
    val loginViewModelFactory = viewModelFactory {
        initializer {
            LoginViewModel(
                authRepository = authRepository,
            )
        }
    }
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
