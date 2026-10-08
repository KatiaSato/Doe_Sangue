package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Representa os dados de uma unidade recebidos do Supabase.
// @Serializable permite que a biblioteca transforme os dados JSON recebidos em um objeto Kotlin
// @SerialName("nome") relaciona o campo nome do JSON à propriedade name no Kotlin.
@Serializable
data class DonationCenterDto(
    val id: Long,

    @SerialName("nome")
    val name: String,

    @SerialName("cidade")
    val city: String,

    @SerialName("endereco")
    val address: String,

    @SerialName("horario_atendimento")
    val openingHours: String? = null,

    @SerialName("telefone")
    val phone: String? = null,

    @SerialName("fuso_horario")
    val timeZoneId: String,

    @SerialName("publicada")
    val published: Boolean,
)
