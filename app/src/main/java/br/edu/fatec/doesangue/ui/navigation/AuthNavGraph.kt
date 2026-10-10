package br.edu.fatec.doesangue.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.fatec.doesangue.presentation.auth.LoginViewModel
import br.edu.fatec.doesangue.ui.screens.auth.EmailVerificationScreen
import br.edu.fatec.doesangue.ui.screens.auth.ForgotPasswordScreen
import br.edu.fatec.doesangue.ui.screens.auth.LoginScreen
import br.edu.fatec.doesangue.ui.screens.auth.RegisterStep1Screen
import br.edu.fatec.doesangue.ui.screens.auth.RegisterStep2Screen
import br.edu.fatec.doesangue.ui.screens.auth.RegisterStep3Screen
import br.edu.fatec.doesangue.ui.screens.auth.RegisterStep4Screen
import br.edu.fatec.doesangue.ui.screens.onboarding.WelcomeScreen

fun NavGraphBuilder.authNavGraph(
    navController: NavHostController,
    loginViewModelFactory: ViewModelProvider.Factory,
    onEnterMain: () -> Unit,
) {
    navigation<AppGraph.Auth>(
        startDestination = AppRoute.Welcome,
    ) {
        composable<AppRoute.Welcome> {
            WelcomeScreen(
                onStart = {
                    navController.navigate(AppRoute.Register1)
                },
                onLogin = {
                    navController.navigate(AppRoute.Login)
                },
            )
        }

        composable<AppRoute.Login> {
            val loginViewModel: LoginViewModel = viewModel(
                factory = loginViewModelFactory,
            )

            val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(uiState.authenticatedUser?.id) {
                if (uiState.authenticatedUser != null) {
                    onEnterMain()
                }
            }
            LoginScreen(

                uiState = uiState,
                onSignIn = loginViewModel::signIn,
                onForgot = {
                    navController.navigate(AppRoute.ForgotPassword)
                },
                onRegister = {
                    navController.navigate(AppRoute.Register1)
                },
            )
        }

        composable<AppRoute.Register1> {
            RegisterStep1Screen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = {
                    navController.navigate(AppRoute.Register2)
                },
                onLogin = {
                    navController.navigate(AppRoute.Login) {
                        // Encerra o percurso de cadastro ao escolher entrar.
                        // retira os desyinos acima de Boas-vindas
                        popUpTo<AppRoute.Welcome> {
                            inclusive = false //mantem Boas-vindas na pilha
                        }
                        launchSingleTop = true //evita acrescentar outra copia de Login se ele ja estiver no topo apos ajuste da pilha
                    }
                },
            )
        }

        composable<AppRoute.Register2> {
            RegisterStep2Screen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = {
                    navController.navigate(AppRoute.Register3)
                },
            )
        }

        composable<AppRoute.Register3> {
            RegisterStep3Screen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = {
                    navController.navigate(AppRoute.Register4)
                },
            )
        }

        composable<AppRoute.Register4> {
            RegisterStep4Screen(
                onBack = {
                    navController.popBackStack()
                },
                onCreate = {
                    // Transição visual: ainda não cria conta nem envia e-mail.
                    navController.navigate(AppRoute.EmailVerification)
                },
            )
        }

        composable<AppRoute.EmailVerification> {
            EmailVerificationScreen(
                onUnderstood = {
                    navController.navigate(AppRoute.Login) {
                        // Remove as etapas de cadastro do histórico,
                        // preservando a tela de boas-vindas.
                        popUpTo<AppRoute.Welcome> {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable<AppRoute.ForgotPassword> {
            ForgotPasswordScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}