package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import br.edu.fatec.doesangue.domain.model.SchedulingStep
import br.edu.fatec.doesangue.presentation.scheduling.*
import br.edu.fatec.doesangue.ui.screens.scheduling.DraftRecoveryScreen

/** Executa a retomada antes de montar efeitos de origem ou telas com escolhas antigas. */
@Composable
internal fun SchedulingDraftGate(model: SchedulingViewModel, nav: NavHostController,
    entry: NavBackStackEntry, step: SchedulingStep, onExit: () -> Unit, content: @Composable () -> Unit) {
    val recovery by model.draftRecovery.collectAsStateWithLifecycle()
    val ui by model.uiState.collectAsStateWithLifecycle()
    val current by nav.currentBackStackEntryAsState()
    if (current?.id != entry.id) return
    LaunchedEffect(recovery.resumeStep, entry.id) {
        val target = recovery.resumeStep ?: return@LaunchedEffect
        if (nav.currentBackStackEntry?.id != entry.id) return@LaunchedEffect
        val destination = entry.destination
        val same = when (target) {
            SchedulingStep.CENTER -> destination.hasRoute<AppRoute.ScheduleCenter>() ||
                destination.hasRoute<AppRoute.ScheduleFromCenter>()
            SchedulingStep.DATE -> destination.hasRoute<AppRoute.ScheduleDate>()
            SchedulingStep.RECURRENCE -> destination.hasRoute<AppRoute.ScheduleRecurrence>()
            SchedulingStep.CONFIRM -> destination.hasRoute<AppRoute.ScheduleConfirm>()
        }
        if (!same) {
            // Reconstrói as etapas anteriores para Voltar permitir revisar as escolhas recuperadas.
            val routes = listOf(AppRoute.ScheduleCenter, AppRoute.ScheduleDate,
                AppRoute.ScheduleRecurrence, AppRoute.ScheduleConfirm)
            routes.take(target.ordinal + 1).forEachIndexed { index, route ->
                nav.navigate(route) {
                    if (index == 0) popUpTo<AppGraph.Scheduling> { inclusive = false }
                    launchSingleTop = true
                }
            }
        }
        model.consumeDraftResumeStep()
    }
    LaunchedEffect(recovery.phase, recovery.resumeStep, step, entry.id) {
        if (recovery.phase == DraftRecoveryPhase.READY && recovery.resumeStep == null && nav.currentBackStackEntry?.id == entry.id)
            model.onStepVisible(step)
    }
    LaunchedEffect(ui.draftFinished) { if (ui.draftFinished && nav.currentBackStackEntry?.id == entry.id) onExit() }
    if (recovery.phase != DraftRecoveryPhase.READY || recovery.resumeStep != null) {
        DraftRecoveryScreen(recovery, model::resumeDraft, model::discardDraft, model::retryDraftRecovery, onExit)
    } else Column(Modifier.fillMaxSize()) {
        ui.draftNotice?.let { message -> Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(message, style = MaterialTheme.typography.bodySmall)
                if (ui.draftSaveFailed) TextButton(onClick = model::retrySavingDraft) { Text("Salvar rascunho novamente") }
            }
        } }
        Box(Modifier.weight(1f)) { content() }
    }
}
