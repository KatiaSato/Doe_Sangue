package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.toRoute
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.model.SchedulingStep
import br.edu.fatec.doesangue.presentation.scheduling.CenterPreparationUiState
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel
import br.edu.fatec.doesangue.ui.screens.scheduling.CenterPreparationScreen

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
    routineViewModelFactory: ViewModelProvider.Factory,
    // função que recebe uma rota e executa uma ação, sem devolver um resultado útil
    onNavigateMain: (AppRoute) -> Unit,
    // função sem parâmetros
    onFinishScheduling: () -> Unit,
) {
    navigation<AppGraph.Scheduling>(
        startDestination = AppRoute.ScheduleCenter,
    ) {
        composable<AppRoute.ScheduleReschedule> { entry ->
            val route = entry.toRoute<AppRoute.ScheduleReschedule>()
            val parent = remember(entry) { navController.getBackStackEntry<AppGraph.Scheduling>() }
            val model: SchedulingViewModel = viewModel(viewModelStoreOwner=parent,factory=schedulingViewModelFactory)
            val state by model.uiState.collectAsStateWithLifecycle()
            SchedulingDraftGate(model,navController,entry,SchedulingStep.CENTER,onFinishScheduling) {
                LaunchedEffect(route.appointmentId) { model.prepareReschedule(route.appointmentId) }
                LaunchedEffect(state.rescheduleOrigin?.appointmentId) {
                    if (state.rescheduleOrigin?.appointmentId == route.appointmentId) navController.navigate(AppRoute.ScheduleCenter) {
                        popUpTo<AppRoute.ScheduleReschedule> { inclusive=true }; launchSingleTop=true
                    }
                }
                br.edu.fatec.doesangue.ui.components.DsScreen(title="Remarcar agendamento",onBack={navController.navigateBackOrHome()}) {
                    if (state.rescheduleLoading) androidx.compose.material3.Text("Consultando agendamento…")
                    state.rescheduleError?.let { message ->
                        androidx.compose.material3.Text(message)
                        br.edu.fatec.doesangue.ui.components.SecondaryButton("Atualizar agendamento",{model.prepareReschedule(route.appointmentId)})
                    }
                }
            }
        }

        composable<AppRoute.ScheduleFromCenter> { entry ->
            val route = entry.toRoute<AppRoute.ScheduleFromCenter>()

            val schedulingEntry = remember(entry) {
                navController.getBackStackEntry<AppGraph.Scheduling>()
            }

            val model: SchedulingViewModel = viewModel(
                viewModelStoreOwner = schedulingEntry,
                factory = schedulingViewModelFactory,
            )

            val state by model.uiState.collectAsStateWithLifecycle()

            SchedulingDraftGate(model, navController, entry, SchedulingStep.CENTER, onFinishScheduling) {
                LaunchedEffect(route.centerId) {
                    model.prepareCenterFromOrigin(route.centerId)
                }

                LaunchedEffect(state.centerPreparation, route.centerId) {
                    val preparation = state.centerPreparation
                            as? CenterPreparationUiState.Ready
                        ?: return@LaunchedEffect

                    if (
                        preparation.centerId != route.centerId ||
                        state.selectedCenter?.id != route.centerId
                    ) {
                        return@LaunchedEffect
                    }

                    navController.navigate(AppRoute.ScheduleDate) {
                        popUpTo<AppRoute.ScheduleFromCenter> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }

                CenterPreparationScreen(
                    state = state.centerPreparation,
                    onRetry = {
                        model.prepareCenterFromOrigin(route.centerId)
                    },
                    onBack = {
                        navController.navigateBackOrHome()
                    },
                )
            }
        }

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

            SchedulingDraftGate(schedulingViewModel, navController, entry, SchedulingStep.CENTER, onFinishScheduling) {
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

            SchedulingDraftGate(schedulingViewModel, navController, entry, SchedulingStep.DATE, onFinishScheduling) {
                ScheduleDateScreen(
                    uiState = uiState,
                    onSelectDate = schedulingViewModel::selectDate,
                    onSelectTimeSlot = schedulingViewModel::selectTimeSlot,
                    onRetry = schedulingViewModel::loadTimeSlots,
                    onLoadAvailableDates = schedulingViewModel::loadAvailableDates,
                    onBack = {
                        navController.popBackStack()
                    },
                    onContinue = {
                        navController.navigate(if (uiState.rescheduleOrigin != null) AppRoute.ScheduleConfirm else AppRoute.ScheduleRecurrence)
                    },
                )
            }
        }

        composable<AppRoute.ScheduleRecurrence> { entry ->
            val schedulingEntry = remember(entry) { navController.getBackStackEntry<AppGraph.Scheduling>() }
            val schedulingViewModel: SchedulingViewModel = viewModel(
                viewModelStoreOwner = schedulingEntry, factory = schedulingViewModelFactory,
            )
            val schedulingState by schedulingViewModel.uiState.collectAsStateWithLifecycle()
            val routineModel: br.edu.fatec.doesangue.presentation.routines.ReminderRoutineViewModel = viewModel(
                viewModelStoreOwner=schedulingEntry,factory=routineViewModelFactory)
            val routineState by routineModel.state.collectAsStateWithLifecycle()
            LaunchedEffect(routineState.loaded,schedulingState.routine) {
                schedulingState.routine?.let { if (routineState.loaded) routineModel.prefill(it) }
            }
            SchedulingDraftGate(schedulingViewModel, navController, entry, SchedulingStep.RECURRENCE, onFinishScheduling) {
                ScheduleRecurrenceScreen(
                    routineState=routineState,hasSelectedRoutine=schedulingState.routine != null,
                    onDate=routineModel::updateDate,onInterval=routineModel::updateInterval,onRetry=routineModel::load,
                    onChooseRoutine={enabled ->
                        val choice = if (enabled) routineModel.choice(schedulingState.selectedCenter?.timeZoneId ?: "America/Sao_Paulo") ?: return@ScheduleRecurrenceScreen else null
                        schedulingViewModel.selectRoutine(choice)
                        navController.navigate(AppRoute.ScheduleConfirm)
                    },
                    onBack = {
                        navController.popBackStack()
                    },

                )
            }
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
            val appointment = uiState.appointmentCreation.appointment

            SchedulingDraftGate(schedulingViewModel, navController, entry, SchedulingStep.CONFIRM, onFinishScheduling) {
                LaunchedEffect(appointment?.id, appointment?.status) {
                    if (appointment?.status == AppointmentStatus.SCHEDULED) {
                        navController.navigate(AppRoute.ScheduleSuccess) {
                            popUpTo<AppRoute.ScheduleConfirm> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
                ScheduleConfirmScreen(
                    uiState = uiState,
                    onBack = {
                        if (uiState.requestLocked) onFinishScheduling() else navController.popBackStack()
                    },
                    onConfirm = schedulingViewModel::confirmAppointment,
                    onReviewRejected = schedulingViewModel::reviewRejectedRequest,
                    onFinishPrevious = schedulingViewModel::finishDraft,
                )
            }
        }

        composable<AppRoute.ScheduleSuccess> { entry ->
            val schedulingEntry = remember(entry) {
                navController.getBackStackEntry<AppGraph.Scheduling>()
            }

            val schedulingViewModel: SchedulingViewModel = viewModel(
                viewModelStoreOwner = schedulingEntry,
                factory = schedulingViewModelFactory,
            )

            val uiState by schedulingViewModel.uiState.collectAsStateWithLifecycle()

            SchedulingDraftGate(schedulingViewModel, navController, entry, SchedulingStep.CONFIRM, onFinishScheduling) {
                ScheduleSuccessScreen(
                    uiState = uiState,
                    onHome = schedulingViewModel::finishDraft,
                )
            }
        }
    }
}
