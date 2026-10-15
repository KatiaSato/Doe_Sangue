package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.remember
import br.edu.fatec.doesangue.di.AppContainer

@Composable
fun DoeSangueApp() {
    val navController = rememberNavController()
    // Cria o container e mantém a mesma instância nas recomposições.
    val appContainer = remember { AppContainer() }

    AppNavHost(
        navController = navController,
        schedulingViewModelFactory = appContainer.schedulingViewModelFactory,
        loginViewModelFactory = appContainer.loginViewModelFactory,
        profileViewModelFactory = appContainer.profileViewModelFactory,
        appointmentsViewModelFactory = appContainer.appointmentsViewModelFactory,
        appointmentDetailViewModelFactory =
            appContainer::appointmentDetailViewModelFactory,
        logoutViewModelFactory = appContainer.logoutViewModelFactory,
        sessionViewModelFactory = appContainer.sessionViewModelFactory,
        registrationViewModelFactory = appContainer.registrationViewModelFactory,
        campaignsViewModelFactory = appContainer.campaignsViewModelFactory,
        campaignDetailViewModelFactory = appContainer::campaignDetailViewModelFactory,
        homeViewModelFactory = appContainer.homeViewModelFactory,
        bloodStockViewModelFactory = appContainer.bloodStockViewModelFactory,
        donationHistoryViewModelFactory = appContainer.donationHistoryViewModelFactory,
    )
}
