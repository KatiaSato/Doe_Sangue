package br.edu.fatec.doesangue.ui.screens.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.presentation.routines.ReminderRoutineUiState
import br.edu.fatec.doesangue.ui.components.*
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.screens.donations.DonationTab
import br.edu.fatec.doesangue.ui.screens.donations.DonationTabs
import br.edu.fatec.doesangue.ui.screens.home.HomeStyle
import br.edu.fatec.doesangue.ui.theme.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReminderOverviewScreen(state: ReminderRoutineUiState, onRetry: () -> Unit, onEdit: () -> Unit,
    onActive: (Boolean) -> Unit, onDelete: () -> Unit, onBack: () -> Unit,
    onTab: (DonationTab) -> Unit, onNavigate: (AppRoute) -> Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    BackHandler { if (!state.saving) onBack() }
    if (confirmDelete && !state.saving) AlertDialog(onDismissRequest = { confirmDelete = false },
        title = { Text("Excluir rotina?") }, text = { Text("Seus agendamentos e registros de doação serão preservados.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Excluir rotina") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Manter rotina") } })
    DsScreen(title = "Minhas doações", onBack = if (state.saving) null else onBack,
        bottomRoute = AppRoute.DonationsReminders, onNavigate = { if (!state.saving) onNavigate(it) }, scrollable = false) {
        DonationTabs(DonationTab.REMINDERS, onTab, enabled = !state.saving)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Seu planejamento, à vista", style = HomeStyle.Hero)
            Text("Organize quando deseja se lembrar de doar.", style = HomeStyle.Supporting, color = InkSecondary)
            when {
                state.loading -> Text("Carregando rotina…")
                state.error != null -> Column {
                    Text(state.error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry, enabled = !state.saving) { Text("Atualizar rotina") }
                }
                state.loaded -> {
                    val routine = state.routine
                    Column(Modifier.fillMaxWidth().background(HomeStyle.Soft, HomeStyle.CardShape).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Image(painterResource(R.drawable.refine_routine_bell), null, Modifier.size(32.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Minha rotina", Modifier.weight(1f), style = HomeStyle.Title)
                            if (routine != null) Text(if (routine.active) "Ativa" else "Pausada", style = HomeStyle.Supporting,
                                color = if (routine.active) HomeStyle.Success else InkSecondary)
                        }
                        if (routine == null) Text("Nenhuma rotina configurada.", style = HomeStyle.Supporting)
                        else {
                            val next = routine.nextDate(LocalDate.now(ZoneId.of(routine.timeZoneId)))
                            Text(next?.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("pt-BR")))
                                ?: if (routine.active) "Sem próxima data" else "Rotina pausada", style = HomeStyle.Greeting)
                            Text("Próxima data do planejamento", style = HomeStyle.Supporting, color = InkSecondary)
                            Column {
                                Text("A cada ${routine.intervalDays} dias", style = HomeStyle.Label)
                                Text("Início em " + routine.startsOn.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                    style = HomeStyle.Supporting, color = InkSecondary)
                            }
                        }
                        PrimaryButton(if (routine == null) "Configurar rotina" else "Editar rotina", onEdit,
                            enabled = !state.saving, shape = HomeStyle.TileShape)
                    }
                    if (routine != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { onActive(!routine.active) }, enabled = !state.saving) {
                            Text(if (routine.active) "Pausar rotina" else "Retomar rotina", style = HomeStyle.Label)
                        }
                        TextButton(onClick = { confirmDelete = true }, enabled = !state.saving) { Text("Excluir", color = InkSecondary) }
                    }
                }
            }
            if (state.saving) Text("Salvando…", style = HomeStyle.Supporting)
            state.message?.let { Text(it, style = HomeStyle.Supporting, color = HomeStyle.Success) }
            Column(Modifier.fillMaxWidth().background(SurfaceSoft, HomeStyle.CardShape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Planejar não é agendar", style = HomeStyle.Label)
                Text("Sua rotina não cria reservas nem define quando você pode doar. Confirme as orientações com o serviço de saúde.",
                    style = HomeStyle.Supporting, color = InkSecondary)
                Text("Notificações no celular: em breve.", style = HomeStyle.Caption, color = InkSecondary)
            }
        }
    }
}
