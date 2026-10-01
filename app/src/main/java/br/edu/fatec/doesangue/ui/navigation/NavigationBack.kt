package br.edu.fatec.doesangue.ui.navigation

import androidx.navigation.NavHostController

// Usado pela seta das telas que também podem ser a raiz de uma aba.
internal fun NavHostController.navigateBackOrHome() {
    // Verificamos antes de remover o destino atual para não esvaziar a pilha.
    // previousBackStackEntry considera a tela anterior, ignorando os grafos.
    if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        // A troca de aba pode ter removido o percurso anterior.
        // Nesse caso, a seta leva ao Início e encerra a seção atual.
        navigate(AppRoute.Home) {
            popUpTo<AppGraph.Main> {
                inclusive = false
            }
            launchSingleTop = true
        }
    }
}
