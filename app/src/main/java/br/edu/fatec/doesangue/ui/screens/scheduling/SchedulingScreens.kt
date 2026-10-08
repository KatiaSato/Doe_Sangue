package br.edu.fatec.doesangue.ui.screens.scheduling

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.fatec.doesangue.ui.components.DonationDatePickerDialog
import br.edu.fatec.doesangue.ui.components.CenteredMessage
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.InfoCard
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.SecondaryButton
import br.edu.fatec.doesangue.ui.components.SelectionCard
import br.edu.fatec.doesangue.ui.components.StepProgress
import br.edu.fatec.doesangue.ui.components.TextAction
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.BloodRedSoft
import br.edu.fatec.doesangue.ui.theme.Ink
import br.edu.fatec.doesangue.ui.theme.InkSecondary
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingUiState

private val scheduleLabels = listOf("Unidade", "Data e horário", "Recorrência", "Confirmar")

@Composable
private fun ScheduleHeader(current: Int) {
    StepProgress(current, scheduleLabels)
    Spacer(Modifier.height(28.dp))
}

@Composable
fun ScheduleCenterScreen(
    uiState: SchedulingUiState,
    onSelectCenter: (DonationCenter) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    DsScreen(title = "Agendar doação", onBack = onBack, bottomRoute = AppRoute.ScheduleCenter, onNavigate = onNavigate) {
        ScheduleHeader(1)
        Text("Escolha uma unidade", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        // Guarda a mensagem em uma variável local para verificar e usar.
        val errorMessage = uiState.errorMessage

// Exibe o conteúdo correspondente ao estado atual da consulta.
        when {
            uiState.isLoading -> {
                Text("Carregando unidades...")
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = "Tentar novamente",
                    onClick = onRetry,
                )
            }

            uiState.centers.isEmpty() -> {
                Text("Nenhuma unidade disponível no momento.")
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = "Atualizar",
                    onClick = onRetry,
                )
            }

            else -> {
                uiState.centers.forEach { center ->
                    SelectionCard(
                        title = center.name,
                        subtitle = "${center.address}\n${center.city}",
                        selected = uiState.selectedCenter?.id == center.id,
                        onClick = { onSelectCenter(center) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // Permite avançar quando a consulta terminou sem erro
// e a unidade selecionada pertence à lista carregada.
        PrimaryButton(
            text = "Continuar",
            onClick = onContinue,
            enabled = !uiState.isLoading &&
                    uiState.errorMessage == null &&
                    uiState.centers.any { center ->
                        center.id == uiState.selectedCenter?.id
                    },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ScheduleDateScreen(
    uiState: SchedulingUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectTimeSlot: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    // Controla somente se a janela do calendário está aberta.
    // A variável começa com false, então o calendário fica fechado. Quando ela passa para true, o Compose exibe o diálogo.
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        DonationDatePickerDialog(
            selectedDate = uiState.selectedDate,
            onConfirm = onSelectDate,
            onDismiss = { showDatePicker = false },
        )
    }
    DsScreen(title = "Agendar doação", onBack = onBack) {
        ScheduleHeader(2)
        Text(
            text = uiState.selectedCenter?.name ?: "Nenhuma unidade selecionada",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))
        Text("Escolha uma data", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        SecondaryButton(
            text = uiState.selectedDate
                ?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                ?: "Selecionar data",
            onClick = { showDatePicker = true },
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Horários de demonstração",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(10.dp))

        val timeSlotsError = uiState.timeSlotsErrorMessage

        when {
            uiState.selectedCenter == null || uiState.selectedDate == null -> {
                Text("Selecione uma unidade e uma data para consultar os horários.")
            }

            uiState.isLoadingTimeSlots -> {
                Text("Carregando horários...")
            }

            timeSlotsError != null -> {
                Text(
                    text = timeSlotsError,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    text = "Tentar novamente",
                    onClick = onRetry,
                )
            }

            uiState.timeSlots.isEmpty() -> {
                Text("Nenhum horário disponível para esta data.")
            }

            else -> {
                val zone = ZoneId.of(uiState.selectedCenter.timeZoneId)

                uiState.timeSlots.chunked(3).forEach { slotsRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        slotsRow.forEach { slot ->
                            // Apresenta o instante no fuso da unidade.
                            val time = slot.startsAt
                                .atZone(zone)
                                .toLocalTime()

                            FilterChip(
                                selected = uiState.selectedTimeSlotId == slot.id,
                                onClick = { onSelectTimeSlot(slot.id) },
                                label = {
                                    Text(time.format(DateTimeFormatter.ofPattern("HH:mm")))
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = "Continuar",
            onClick = onContinue,
            enabled = uiState.selectedCenter != null &&
                    uiState.selectedDate != null &&
                    uiState.selectedTime != null &&
                    uiState.selectedTimeSlotId != null &&
                    !uiState.isLoadingTimeSlots &&
                    uiState.timeSlotsErrorMessage == null,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ScheduleRecurrenceScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    DsScreen(title = "Agendar doação", onBack = onBack) {
        ScheduleHeader(3)
        Text("Recorrência", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        SelectionCard("Somente este agendamento", "Sem repetição automática", selected = true)
        Spacer(Modifier.height(18.dp))
        InfoCard {
            Text("A recorrência ainda não está disponível. Consulte o hemocentro sobre o intervalo adequado entre doações. Agendar não confirma aptidão clínica.",
                style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Continuar", onContinue)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ScheduleConfirmScreen(
    uiState: SchedulingUiState,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
) {
    DsScreen(title = "Confirmar agendamento", onBack = onBack) {
        ScheduleHeader(4)
        Text("Revise os dados", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        InfoCard {
            ReviewRow(
                "Unidade",
                uiState.selectedCenter?.name ?: "Nenhuma unidade selecionada",
            )
            ReviewRow(
                "Data",
                uiState.selectedDate
                    ?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    ?: "Nenhuma data selecionada",
            )
            ReviewRow(
                "Horário",
                uiState.selectedTime
                    ?.format(DateTimeFormatter.ofPattern("HH:mm"))
                    ?: "Nenhum horário selecionado",
            )
            ReviewRow("Recorrência", "Sem repetição automática")
        }
        Spacer(Modifier.height(22.dp))
        Text("♡  Obrigada por doar!", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = BloodRed, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(80.dp))
        PrimaryButton(
            text = "Confirmar agendamento",
            onClick = onConfirm,
            enabled = uiState.selectedCenter != null &&
                    uiState.selectedDate != null &&
                    uiState.selectedTime != null &&
                    uiState.selectedTimeSlotId != null,
        )
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        Text(value, style = MaterialTheme.typography.labelMedium, color = Ink)
    }
}

@Composable
fun ScheduleSuccessScreen(onHome: () -> Unit) {
    DsScreen {
        Spacer(Modifier.height(110.dp))
        CenteredMessage("✓", "Agendamento confirmado!", "Sua doação está marcada para\n12/09/2026 às 14:00")
        Spacer(Modifier.height(34.dp))
        InfoCard {
            Text("Hemocentro Campinas", style = MaterialTheme.typography.titleMedium)
            Text("Rua Carlos Chagas, 480", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            Text("Sem repetição automática", style = MaterialTheme.typography.bodyMedium, color = BloodRed)
        }
        Spacer(Modifier.height(18.dp))
        Text("Vamos lembrar você antes da doação ♡", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = InkSecondary)
        Spacer(Modifier.height(56.dp))
        PrimaryButton("Ir para o início", onHome)
    }
}

@Composable
fun AppointmentDetailScreen(onBack: () -> Unit, onChange: () -> Unit, onEditRoutine: () -> Unit, onCancel: () -> Unit, onNavigate: (AppRoute) -> Unit) {
    DsScreen(title = "Detalhe do agendamento", onBack = onBack, bottomRoute = AppRoute.DonationsAppointments, onNavigate = onNavigate) {
        Spacer(Modifier.height(20.dp))
        AppointmentCard()
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Alterar agendamento", onChange)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Editar rotina", onEditRoutine)
        TextAction("Cancelar agendamento", onCancel, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(22.dp))
        InfoCard { Text("A aptidão final será confirmada na triagem.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary) }
    }
}

@Composable
private fun AppointmentCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BloodRedSoft),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("12 SETEMBRO 2026", style = MaterialTheme.typography.titleMedium, color = BloodRed)
            Text("14:00", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Hemocentro Campinas", style = MaterialTheme.typography.titleMedium)
            Text("Rua Carlos Chagas, 480", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            Spacer(Modifier.height(10.dp))
            Text("Sem repetição automática", style = MaterialTheme.typography.bodyMedium, color = BloodRed)
            Text("Lembrete ativo ✓", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        }
    }
}

@Composable
fun CancelAppointmentScreen(onBack: () -> Unit, onCancel: () -> Unit, onNavigate: (AppRoute) -> Unit) {
    DsScreen(title = "Meu agendamento", onBack = onBack, bottomRoute = AppRoute.DonationsAppointments, onNavigate = onNavigate) {
        Spacer(Modifier.height(28.dp))
        AppointmentCard()
        Spacer(Modifier.height(52.dp))
        Text("Deseja cancelar este agendamento?", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Manter agendamento", onBack)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Cancelar agendamento", onCancel)
    }
}

@Composable
fun EditRoutineScreen(onBack: () -> Unit) {
    DsScreen(title = "Editar rotina", onBack = onBack) {
        Spacer(Modifier.height(18.dp))
        Text("Sem repetição automática", style = MaterialTheme.typography.titleLarge, color = BloodRed)
        Spacer(Modifier.height(24.dp))
        InfoCard {
            Text("A recorrência ainda não está disponível. O intervalo entre doações depende dos critérios aplicáveis e da avaliação do hemocentro.",
                style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        }
        Spacer(Modifier.height(42.dp))
        PrimaryButton("Voltar", onBack)
    }
}

@Composable
fun AppointmentCancelledScreen(onNew: () -> Unit, onHome: () -> Unit) {
    DsScreen {
        Spacer(Modifier.height(170.dp))
        CenteredMessage("✓", "Agendamento cancelado", "Nenhum agendamento futuro foi criado automaticamente")
        Spacer(Modifier.height(100.dp))
        PrimaryButton("Fazer novo agendamento", onNew)
        TextAction("Voltar ao início", onHome, Modifier.align(Alignment.CenterHorizontally))
    }
}
