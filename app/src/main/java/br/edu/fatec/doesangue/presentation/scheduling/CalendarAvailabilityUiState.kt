package br.edu.fatec.doesangue.presentation.scheduling

import java.time.LocalDate
import java.time.YearMonth

data class CalendarAvailabilityUiState(
    val month: YearMonth? = null,
    val dates: Set<LocalDate> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
