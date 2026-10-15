package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import br.edu.fatec.doesangue.di.AppContainer

@Composable
internal fun FeatureTestHost(
    container: AppContainer, nav: NavHostController,
    registrationFactory: ViewModelProvider.Factory = container.registrationViewModelFactory,
    campaignsFactory: ViewModelProvider.Factory = container.campaignsViewModelFactory,
    homeFactory: ViewModelProvider.Factory = isolatedHomeFactory(),
) {
    AppNavHost(
        navController = nav,
        schedulingViewModelFactory = container.schedulingViewModelFactory,
        loginViewModelFactory = container.loginViewModelFactory,
        profileViewModelFactory = container.profileViewModelFactory,
        appointmentsViewModelFactory = container.appointmentsViewModelFactory,
        appointmentDetailViewModelFactory = container::appointmentDetailViewModelFactory,
        logoutViewModelFactory = container.logoutViewModelFactory,
        sessionViewModelFactory = container.sessionViewModelFactory,
        registrationViewModelFactory = registrationFactory,
        campaignsViewModelFactory = campaignsFactory,
        campaignDetailViewModelFactory = container::campaignDetailViewModelFactory,
        homeViewModelFactory = homeFactory,
        bloodStockViewModelFactory = container.bloodStockViewModelFactory,
        donationHistoryViewModelFactory = container.donationHistoryViewModelFactory,
    )
}
