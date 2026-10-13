package br.edu.fatec.doesangue.ui.screens.donations

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onBack: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    DsScreen(
        title = "Detalhe do agendamento",
        onBack = onBack,
        bottomRoute = AppRoute.DonationsAppointments,
        onNavigate = onNavigate,
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
                    AppointmentStatus.CANCELLED -> "Cancelado"
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
            }
        }
    }
}