package br.edu.fatec.doesangue.presentation.scheduling

import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.model.DonationTimeSlot
import java.time.LocalDate
import java.time.LocalTime
/*
 * Representa o estado do preenchimento do agendamento.
 * Guarda as unidades carregadas, a unidade selecionada,
 * a data, o horário, o carregamento e uma possível falha.
 *
 * As telas observam esse estado para decidir o que exibir.
 */
data class SchedulingUiState(
    val centers: List<DonationCenter> = emptyList(),
    val selectedCenter: DonationCenter? = null,
    // Guarda as escolhas enquanto a pessoa preenche o agendamento.
    val selectedDate: LocalDate? = null,
    val selectedTime: LocalTime? = null,

    // Guarda os horários retornados para a unidade e a data escolhidas.
    val timeSlots: List<DonationTimeSlot> = emptyList(),

// Indica o andamento e uma possível falha da consulta de horários.
    val isLoadingTimeSlots: Boolean = false,
    val timeSlotsErrorMessage: String? = null,

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)