package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkUiState
import br.edu.fatec.doesangue.presentation.auth.EmailConfirmationViewModel
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DoeSangueApp(
    appContainer: AppContainer,
    recoveryLinkState: StateFlow<RecoveryLinkUiState>,
    onUpdateRecoveryPassword: suspend (Long, String) -> Result<Unit>,
    onRetryRecovery: () -> Unit,
    onFinishRecovery: () -> Unit,
    onRecoveryFinished: () -> Unit,
    emailConfirmationViewModel: EmailConfirmationViewModel,
) {
    val navController = rememberNavController()

    AppNavHost(
        navController = navController,
        recoveryLinkState = recoveryLinkState,
        onRetryRecovery = onRetryRecovery,
        onFinishRecovery = onFinishRecovery,
        onRecoveryFinished = onRecoveryFinished,
        emailConfirmationState = emailConfirmationViewModel.uiState,
        onConfirmationEmailChange = emailConfirmationViewModel::updateEmail,
        onResendConfirmation = emailConfirmationViewModel::resend,
        onRetryConfirmation = emailConfirmationViewModel::retryLink,
        onDismissConfirmation = emailConfirmationViewModel::dismissLink,
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
        editProfileViewModelFactory = appContainer.editProfileViewModelFactory,
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
