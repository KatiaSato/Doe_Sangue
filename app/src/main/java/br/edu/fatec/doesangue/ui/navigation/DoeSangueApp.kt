package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController

@Composable
fun DoeSangueApp() {
    val navController = rememberNavController()

    AppNavHost(
        navController = navController,
    )
}