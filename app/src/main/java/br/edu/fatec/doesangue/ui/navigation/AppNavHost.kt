package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import br.edu.fatec.doesangue.ui.screens.onboarding.SplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.fatec.doesangue.presentation.auth.SessionViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    schedulingViewModelFactory: ViewModelProvider.Factory,
    loginViewModelFactory: ViewModelProvider.Factory,
    profileViewModelFactory: ViewModelProvider.Factory,
    logoutViewModelFactory: ViewModelProvider.Factory,
    sessionViewModelFactory: ViewModelProvider.Factory,
) {
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
            onEnterMain = {
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
            logoutViewModelFactory = logoutViewModelFactory,
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
                navController.navigate(AppRoute.HomeScheduled) {
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
