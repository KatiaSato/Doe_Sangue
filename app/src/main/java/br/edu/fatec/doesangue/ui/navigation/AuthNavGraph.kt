package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import br.edu.fatec.doesangue.presentation.auth.LoginViewModel
import br.edu.fatec.doesangue.presentation.auth.RegistrationViewModel
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
    registrationViewModelFactory: ViewModelProvider.Factory,
    onEnterMain: () -> Unit,
) {
    navigation<AppGraph.Auth>(startDestination = AppRoute.Welcome) {
        composable<AppRoute.Welcome> { entry ->
            val registration = registrationModel(navController, registrationViewModelFactory, entry)
            WelcomeScreen(
                onStart = { registration.reset(); navController.navigate(AppRoute.Register1) },
                onLogin = { navController.navigate(AppRoute.Login) },
            )
        }
        composable<AppRoute.Login> { entry ->
            val login: LoginViewModel = viewModel(factory = loginViewModelFactory)
            val registration = registrationModel(navController, registrationViewModelFactory, entry)
            val state by login.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.authenticatedUser?.id) {
                if (state.authenticatedUser != null) onEnterMain()
            }
            LoginScreen(state, login::signIn,
                onForgot = { navController.navigate(AppRoute.ForgotPassword) },
                onRegister = { registration.reset(); navController.navigate(AppRoute.Register1) },
            )
        }
        composable<AppRoute.Register1> { entry ->
            val model = registrationModel(navController, registrationViewModelFactory, entry)
            val state by model.uiState.collectAsStateWithLifecycle()
            RegisterStep1Screen(state, model::updateName, model::updateEmail,
                onBack = { navController.popBackStack() },
                onContinue = { if (model.validateIdentity()) navController.navigate(AppRoute.Register2) },
                onLogin = {
                    model.reset()
                    navController.navigate(AppRoute.Login) {
                        popUpTo<AppRoute.Welcome> { inclusive = false }; launchSingleTop = true
                    }
                },
            )
        }
        composable<AppRoute.Register2> { entry ->
            val model = registrationModel(navController, registrationViewModelFactory, entry)
            val state by model.uiState.collectAsStateWithLifecycle()
            RegisterStep2Screen(state, model::selectBloodType,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(AppRoute.Register3) },
            )
        }
        composable<AppRoute.Register3> { entry ->
            val model = registrationModel(navController, registrationViewModelFactory, entry)
            val state by model.uiState.collectAsStateWithLifecycle()
            RegisterStep3Screen(state, onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(AppRoute.Register4) })
        }
        composable<AppRoute.Register4> { entry ->
            val model = registrationModel(navController, registrationViewModelFactory, entry)
            val state by model.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.result) {
                state.result?.let { result ->
                    if (result.authenticatedUser != null) onEnterMain()
                    else navController.navigate(AppRoute.EmailVerification) {
                        popUpTo<AppRoute.Register1> { inclusive = true }; launchSingleTop = true
                    }
                }
            }
            RegisterStep4Screen(state, onBack = { navController.popBackStack() }, onCreate = model::register)
        }
        composable<AppRoute.EmailVerification> { entry ->
            val model = registrationModel(navController, registrationViewModelFactory, entry)
            EmailVerificationScreen(onUnderstood = {
                model.reset()
                navController.navigate(AppRoute.Login) {
                    popUpTo<AppRoute.Welcome> { inclusive = false }; launchSingleTop = true
                }
            })
        }
        composable<AppRoute.ForgotPassword> {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun registrationModel(
    nav: NavHostController, factory: ViewModelProvider.Factory, entry: NavBackStackEntry,
): RegistrationViewModel {
    val owner = remember(nav, entry) { nav.getBackStackEntry<AppGraph.Auth>() }
    return viewModel(viewModelStoreOwner = owner, factory = factory)
}
