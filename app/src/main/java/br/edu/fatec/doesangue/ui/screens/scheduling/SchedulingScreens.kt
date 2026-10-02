package br.edu.fatec.doesangue.ui.screens.scheduling

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ScheduleDateScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    DsScreen(title = "Agendar doação", onBack = onBack) {
        ScheduleHeader(2)
        Text("Escolha uma data", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text("Setembro 2026", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("D", "S", "T", "Q", "Q", "S", "S").forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = InkSecondary) }
                }
                (1..30).chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        week.forEach { day ->
                            Box(
                                Modifier.size(34.dp).background(if (day == 12) BloodRed else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Text("$day", color = if (day == 12) Color.White else Ink, style = MaterialTheme.typography.bodyMedium) }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.size(34.dp)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("Horários disponíveis", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        listOf("08:00", "09:30", "11:00", "14:00", "15:30", "17:00").chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { time -> SecondaryButton(time, {}, Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Continuar", onContinue)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ScheduleRecurrenceScreen(onBack: () -> Unit, onContinue: () -> Unit) {
    DsScreen(title = "Agendar doação", onBack = onBack) {
        ScheduleHeader(3)
        Text("Quer criar uma rotina?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        SelectionCard("Somente esta doação", "Sem repetição")
        Spacer(Modifier.height(10.dp))
        SelectionCard("A cada 3 meses", "Rotina mais frequente", selected = true)
        Spacer(Modifier.height(10.dp))
        SelectionCard("A cada 6 meses", "Duas vezes ao ano")
        Spacer(Modifier.height(10.dp))
        SelectionCard("A cada 12 meses", "Uma vez ao ano")
        Spacer(Modifier.height(18.dp))
        InfoCard { Text("A frequência pode ser alterada ou cancelada depois.", style = MaterialTheme.typography.bodyMedium, color = InkSecondary) }
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Continuar", onContinue)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ScheduleConfirmScreen(onBack: () -> Unit, onConfirm: () -> Unit) {
    DsScreen(title = "Confirmar agendamento", onBack = onBack) {
        ScheduleHeader(4)
        Text("Revise os dados", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(14.dp))
        InfoCard {
            ReviewRow("Unidade", "Hemocentro Campinas")
            ReviewRow("Data", "12/09/2026")
            ReviewRow("Horário", "14:00")
            ReviewRow("Rotina", "A cada 3 meses")
        }
        Spacer(Modifier.height(22.dp))
        Text("♡  Obrigada por doar!", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = BloodRed, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(80.dp))
        PrimaryButton("Confirmar agendamento", onConfirm)
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
            Text("Rotina: a cada 3 meses", style = MaterialTheme.typography.bodyMedium, color = BloodRed)
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
            Text("Rotina: a cada 3 meses", style = MaterialTheme.typography.bodyMedium, color = BloodRed)
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
        Text("Frequência atual", style = MaterialTheme.typography.labelMedium)
        Text("A cada 3 meses", style = MaterialTheme.typography.headlineMedium, color = BloodRed)
        Spacer(Modifier.height(24.dp))
        SelectionCard("Assim que possível", "Baseada no intervalo aplicável")
        Spacer(Modifier.height(10.dp))
        SelectionCard("A cada 3 meses", "Frequência atual", selected = true)
        Spacer(Modifier.height(10.dp))
        SelectionCard("A cada 6 meses", "Duas vezes ao ano")
        Spacer(Modifier.height(10.dp))
        SelectionCard("A cada 12 meses", "Uma vez ao ano")
        Spacer(Modifier.height(42.dp))
        PrimaryButton("Salvar alterações", onBack)
        TextAction("Encerrar rotina", {}, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun AppointmentCancelledScreen(onNew: () -> Unit, onHome: () -> Unit) {
    DsScreen {
        Spacer(Modifier.height(170.dp))
        CenteredMessage("✓", "Agendamento cancelado", "Sua rotina continua ativa")
        Spacer(Modifier.height(100.dp))
        PrimaryButton("Fazer novo agendamento", onNew)
        TextAction("Voltar ao início", onHome, Modifier.align(Alignment.CenterHorizontally))
    }
}
