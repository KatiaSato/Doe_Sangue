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
import br.edu.fatec.doesangue.ui.screens.profile.BirthDateMask
import br.edu.fatec.doesangue.ui.screens.profile.birthDateDigits
import br.edu.fatec.doesangue.ui.screens.profile.formatBirthDateInput
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
    if (state.loading) Text("Carregando rotina…")
    if (state.loaded) {
        OutlinedTextField(value=birthDateDigits(state.dateText),onValueChange={onDate(formatBirthDateInput(it))},
            label={Text("Data inicial")},placeholder={Text("DD/MM/AAAA")},visualTransformation=BirthDateMask,
            keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,
            enabled=!state.saving,isError=state.dateError!=null,
            supportingText={state.dateError?.let {Text(it)}},modifier=Modifier.fillMaxWidth())
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
