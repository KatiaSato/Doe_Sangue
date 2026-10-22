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
import br.edu.fatec.doesangue.data.repository.SupabaseAppointmentRepository
import br.edu.fatec.doesangue.data.repository.SupabaseDonationCenterRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseDonationCenterDataSource
import br.edu.fatec.doesangue.data.supabase.createDonationSupabaseClient
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import br.edu.fatec.doesangue.presentation.auth.LoginViewModel
import br.edu.fatec.doesangue.data.repository.SupabaseDonorProfileRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseAppointmentDataSource
import br.edu.fatec.doesangue.data.supabase.SupabaseDonorProfileDataSource
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import br.edu.fatec.doesangue.presentation.auth.LogoutViewModel
import br.edu.fatec.doesangue.presentation.auth.SessionViewModel
import br.edu.fatec.doesangue.presentation.profile.ProfileViewModel
import br.edu.fatec.doesangue.presentation.profile.EditProfileViewModel
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import br.edu.fatec.doesangue.presentation.appointments.AppointmentDetailViewModel
import br.edu.fatec.doesangue.presentation.appointments.AppointmentsViewModel
import br.edu.fatec.doesangue.domain.repository.CampaignRepository
import br.edu.fatec.doesangue.data.repository.SupabaseCampaignRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseCampaignDataSource
import br.edu.fatec.doesangue.presentation.auth.RegistrationViewModel
import br.edu.fatec.doesangue.presentation.campaigns.CampaignsViewModel
import br.edu.fatec.doesangue.presentation.campaigns.CampaignDetailViewModel
import br.edu.fatec.doesangue.domain.repository.BloodStockRepository
import br.edu.fatec.doesangue.domain.repository.DonationHistoryRepository
import br.edu.fatec.doesangue.data.repository.SupabaseBloodStockRepository
import br.edu.fatec.doesangue.data.repository.SupabaseDonationHistoryRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseBloodStockDataSource
import br.edu.fatec.doesangue.data.supabase.SupabaseDonationHistoryDataSource
import br.edu.fatec.doesangue.presentation.auth.ChangePasswordViewModel
import br.edu.fatec.doesangue.presentation.auth.PasswordRecoveryViewModel
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkViewModel
import br.edu.fatec.doesangue.presentation.auth.ResetPasswordViewModel
import br.edu.fatec.doesangue.presentation.centers.CenterDetailViewModel
import br.edu.fatec.doesangue.presentation.centers.CentersViewModel
import br.edu.fatec.doesangue.presentation.home.HomeViewModel
import br.edu.fatec.doesangue.presentation.stock.BloodStockViewModel
import br.edu.fatec.doesangue.presentation.donations.DonationHistoryViewModel

// Ele é responsável por criar os objetos e fornecer suas dependências
class AppContainer(
    private val centerRepositoryOverride: DonationCenterRepository? = null,
    private val timeSlotRepositoryOverride: DonationTimeSlotRepository? = null,
    private val authRepositoryOverride: AuthRepository? = null,
    private val donorProfileRepositoryOverride: DonorProfileRepository? = null,
    private val appointmentRepositoryOverride: AppointmentRepository? = null,
    private val campaignRepositoryOverride: CampaignRepository? = null,
    private val bloodStockRepositoryOverride: BloodStockRepository? = null,
    private val donationHistoryRepositoryOverride: DonationHistoryRepository? = null,
) {
    val bloodStockRepository: BloodStockRepository by lazy {
        bloodStockRepositoryOverride ?: SupabaseBloodStockRepository(SupabaseBloodStockDataSource(supabaseClient))
    }
    val donationHistoryRepository: DonationHistoryRepository by lazy {
        donationHistoryRepositoryOverride ?: SupabaseDonationHistoryRepository(SupabaseDonationHistoryDataSource(supabaseClient))
    }
    val homeViewModelFactory = viewModelFactory {
        initializer { HomeViewModel(donorProfileRepository, appointmentRepository, campaignRepository, bloodStockRepository) }
    }
    val bloodStockViewModelFactory = viewModelFactory {
        initializer { BloodStockViewModel(bloodStockRepository) }
    }
    val donationHistoryViewModelFactory = viewModelFactory {
        initializer { DonationHistoryViewModel(donationHistoryRepository) }
    }
    val campaignRepository: CampaignRepository by lazy {
        campaignRepositoryOverride ?: SupabaseCampaignRepository(SupabaseCampaignDataSource(supabaseClient))
    }

    val registrationViewModelFactory = viewModelFactory {
        initializer { RegistrationViewModel(authRepository) }
    }
    val campaignsViewModelFactory = viewModelFactory {
        initializer { CampaignsViewModel(campaignRepository) }
    }
    fun campaignDetailViewModelFactory(id: String) = viewModelFactory {
        initializer { CampaignDetailViewModel(campaignRepository, id) }
    }

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

    private val supabaseAppointmentDataSource by lazy {
        SupabaseAppointmentDataSource(
            client = supabaseClient,
        )
    }

    val appointmentRepository: AppointmentRepository by lazy {
        appointmentRepositoryOverride ?: SupabaseAppointmentRepository(
            dataSource = supabaseAppointmentDataSource,
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

    fun appointmentDetailViewModelFactory(appointmentId: String) =
        viewModelFactory {
            initializer {
                AppointmentDetailViewModel(
                    repository = appointmentRepository,
                    appointmentId = appointmentId,
                )
            }
        }

    // Cria o ViewModel que consulta os agendamentos da conta.
    val appointmentsViewModelFactory = viewModelFactory {
        initializer {
            AppointmentsViewModel(
                repository = appointmentRepository,
            )
        }
    }
    // Cria o ViewModel do perfil com o repository configurado.
    val editProfileViewModelFactory = viewModelFactory {
        initializer { EditProfileViewModel(donorProfileRepository) }
    }

    val profileViewModelFactory = viewModelFactory {
        initializer {
            ProfileViewModel(
                repository = donorProfileRepository,
            )
        }
    }

    // Cria o ViewModel que consulta a sessão na abertura.
    val sessionViewModelFactory = viewModelFactory {
        initializer {
            SessionViewModel(
                authRepository = authRepository,
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

    // Essa factory cria o ViewModel usando o repository de autenticação existente.
    val passwordRecoveryViewModelFactory = viewModelFactory {
        initializer {
            PasswordRecoveryViewModel(
                authRepository = authRepository,
            )
        }
    }

    val resetPasswordViewModelFactory = viewModelFactory {
        initializer {
            ResetPasswordViewModel(
                authRepository = authRepository,
            )
        }
    }

    val changePasswordViewModelFactory = viewModelFactory {
        initializer {
            ChangePasswordViewModel(
                authRepository = authRepository,
            )
        }
    }

    val recoveryLinkViewModelFactory = viewModelFactory {
        initializer {
            RecoveryLinkViewModel(
                authRepository = authRepository,
            )
        }
    }

    fun resetPasswordViewModelFactory(updatePassword: suspend (String) -> Result<Unit>) =
        viewModelFactory {
            initializer { ResetPasswordViewModel(updatePassword = updatePassword) }
        }

    // Cria o ViewModel de saída com o repository de autenticação.
    val logoutViewModelFactory = viewModelFactory {
        initializer {
            LogoutViewModel(
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
                appointmentRepository = appointmentRepository,
            )
        }
    }

    val centersViewModelFactory = viewModelFactory {
        initializer {
            CentersViewModel(donationCenterRepository)
        }
    }

    fun centerDetailViewModelFactory(centerId: String) =
        viewModelFactory {
            initializer {
                CenterDetailViewModel(
                    repository = donationCenterRepository,
                    centerId = centerId,
                )
            }
        }
}
