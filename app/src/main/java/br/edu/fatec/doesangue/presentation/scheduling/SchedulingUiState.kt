package br.edu.fatec.doesangue.presentation.scheduling

import br.edu.fatec.doesangue.domain.model.DonationCenter

/*
 * Representa o estado da tela de seleção de unidade.
 * Guarda as unidades carregadas, a escolha da pessoa,
 * o andamento da consulta e uma possível mensagem de erro.
 *
 * A tela observa esse estado para decidir o que exibir.
 * Os valores iniciais representam uma consulta ainda não iniciada.
 */
data class SchedulingUiState(
    val centers: List<DonationCenter> = emptyList(),
    val selectedCenter: DonationCenter? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)