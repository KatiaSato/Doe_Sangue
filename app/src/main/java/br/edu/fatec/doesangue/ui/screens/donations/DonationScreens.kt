package br.edu.fatec.doesangue.ui.screens.donations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.model.DonationRecord
import br.edu.fatec.doesangue.presentation.common.ReadUiState
import br.edu.fatec.doesangue.presentation.appointments.AppointmentsUiState
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.InfoCard
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.SectionHeader
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.Ink
import br.edu.fatec.doesangue.ui.theme.InkSecondary
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DonationHistoryScreen(
    state: ReadUiState<List<DonationRecord>>, onRetry: () -> Unit,
    onBack: () -> Unit, onAppointments: () -> Unit, onNavigate: (AppRoute) -> Unit,
) {
    DsScreen(title = "Minhas doações", onBack = onBack, bottomRoute = AppRoute.DonationsHistory, onNavigate = onNavigate) {
        DonationTabs(history = true, onHistory = {}, onAppointments = onAppointments)
        Spacer(Modifier.height(18.dp))
        Text("Histórico de demonstração: registros fictícios publicados pela administração.")
        Text("Agendamentos não são contabilizados como doações.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        when (state) {
            ReadUiState.Loading -> Text("Carregando histórico…")
            is ReadUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                PrimaryButton("Tentar novamente", onRetry)
            }
            is ReadUiState.Loaded -> {
                if (state.data.isEmpty()) Text("Nenhum registro de doação fictícia encontrado.")
                state.data.forEach { record ->
                    InfoCard {
                        Text(record.occurredAt.atZone(ZoneId.of(record.timeZoneId))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm")),
                            style = MaterialTheme.typography.titleMedium)
                        Text(record.centerName)
                        Text("Doação fictícia • Registro ${record.id}", color = BloodRed)
                        Text("Fonte: ${record.source}", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                InfoCard {
                    Text("Total de registros fictícios", style = MaterialTheme.typography.titleMedium)
                    Text(state.data.size.toString(), style = MaterialTheme.typography.headlineMedium, color = BloodRed)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun DonationAppointmentsScreen(
    uiState: AppointmentsUiState,
    onRetry: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onBack: () -> Unit,
    onHistory: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    routineContent: @Composable () -> Unit = {},
) {
    DsScreen(title = "Minhas doações", onBack = onBack, bottomRoute = AppRoute.DonationsAppointments, onNavigate = onNavigate) {
        DonationTabs(history = false, onHistory = onHistory, onAppointments = {})
        Spacer(Modifier.height(22.dp))
        when (val state = uiState) {
            AppointmentsUiState.Loading -> {
                Text("Carregando agendamentos…")
            }

            is AppointmentsUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Tentar novamente", onRetry)
            }

            is AppointmentsUiState.Loaded -> {
                if (state.appointments.isEmpty()) {
                    Text("Você ainda não possui agendamentos.")
                } else {
                    state.appointments.forEach { summary ->
                        val localStart = summary.startsAt.atZone(
                            ZoneId.of(summary.timeZoneId),
                        )
                        val dateTime = localStart.format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm"),
                        )
                        val statusLabel = when (summary.appointment.status) {
                            AppointmentStatus.SCHEDULED -> "Agendado"
                            AppointmentStatus.CANCELLED -> if(summary.rescheduledToId != null) "Remarcado" else "Cancelado"
                        }

                        InfoCard {
                            Text(
                                text = dateTime,
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink,
                            )
                            Text(summary.centerName)
                            Text("Agendamento ${summary.appointment.id}")
                            Text(statusLabel, color = BloodRed)
                            Spacer(Modifier.height(12.dp))
                            PrimaryButton(
                                text = "Ver detalhes",
                                onClick = { onOpenDetails(summary.appointment.id) },
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        routineContent()
    }
}

@Composable
private fun DonationTabs(history: Boolean, onHistory: () -> Unit, onAppointments: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            "Histórico",
            Modifier.weight(1f).clickable(onClick = onHistory).padding(12.dp),
            textAlign = TextAlign.Center,
            color = if (history) BloodRed else InkSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            "Agendamentos",
            Modifier.weight(1f).clickable(onClick = onAppointments).padding(12.dp),
            textAlign = TextAlign.Center,
            color = if (!history) BloodRed else InkSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
