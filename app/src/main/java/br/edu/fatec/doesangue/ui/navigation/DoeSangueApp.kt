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
    )
}