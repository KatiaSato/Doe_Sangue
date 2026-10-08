package br.edu.fatec.doesangue.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel

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

    schedulingViewModelFactory: ViewModelProvider.Factory,
    // função que recebe uma rota e executa uma ação, sem devolver um resultado útil
    onNavigateMain: (AppRoute) -> Unit,
    // função sem parâmetros
    onFinishScheduling: () -> Unit,
) {
    navigation<AppGraph.Scheduling>(
        startDestination = AppRoute.ScheduleCenter,
    ) {
        composable<AppRoute.ScheduleCenter> { entry ->
            // Localiza o grafo de agendamento que contém esta tela.
            val schedulingEntry = remember(entry) {
                navController.getBackStackEntry<AppGraph.Scheduling>()
            }

            // Obtem um ViewModel vinculado ao fluxo inteiro de agendamento.
            val schedulingViewModel: SchedulingViewModel = viewModel(
                viewModelStoreOwner = schedulingEntry,
                factory = schedulingViewModelFactory,
            )
            // Observa o estado respeitando o ciclo de vida da tela.
            val uiState by schedulingViewModel.uiState.collectAsStateWithLifecycle()

            //::selectCenter passa uma referência à função. Ela será executada quando a tela comunicar a escolha.
            ScheduleCenterScreen(
                uiState = uiState,
                onSelectCenter = schedulingViewModel::selectCenter,
                onBack = {
                    navController.navigateBackOrHome()
                },
                onContinue = {
                    navController.navigate(AppRoute.ScheduleDate)
                },
                onNavigate = onNavigateMain,
                onRetry = schedulingViewModel::loadCenters,
            )
        }

        composable<AppRoute.ScheduleDate> { entry ->
            // Localiza o mesmo grafo usado na etapa de escolha da unidade.
            val schedulingEntry = remember(entry) {
                navController.getBackStackEntry<AppGraph.Scheduling>()
            }

            // Obtém o ViewModel compartilhado entre as etapas.
            val schedulingViewModel: SchedulingViewModel = viewModel(
                viewModelStoreOwner = schedulingEntry,
                factory = schedulingViewModelFactory,
            )

            val uiState by schedulingViewModel.uiState.collectAsStateWithLifecycle()

            ScheduleDateScreen(
                uiState = uiState,
                onSelectDate = schedulingViewModel::selectDate,
                onSelectTime = schedulingViewModel::selectTime,
                onRetry = schedulingViewModel::loadTimeSlots,
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

        composable<AppRoute.ScheduleConfirm> { entry ->
            // Localiza o grafo que guarda o rascunho do agendamento.
            val schedulingEntry = remember(entry) {
                navController.getBackStackEntry<AppGraph.Scheduling>()
            }

            // Recupera o mesmo ViewModel usado nas etapas anteriores.
            val schedulingViewModel: SchedulingViewModel = viewModel(
                viewModelStoreOwner = schedulingEntry,
                factory = schedulingViewModelFactory,
            )

            // Observa o estado atual com unidade, data e horário.
            val uiState by schedulingViewModel.uiState.collectAsStateWithLifecycle()
            ScheduleConfirmScreen(
                uiState = uiState,
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