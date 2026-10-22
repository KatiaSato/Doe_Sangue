package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkUiState
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DoeSangueApp(
    appContainer: AppContainer,
    recoveryLinkState: StateFlow<RecoveryLinkUiState>,
    onUpdateRecoveryPassword: suspend (Long, String) -> Result<Unit>,
    onRetryRecovery: () -> Unit,
    onFinishRecovery: () -> Unit,
    onRecoveryFinished: () -> Unit,
) {
    val navController = rememberNavController()

    AppNavHost(
        navController = navController,
        recoveryLinkState = recoveryLinkState,
        onRetryRecovery = onRetryRecovery,
        onFinishRecovery = onFinishRecovery,
        onRecoveryFinished = onRecoveryFinished,
        schedulingViewModelFactory = appContainer.schedulingViewModelFactory,
        loginViewModelFactory = appContainer.loginViewModelFactory,
        passwordRecoveryViewModelFactory =
            appContainer.passwordRecoveryViewModelFactory,
        resetPasswordViewModelFactory = { validationId ->
            appContainer.resetPasswordViewModelFactory { password ->
                onUpdateRecoveryPassword(validationId, password)
            }
        },
        changePasswordViewModelFactory =
            appContainer.changePasswordViewModelFactory,
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
        centersViewModelFactory = appContainer.centersViewModelFactory,
        centerDetailViewModelFactory =
            appContainer::centerDetailViewModelFactory,
    )
}
