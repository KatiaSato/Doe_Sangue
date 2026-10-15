package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DonationRecordDto(
    val id: Long,
    @SerialName("unidade_nome") val centerName: String,
    @SerialName("ocorrido_em") val occurredAt: String,
    @SerialName("fuso_horario") val timeZoneId: String,
    @SerialName("fonte") val source: String,
    @SerialName("sintetico") val synthetic: Boolean,
)
