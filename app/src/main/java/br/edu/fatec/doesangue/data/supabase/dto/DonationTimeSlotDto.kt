package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Representa um horário recebido da API do Supabase.
// @Serializable permite transformar o JSON recebido em um objeto Kotlin
// @SerialName relaciona o nome da coluna ao nome da propriedade
@Serializable
data class DonationTimeSlotDto(
    val id: Long,

    @SerialName("unidade_id")
    val centerId: Long,

    @SerialName("inicio")
    val startsAt: String,

    @SerialName("capacidade")
    val capacity: Int,

    @SerialName("ativo")
    val active: Boolean,
)