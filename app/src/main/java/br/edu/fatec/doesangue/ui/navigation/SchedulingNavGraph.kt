package br.edu.fatec.doesangue.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleCenterScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleConfirmScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleDateScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleRecurrenceScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleSuccessScreen

// Responsabilidade desse arquivo: Unidade → Data/horário → Recorrência → Revisão → Resultado visual
/*
* | Parâmetro | Responsabilidade |
| `navController` | Avançar e voltar entre as etapas do fluxo. |
| `onNavigateMain` | Comunicar cliques na barra inferior para a navegação principal. |
| `onFinishScheduling` | Comunicar que a pessoa terminou o percurso e pediu para voltar à Home. |
*
 */

fun NavGraphBuilder.schedulingNavGraph(
    navController: NavHostController,
    // função que recebe uma rota e executa uma ação, sem devolver um resultado útil
    onNavigateMain: (AppRoute) -> Unit,
    // função sem parâmetros
    onFinishScheduling: () -> Unit,
) {
    navigation<AppGraph.Scheduling>(
        startDestination = AppRoute.ScheduleCenter,
    ) {
        composable<AppRoute.ScheduleCenter> {
            ScheduleCenterScreen(
                onBack = {
                    navController.navigateBackOrHome()
                },
                onContinue = {
                    navController.navigate(AppRoute.ScheduleDate)
                },
                onNavigate = onNavigateMain,
            )
        }

        composable<AppRoute.ScheduleDate> {
            ScheduleDateScreen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = {
                    navController.navigate(AppRoute.ScheduleRecurrence)
                },
            )
        }

        composable<AppRoute.ScheduleRecurrence> {
            ScheduleRecurrenceScreen(
                onBack = {
                    navController.popBackStack()
                },
                onContinue = {
                    navController.navigate(AppRoute.ScheduleConfirm)
                },
            )
        }

        composable<AppRoute.ScheduleConfirm> {
            ScheduleConfirmScreen(
                onBack = {
                    navController.popBackStack()
                },
                onConfirm = {
                    // Apenas reproduz o fluxo visual do protótipo.
                    // Na etapa funcional, avançar somente após
                    // a operação retornar sucesso e o ID persistido.
                    navController.navigate(AppRoute.ScheduleSuccess)
                },
            )
        }

        composable<AppRoute.ScheduleSuccess> {
            ScheduleSuccessScreen(
                onHome = onFinishScheduling,
            )
        }
    }
}