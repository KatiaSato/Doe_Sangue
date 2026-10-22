package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import br.edu.fatec.doesangue.ui.screens.onboarding.SplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkUiState
import br.edu.fatec.doesangue.presentation.auth.SessionViewModel
import br.edu.fatec.doesangue.presentation.auth.ResetPasswordViewModel
import br.edu.fatec.doesangue.ui.screens.auth.ResetPasswordScreen
import br.edu.fatec.doesangue.ui.screens.auth.RecoveryStatusScreen
import kotlinx.coroutines.flow.StateFlow

@Composable
fun AppNavHost(
    navController: NavHostController,
    recoveryLinkState: StateFlow<RecoveryLinkUiState>,
    onRetryRecovery: () -> Unit = {},
    onFinishRecovery: () -> Unit = {},
    onRecoveryFinished: () -> Unit = {},
    schedulingViewModelFactory: ViewModelProvider.Factory,
    loginViewModelFactory: ViewModelProvider.Factory,
    passwordRecoveryViewModelFactory: ViewModelProvider.Factory,
    resetPasswordViewModelFactory: (Long) -> ViewModelProvider.Factory,
    changePasswordViewModelFactory: ViewModelProvider.Factory,
    profileViewModelFactory: ViewModelProvider.Factory,
    appointmentsViewModelFactory: ViewModelProvider.Factory,
    appointmentDetailViewModelFactory: (String) -> ViewModelProvider.Factory,
    logoutViewModelFactory: ViewModelProvider.Factory,
    sessionViewModelFactory: ViewModelProvider.Factory,
    registrationViewModelFactory: ViewModelProvider.Factory,
    campaignsViewModelFactory: ViewModelProvider.Factory,
    campaignDetailViewModelFactory: (String) -> ViewModelProvider.Factory,
    homeViewModelFactory: ViewModelProvider.Factory,
    bloodStockViewModelFactory: ViewModelProvider.Factory,
    donationHistoryViewModelFactory: ViewModelProvider.Factory,
    centersViewModelFactory: ViewModelProvider.Factory,
    centerDetailViewModelFactory: (String) -> ViewModelProvider.Factory,
) {
    val currentRecoveryLinkState by
    recoveryLinkState.collectAsStateWithLifecycle()

    // Cada transição remove as telas e ViewModels da conta/tentativa anterior.
    LaunchedEffect(currentRecoveryLinkState) {
        when (currentRecoveryLinkState) {
            RecoveryLinkUiState.Idle -> Unit
            RecoveryLinkUiState.Finished -> {
                navController.navigate(AppRoute.Login) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
                onRecoveryFinished()
            }
            else -> navController.navigate(AppRoute.ResetPassword) {
                popUpTo(navController.graph.id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    BackHandler(enabled = currentRecoveryLinkState != RecoveryLinkUiState.Idle) {
        onFinishRecovery()
    }

    // Define como abrir destinos solicitados pelas telas
    // e pela barra inferior.
    fun navigateMain(route: AppRoute) {
        when (route) {
            AppRoute.ScheduleCenter -> {
                // Entramos pelo grupo para respeitar seu destino inicial.
                navController.navigate(AppGraph.Scheduling) {
                    popUpTo<AppGraph.Main> {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }

            AppRoute.Home,
            AppRoute.DonationsHistory,
            AppRoute.Centers,
            AppRoute.Profile -> {
                // Nesta primeira versão, trocar de seção
                // remove o percurso anterior da área principal.
                navController.navigate(route) {
                    popUpTo<AppGraph.Main> {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }

            else -> {
                // Destinos internos mantêm a origem para permitir Voltar.
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Splash,
    ) {
        composable<AppRoute.ResetPassword> {
            when (val recovery = currentRecoveryLinkState) {
                is RecoveryLinkUiState.Validated -> {
                    val model: ResetPasswordViewModel = viewModel(
                        key = "reset-${recovery.validationId}",
                        factory = resetPasswordViewModelFactory(recovery.validationId),
                    )
                    val state by model.uiState.collectAsStateWithLifecycle()
                    ResetPasswordScreen(
                        state = state,
                        onSave = { password, confirmation ->
                            if (recoveryLinkState.value == recovery) {
                                model.savePassword(password, confirmation)
                            }
                        },
                        onFinish = onFinishRecovery,
                    )
                }
                is RecoveryLinkUiState.Error -> RecoveryStatusScreen(
                    message = recovery.message,
                    onRetry = if (recovery.canRetry) onRetryRecovery else null,
                    onExit = onFinishRecovery,
                )
                is RecoveryLinkUiState.Leaving -> RecoveryStatusScreen(
                    message = recovery.errorMessage ?: "Encerrando a sessão…",
                    onRetry = if (recovery.errorMessage != null) onFinishRecovery else null,
                )
                RecoveryLinkUiState.Processing -> RecoveryStatusScreen(
                    message = "Validando seu link…",
                    onExit = onFinishRecovery,
                )
                RecoveryLinkUiState.Finished -> RecoveryStatusScreen("Concluindo…")
                RecoveryLinkUiState.Idle -> {
                    LaunchedEffect(Unit) {
                        navController.navigate(AppRoute.Login) {
                            popUpTo(navController.graph.id) { inclusive = false }
                        }
                    }
                }
            }
        }
        composable<AppRoute.Splash> {

            val sessionViewModel: SessionViewModel = viewModel(
                factory = sessionViewModelFactory,
            )

            val sessionUiState by
            sessionViewModel.uiState.collectAsStateWithLifecycle()
            SplashScreen(
                uiState = sessionUiState,
                onRetry = sessionViewModel::restoreSession,
                onFinished = { authenticated ->
                    if (recoveryLinkState.value != RecoveryLinkUiState.Idle) {
                        return@SplashScreen
                    }
                    val destination = if (authenticated) {
                        AppGraph.Main
                    } else {
                        AppGraph.Auth
                    }

                    navController.navigate(destination) {
                        // Remove a Splash após concluir a abertura.
                        popUpTo<AppRoute.Splash> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        authNavGraph(
            navController = navController,
            loginViewModelFactory = loginViewModelFactory,
            passwordRecoveryViewModelFactory = passwordRecoveryViewModelFactory,
            registrationViewModelFactory = registrationViewModelFactory,
            onEnterMain = {
                if (recoveryLinkState.value != RecoveryLinkUiState.Idle) {
                    return@authNavGraph
                }
                // Abre a área principal após o login bem-sucedido.
                navController.navigate(AppGraph.Main) {
                    // Ao entrar, remove todo o fluxo de autenticação.
                    popUpTo<AppGraph.Auth> {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
        )

        mainNavGraph(
            navController = navController,
            schedulingViewModelFactory = schedulingViewModelFactory,
            profileViewModelFactory = profileViewModelFactory,
            changePasswordViewModelFactory = changePasswordViewModelFactory,
            appointmentsViewModelFactory = appointmentsViewModelFactory,
            appointmentDetailViewModelFactory = appointmentDetailViewModelFactory,
            logoutViewModelFactory = logoutViewModelFactory,
            campaignsViewModelFactory = campaignsViewModelFactory,
            campaignDetailViewModelFactory = campaignDetailViewModelFactory,
            homeViewModelFactory = homeViewModelFactory,
            bloodStockViewModelFactory = bloodStockViewModelFactory,
            donationHistoryViewModelFactory = donationHistoryViewModelFactory,
            centersViewModelFactory = centersViewModelFactory,
            centerDetailViewModelFactory = centerDetailViewModelFactory,
            onSignedOut = {
                navController.navigate(AppGraph.Auth) {
                    // Remove as telas da conta que acabou de sair.
                    popUpTo<AppGraph.Main> {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onNavigateMain = ::navigateMain,
            onFinishScheduling = {
                navController.navigate(AppRoute.Home) {
                    // Encerra o percurso visual de agendamento.
                    // Voltar não deve reabrir as etapas concluídas.
                    popUpTo<AppGraph.Main> {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            },
        )
    }
}
