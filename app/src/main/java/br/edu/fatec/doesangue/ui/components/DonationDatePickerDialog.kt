package br.edu.fatec.doesangue.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.validation.SchedulingDateInput
import br.edu.fatec.doesangue.presentation.scheduling.CalendarAvailabilityUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationDatePickerDialog(
    selectedDate: LocalDate?,
    today: LocalDate,
    availability: CalendarAvailabilityUiState,
    onMonthChanged: (YearMonth) -> Unit,
    onRetry: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var manualMode by rememberSaveable { mutableStateOf(false) }
    var manualText by rememberSaveable { mutableStateOf("") }
    val manualDate = SchedulingDateInput.parse(manualText)
    val selectableDates = remember(availability, today) {
        object : SelectableDates {
            override fun isSelectableYear(year: Int) = year >= today.year
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return !availability.isLoading && availability.errorMessage == null &&
                    YearMonth.from(date) == availability.month && !date.isBefore(today) && date in availability.dates
            }
        }
    }
    val initialDate = selectedDate ?: today
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
        initialDisplayedMonthMillis = initialDate.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = selectableDates,
    )

    val currentMonthChanged by rememberUpdatedState(onMonthChanged)
    LaunchedEffect(datePickerState, manualMode) {
        if (!manualMode) {
            snapshotFlow { datePickerState.displayedMonthMillis }.distinctUntilChanged().collect { millis ->
                currentMonthChanged(YearMonth.from(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC)))
            }
        }
    }
    val manualError = when {
        manualText.isBlank() -> null
        manualDate == null -> "Informe uma data válida, como 12/8/2027 ou 12082027."
        manualDate.isBefore(today) -> "Esta data já passou. Escolha uma data a partir de hoje."
        manualDate.year !in datePickerState.yearRange -> "Escolha uma data até 31/12/${datePickerState.yearRange.last}."
        else -> null
    }
    LaunchedEffect(manualMode, manualDate, manualError) {
        if (manualMode && manualDate != null && manualError == null) {
            val month = YearMonth.from(manualDate)
            if (month != availability.month) currentMonthChanged(month)
        }
    }

    // Guarda a escolha temporária feita dentro do calendário.
    val selectedMillis = if (manualMode) manualDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        else datePickerState.selectedDateMillis
    val selectionAllowed = selectedMillis?.let(selectableDates::isSelectableDate) == true
        && (!manualMode || manualError == null)
    val requestedMonth = if (manualMode) manualDate?.takeIf { manualError == null }?.let(YearMonth::from)
        else YearMonth.from(Instant.ofEpochMilli(datePickerState.displayedMonthMillis).atZone(ZoneOffset.UTC))
    val matchingMonth = requestedMonth != null && requestedMonth == availability.month
    val allowed = selectionAllowed && matchingMonth

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = allowed,
                onClick = {
                    if (allowed) {
                        // Converte a escolha do calendário para LocalDate.
                        val date = Instant
                            .ofEpochMilli(selectedMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()

                        onConfirm(date)
                        onDismiss()
                    }
                },
            ) {
                Text("Confirmar data")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    ) {
        // Let the grid keep its natural height; the dialog's actions stay outside this scroll.
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
                TextButton(onClick = {
                    if (manualMode) {
                        if (manualDate != null && manualError == null) {
                            datePickerState.displayedMonthMillis = manualDate.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                            datePickerState.selectedDateMillis = selectedMillis.takeIf { allowed }
                        }
                    } else {
                        datePickerState.selectedDateMillis?.let { millis ->
                            manualText = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        }
                    }
                    manualMode = !manualMode
                }) { Text(if (manualMode) "Ver calendário" else "Digitar data") }
                if (manualMode) {
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it.take(32) },
                        label = { Text("Data do agendamento") },
                        placeholder = { Text("12/8/2027 ou 12082027") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrectEnabled = false),
                        isError = manualError != null,
                        supportingText = { Text(manualError ?: manualDate?.let {
                            "Data reconhecida: ${it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
                        } ?: "Informe o dia, o mês e o ano.") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                when {
                    manualMode && (manualDate == null || manualError != null) -> Unit
                    !matchingMonth || availability.isLoading -> Text("Consultando dias disponíveis…")
                    availability.errorMessage != null -> {
                        Text(availability.errorMessage, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Tentar novamente") }
                    }
                    manualMode && manualDate !in availability.dates -> {
                        Text("Não há horários com vagas disponíveis nesta data.")
                        TextButton(onClick = { currentMonthChanged(requestedMonth) }) { Text("Atualizar dias") }
                    }
                    availability.dates.isEmpty() -> {
                        Text("Nenhum dia com vagas neste mês.")
                        TextButton(onClick = onRetry) { Text("Atualizar dias") }
                    }
                    else -> Text("Selecione um dia com vagas disponíveis.", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (!manualMode) DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}
