package br.edu.fatec.doesangue.ui.screens.donations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.presentation.appointments.AppointmentDetailUiState
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.InfoCard
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRed
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AppointmentDetailByIdScreen(
    uiState: AppointmentDetailUiState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onReschedule: () -> Unit = {},
    onOpenReplacement: (String) -> Unit = {},
) {
    var showCancelConfirmation by rememberSaveable {
        mutableStateOf(false)
    }

    val loadedState = uiState as? AppointmentDetailUiState.Loaded
    val isCancelling = loadedState?.isCancelling == true

    BackHandler(enabled = isCancelling) {}

    if (
        showCancelConfirmation &&
        loadedState != null &&
        !loadedState.isCancelling &&
        loadedState.appointment.appointment.status == AppointmentStatus.SCHEDULED
    ) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmation = false },
            title = { Text("Cancelar agendamento?") },
            text = { Text("Deseja cancelar este agendamento?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelConfirmation = false
                        onCancel()
                    },
                ) {
                    Text("Sim, cancelar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCancelConfirmation = false },
                ) {
                    Text("Manter agendamento")
                }
            },
        )
    }

    DsScreen(
        title = "Detalhe do agendamento",
        onBack = if (isCancelling) null else onBack,
        bottomRoute = AppRoute.DonationsAppointments,
        onNavigate = { route ->
            if (!isCancelling) onNavigate(route)
        },
    ) {
        when (val state = uiState) {
            AppointmentDetailUiState.Loading -> {
                Text("Carregando agendamento…")
            }

            AppointmentDetailUiState.NotFound -> {
                Text("Agendamento não encontrado ou indisponível.")
            }

            is AppointmentDetailUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Tentar novamente", onRetry)
            }

            is AppointmentDetailUiState.Loaded -> {
                val summary = state.appointment
                val localStart = summary.startsAt.atZone(
                    ZoneId.of(summary.timeZoneId),
                )
                val statusLabel = when (summary.appointment.status) {
                    AppointmentStatus.SCHEDULED -> "Agendado"
                    AppointmentStatus.CANCELLED -> if(summary.rescheduledToId != null) "Remarcado" else "Cancelado"
                }

                InfoCard {
                    Text(
                        text = "Agendamento ${summary.appointment.id}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = summary.centerName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        localStart.format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm"),
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(statusLabel, color = BloodRed)
                }

                summary.rescheduledToId?.let { replacement ->
                    Spacer(Modifier.height(12.dp))
                    br.edu.fatec.doesangue.ui.components.SecondaryButton("Ver novo agendamento $replacement",{onOpenReplacement(replacement)})
                }
                summary.rescheduledFromId?.let { Text("Remarcado a partir do agendamento $it.",style=MaterialTheme.typography.bodySmall) }
                if (summary.appointment.status == AppointmentStatus.SCHEDULED && summary.startsAt > java.time.Instant.now()) {
                    Spacer(Modifier.height(16.dp))
                    br.edu.fatec.doesangue.ui.components.SecondaryButton("Remarcar agendamento",onReschedule,enabled=!state.isCancelling)
                }
                //O botão abre a confirmação e fica desabilitado durante o envio. Após a resposta Cancelado, ele desaparece
                state.cancellationError?.let { message ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (summary.appointment.status == AppointmentStatus.SCHEDULED) {
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = if (state.isCancelling) {
                            "Cancelando…"
                        } else {
                            "Cancelar agendamento"
                        },
                        onClick = { showCancelConfirmation = true },
                        enabled = !state.isCancelling,
                    )
                }
            }
        }
    }
}
