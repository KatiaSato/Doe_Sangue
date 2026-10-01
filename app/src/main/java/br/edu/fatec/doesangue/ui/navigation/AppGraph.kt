package br.edu.fatec.doesangue.ui.navigation

import kotlinx.serialization.Serializable

// Identifica os grupos de navegação do aplicativo.
// Cada grupo terá suas telas e um destino inicial.
sealed interface AppGraph {

    @Serializable
    data object Auth : AppGraph

    @Serializable
    data object Main : AppGraph

    @Serializable
    data object Scheduling : AppGraph
}