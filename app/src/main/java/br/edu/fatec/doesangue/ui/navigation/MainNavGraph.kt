package br.edu.fatec.doesangue.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import br.edu.fatec.doesangue.presentation.profile.ProfileViewModel
import br.edu.fatec.doesangue.ui.screens.restrictions.RestrictionsScreen
import br.edu.fatec.doesangue.ui.screens.restrictions.RestrictionDetailScreen
import br.edu.fatec.doesangue.ui.screens.home.HomeScheduledScreen
import br.edu.fatec.doesangue.ui.screens.home.HomeScreen
import br.edu.fatec.doesangue.ui.screens.campaigns.CampaignDetailScreen
import br.edu.fatec.doesangue.ui.screens.campaigns.CampaignsScreen
import br.edu.fatec.doesangue.ui.screens.centers.CenterDetailScreen
import br.edu.fatec.doesangue.ui.screens.centers.CentersScreen
import br.edu.fatec.doesangue.ui.screens.donations.DonationAppointmentsScreen
import br.edu.fatec.doesangue.ui.screens.donations.DonationHistoryScreen
import br.edu.fatec.doesangue.ui.screens.profile.AboutScreen
import br.edu.fatec.doesangue.ui.screens.profile.ContactScreen
import br.edu.fatec.doesangue.ui.screens.profile.EditProfileScreen
import br.edu.fatec.doesangue.ui.screens.profile.FaqScreen
import br.edu.fatec.doesangue.ui.screens.profile.NotificationsScreen
import br.edu.fatec.doesangue.ui.screens.profile.ProfileScreen
import br.edu.fatec.doesangue.ui.screens.profile.SecurityScreen
import br.edu.fatec.doesangue.ui.screens.profile.SettingsScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.AppointmentCancelledScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.AppointmentDetailScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.CancelAppointmentScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.EditRoutineScreen

fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
    schedulingViewModelFactory: ViewModelProvider.Factory,
    profileViewModelFactory: ViewModelProvider.Factory,
    onNavigateMain: (AppRoute) -> Unit,
    onFinishScheduling: () -> Unit,
) {
    navigation<AppGraph.Main>(
        startDestination = AppRoute.Home,
    ) {
        composable<AppRoute.Home> {
            HomeScreen(
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.HomeScheduled> {
            // Variante visual do protótipo.
            // Futuramente, a Home exibirá os agendamentos
            // conforme o estado fornecido pelo ViewModel.
            HomeScheduledScreen(
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.Restrictions> {
            RestrictionsScreen(
                onBack = { navController.navigateBackOrHome() },
                onCategory = { category ->
                    navController.navigate(AppRoute.RestrictionDetail(category)) {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<AppRoute.RestrictionDetail> { entry ->
            RestrictionDetailScreen(
                category = entry.toRoute<AppRoute.RestrictionDetail>().category,
                onBack = { navController.navigateBackOrHome() },
            )
        }

        // Doações
        composable<AppRoute.DonationsHistory> {
            DonationHistoryScreen(
                onBack = {
                    navController.navigateBackOrHome()
                },
                onAppointments = {
                    navController.navigate(AppRoute.DonationsAppointments) {
                        // Troca a seção exibida sem acumular
                        // Histórico e Agendamentos na pilha.
                        popUpTo<AppRoute.DonationsHistory> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.DonationsAppointments> {
            DonationAppointmentsScreen(
                onBack = {
                    navController.navigateBackOrHome()
                },
                onHistory = {
                    navController.navigate(AppRoute.DonationsHistory) {
                        popUpTo<AppRoute.DonationsAppointments> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onDetails = {
                    navController.navigate(AppRoute.AppointmentDetail)
                },
                onNavigate = onNavigateMain,
            )
        }

        // Acompanhamento do agendamento
        composable<AppRoute.AppointmentDetail> {
            AppointmentDetailScreen(
                onBack = {
                    navController.popBackStack()
                },
                onChange = {
                    // Mantém o caminho visual existente.
                    // A edição real precisará do ID e dos dados do agendamento.
                    navController.navigate(AppRoute.ScheduleDate)
                },
                onEditRoutine = {
                    navController.navigate(AppRoute.EditRoutine)
                },
                onCancel = {
                    navController.navigate(AppRoute.CancelAppointment)
                },
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.EditRoutine> {
            EditRoutineScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }

        composable<AppRoute.CancelAppointment> {
            CancelAppointmentScreen(
                onBack = {
                    navController.popBackStack()
                },
                onCancel = {
                    // Demonstração visual: nenhuma operação de cancelamento
                    // é executada no banco.
                    navController.navigate(AppRoute.AppointmentCancelled) {
                        // Retira o detalhe e a confirmação do cancelamento
                        // para que Voltar não reabra esse percurso concluído.
                        popUpTo<AppRoute.AppointmentDetail> {
                            inclusive = true
                        }
                    }
                },
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.AppointmentCancelled> {
            AppointmentCancelledScreen(
                onNew = {
                    onNavigateMain(AppRoute.ScheduleCenter)
                },
                onHome = {
                    onNavigateMain(AppRoute.Home)
                },
            )
        }

        // Campanhas
        composable<AppRoute.Campaigns> {
            CampaignsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onDetail = {
                    navController.navigate(AppRoute.CampaignDetail)
                },
                onDonate = {
                    onNavigateMain(AppRoute.ScheduleCenter)
                },
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.CampaignDetail> {
            CampaignDetailScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSchedule = {
                    onNavigateMain(AppRoute.ScheduleCenter)
                },
                onNavigate = onNavigateMain,
            )
        }

        // Unidades
        composable<AppRoute.Centers> {
            CentersScreen(
                onBack = {
                    navController.navigateBackOrHome()
                },
                onDetail = {
                    navController.navigate(AppRoute.CenterDetail)
                },
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.CenterDetail> {
            CenterDetailScreen(
                onBack = {
                    navController.popBackStack()
                },
                onSchedule = {
                    onNavigateMain(AppRoute.ScheduleCenter)
                },
                onNavigate = onNavigateMain,
            )
        }

        // Perfil e configurações
        composable<AppRoute.Profile> {
            val profileViewModel: ProfileViewModel = viewModel(
                factory = profileViewModelFactory,
            )

            val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
            ProfileScreen(
                uiState = uiState,
                onRetry = profileViewModel::loadProfile,
                onDisplayNameChange = profileViewModel::updateDisplayName,
                onCreateProfile = profileViewModel::createProfile,
                onBack = {
                    navController.navigateBackOrHome()
                },
                onDonations = {
                    onNavigateMain(AppRoute.DonationsHistory)
                },
                onAppointment = {
                    navController.navigate(AppRoute.AppointmentDetail)
                },
                onSettings = {
                    navController.navigate(AppRoute.Settings)
                },
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.Settings> {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onEditProfile = {
                    navController.navigate(AppRoute.EditProfile)
                },
                onSecurity = {
                    navController.navigate(AppRoute.Security)
                },
                onNotifications = {
                    navController.navigate(AppRoute.Notifications)
                },
                onFaq = {
                    navController.navigate(AppRoute.Faq)
                },
                onContact = {
                    navController.navigate(AppRoute.Contact)
                },
                onAbout = {
                    navController.navigate(AppRoute.About)
                },
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.EditProfile> {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<AppRoute.Security> {
            SecurityScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<AppRoute.Notifications> {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<AppRoute.Faq> {
            FaqScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<AppRoute.Contact> {
            ContactScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable<AppRoute.About> {
            AboutScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // Inclui o fluxo de agendamento dentro da área principal.
        schedulingNavGraph(
            navController = navController,
            schedulingViewModelFactory = schedulingViewModelFactory,
            onNavigateMain = onNavigateMain,
            onFinishScheduling = onFinishScheduling,
        )
    }
}
