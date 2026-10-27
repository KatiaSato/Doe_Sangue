package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Estado somente leitura para testes de percursos sem retorno de recuperação.
internal val idleRecoveryLinkState: StateFlow<RecoveryLinkUiState> =
    MutableStateFlow<RecoveryLinkUiState>(RecoveryLinkUiState.Idle).asStateFlow()

@Composable
internal fun FeatureTestHost(
    container: AppContainer, nav: NavHostController,
    registrationFactory: ViewModelProvider.Factory = container.registrationViewModelFactory,
    campaignsFactory: ViewModelProvider.Factory = container.campaignsViewModelFactory,
    homeFactory: ViewModelProvider.Factory = isolatedHomeFactory(),
    recoveryLinkState: StateFlow<RecoveryLinkUiState> = idleRecoveryLinkState,
    sessionState: StateFlow<br.edu.fatec.doesangue.presentation.auth.SessionMonitorUiState>? = null,
) {
    AppNavHost(
        recoveryLinkState = recoveryLinkState,
        sessionState = sessionState,
        navController = nav,
        schedulingViewModelFactory = container.schedulingViewModelFactory,
                    routineViewModelFactory = container.routineViewModelFactory,
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
        centersViewModelFactory = container.centersViewModelFactory,
        centerDetailViewModelFactory = container::centerDetailViewModelFactory,
        passwordRecoveryViewModelFactory = container.passwordRecoveryViewModelFactory,
        resetPasswordViewModelFactory = { container.resetPasswordViewModelFactory },
        changePasswordViewModelFactory = container.changePasswordViewModelFactory,
        editProfileViewModelFactory = container.editProfileViewModelFactory,
    )
}
