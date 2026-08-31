package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

@Stable
class AppNavigator(initial: AppRoute = AppRoute.Splash) {
    private val stack = mutableStateListOf(initial)
    val current: AppRoute get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun navigate(route: AppRoute) {
        if (route != current) stack += route
    }

    fun replaceRoot(route: AppRoute) {
        stack.clear()
        stack += route
    }

    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

@Composable
fun rememberAppNavigator(): AppNavigator = remember { AppNavigator() }

