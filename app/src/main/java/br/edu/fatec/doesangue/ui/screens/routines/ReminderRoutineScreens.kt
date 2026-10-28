package br.edu.fatec.doesangue.ui.screens.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.presentation.routines.ReminderRoutineUiState
import br.edu.fatec.doesangue.ui.components.*
import br.edu.fatec.doesangue.domain.validation.RoutineDateInput
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayDate = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun RoutineSummaryCard(state: ReminderRoutineUiState,onRetry: () -> Unit,onEdit: () -> Unit) {
    InfoCard {
        Text("Minha rotina de lembretes", style = MaterialTheme.typography.titleMedium)
        when {
            state.loading -> Text("Carregando rotina…")
            state.error != null -> { Text(state.error,color = MaterialTheme.colorScheme.error); TextButton(onClick=onRetry) { Text("Atualizar rotina") } }
            state.routine == null -> Text("Nenhuma rotina configurada.")
            else -> {
                val routine = state.routine
                Text(if (routine.active) "Ativa • a cada ${routine.intervalDays} dias" else "Pausada • a cada ${routine.intervalDays} dias")
                routine.nextDate(LocalDate.now(ZoneId.of(routine.timeZoneId)))?.let { Text("Próxima data planejada: ${it.format(displayDate)}") }
            }
        }
        Text("Preferência pessoal. Não confirma aptidão nem reserva uma vaga.",style=MaterialTheme.typography.bodySmall)
        TextButton(onClick=onEdit) { Text(if (state.routine == null) "Configurar rotina" else "Editar rotina") }
    }
}

@Composable
fun RoutineFields(state: ReminderRoutineUiState,onDate: (String)->Unit,onInterval: (String)->Unit) {
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var calendarOpen by rememberSaveable { mutableStateOf(false) }
    if (state.loading) Text("Carregando rotina…")
    if (state.loaded) {
        val candidates = remember(state.dateText) { RoutineDateInput.candidates(state.dateText) }
        if (calendarOpen && !state.saving) {
            RoutineDatePickerDialog(
                selectedDate = candidates.singleOrNull(),
                today = LocalDate.now(ZoneId.of(state.timeZoneId)),
                retainedDate = state.routine?.startsOn,
                onConfirm = { onDate(it.format(displayDate)) },
                onDismiss = { calendarOpen = false },
            )
        }
        val dateHint = when {
            candidates.size > 1 -> "Esta data tem duas interpretações. Escolha uma das opções."
            candidates.size == 1 -> "Data reconhecida: ${candidates.single().format(displayDate)}"
            else -> "Digite dia, mês e ano, com ou sem barras. Ex.: 1/11/2026 ou 01112026."
        }
        OutlinedTextField(value=state.dateText,onValueChange=onDate,
            label={Text("Data inicial")},placeholder={Text("DD/MM/AAAA")},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,
            enabled=!state.saving,isError=state.dateError!=null || candidates.size > 1,
            supportingText={Text(state.dateError ?: dateHint)},modifier=Modifier.fillMaxWidth())
        TextButton(onClick = {
            keyboard?.hide(); focus.clearFocus(); calendarOpen = true
        }, enabled = !state.saving) { Text("Escolher no calendário") }
        if (candidates.size > 1) {
            candidates.forEach { date ->
                TextButton(onClick = { onDate(date.format(displayDate)) }, enabled = !state.saving) {
                    Text("Usar ${date.format(displayDate)}")
                }
            }
        }
        OutlinedTextField(value=state.intervalText,onValueChange=onInterval,label={Text("Intervalo em dias")},
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,
            enabled=!state.saving,isError=state.intervalError!=null,
            supportingText={Text(state.intervalError ?: "Escolha um intervalo pessoal entre 1 e 3650 dias.")},modifier=Modifier.fillMaxWidth())
    }
    state.error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
    if (state.routine?.active == false) Text("Sua rotina está pausada. Salvar os dados mantém a pausa.",style=MaterialTheme.typography.bodySmall)
    Text("Esta rotina não cria agendamentos. O envio de notificações ainda não está disponível.",style=MaterialTheme.typography.bodySmall)
    Text("Consulte o hemocentro sobre quando poderá doar novamente.",style=MaterialTheme.typography.bodySmall)
}

@Composable
fun ReminderRoutineScreen(state: ReminderRoutineUiState,onDate: (String)->Unit,onInterval: (String)->Unit,
    onSave: ()->Unit,onActive: (Boolean)->Unit,onDelete: ()->Unit,onReload: ()->Unit,onBack: ()->Unit) {
    val focus=androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled=state.saving) {}
    if (confirmDelete && !state.saving) AlertDialog(onDismissRequest={confirmDelete=false},
        title={Text("Excluir rotina?")},text={Text("Seus agendamentos e registros de doação serão preservados.")},
        confirmButton={TextButton(onClick={confirmDelete=false;onDelete()}) {Text("Excluir rotina")}},
        dismissButton={TextButton(onClick={confirmDelete=false}) {Text("Manter rotina")}})
    DsScreen(title="Minha rotina",onBack=if(state.saving)null else onBack) {
        Text(if(state.routine==null) "Planeje seus lembretes" else if(state.routine.active) "Rotina ativa" else "Rotina pausada",style=MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        RoutineFields(state,onDate,onInterval)
        state.message?.let { Text(it,color=MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(if(state.saving) "Salvando…" else "Salvar rotina",{keyboard?.hide();focus.clearFocus();onSave()},enabled=state.loaded && !state.saving)
        state.routine?.let { routine ->
            SecondaryButton(if(routine.active) "Pausar rotina" else "Retomar rotina",{onActive(!routine.active)},enabled=!state.saving)
            TextButton(onClick={confirmDelete=true},enabled=!state.saving) {Text("Excluir rotina")}
        }
        TextButton(onClick=onReload,enabled=!state.saving) {Text("Recarregar rotina")}
    }
}
