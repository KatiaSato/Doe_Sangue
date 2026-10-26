package br.edu.fatec.doesangue.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.presentation.scheduling.CalendarAvailabilityUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

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
    LaunchedEffect(datePickerState) {
        snapshotFlow { datePickerState.displayedMonthMillis }.distinctUntilChanged().collect { millis ->
            currentMonthChanged(YearMonth.from(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC)))
        }
    }

    // Guarda a escolha temporária feita dentro do calendário.
    val selectedMillis = datePickerState.selectedDateMillis
    val allowed = selectedMillis?.let(selectableDates::isSelectableDate) == true

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
        Column {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp)) {
                when {
                    availability.month == null || availability.isLoading -> Text("Consultando dias disponíveis…")
                    availability.errorMessage != null -> {
                        Text(availability.errorMessage, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) { Text("Tentar novamente") }
                    }
                    availability.dates.isEmpty() -> {
                        Text("Nenhum dia com vagas neste mês.")
                        TextButton(onClick = onRetry) { Text("Atualizar dias") }
                    }
                    else -> Text("Selecione um dia com vagas disponíveis.", style = MaterialTheme.typography.bodySmall)
                }
            }
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }
}
