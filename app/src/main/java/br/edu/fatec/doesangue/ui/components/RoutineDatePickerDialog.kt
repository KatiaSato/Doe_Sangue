package br.edu.fatec.doesangue.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Planejamento pessoal: não consulta ofertas nem cria reservas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDatePickerDialog(
    selectedDate: LocalDate?,
    today: LocalDate,
    retainedDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val selectable = remember(today, retainedDate) {
        object : SelectableDates {
            override fun isSelectableYear(year: Int) = year >= today.year || year == retainedDate?.year
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return date >= today || date == retainedDate
            }
        }
    }
    val initial = selectedDate?.takeIf { it >= today || it == retainedDate }
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        initialDisplayedMonthMillis = (initial ?: today).withDayOfMonth(1)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        yearRange = 1900..9999,
        selectableDates = selectable,
    )
    val millis = picker.selectedDateMillis
    val allowed = millis != null && selectable.isSelectableDate(millis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = allowed, onClick = {
                if (allowed) {
                    onConfirm(Instant.ofEpochMilli(checkNotNull(millis)).atZone(ZoneOffset.UTC).toLocalDate())
                    onDismiss()
                }
            }) { Text("Usar data") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    ) {
        DatePicker(state = picker, showModeToggle = false)
    }
}
