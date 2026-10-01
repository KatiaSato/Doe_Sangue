package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import br.edu.fatec.doesangue.ui.screens.onboarding.SplashScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
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
            SplashScreen(
                onFinished = {
                    navController.navigate(AppGraph.Auth) {
                        // A Splash já cumpriu seu papel de abertura.
                        // Removemos também a própria Splash da pilha
                        // para que Voltar não a exiba novamente.
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
            onEnterMain = {
                // Entrada visual do protótipo.
                // Futuramente, dependerá de autenticação bem-sucedida.
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