package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Representa o agendamento recebido da API, inclusive na repetição do pedido.
@Serializable
data class AppointmentDto(

    /*
    id: identificador do agendamento.
    donorId: identificador do perfil do doador.
    timeSlotId: identificador do horário reservado.
    status: texto retornado pelo banco, agendado ou cancelado.
    idempotencyKey: chave que permite reconhecer a repetição do mesmo pedido.
    createdAt: data de criação, recebida como texto.
     */
    val id: Long,

    @SerialName("doador_id")
    val donorId: String,

    @SerialName("horario_id")
    val timeSlotId: Long,

    @SerialName("estado")
    val status: String,

    @SerialName("chave_idempotencia")
    val idempotencyKey: String,

    @SerialName("criado_em")
    val createdAt: String,
)
