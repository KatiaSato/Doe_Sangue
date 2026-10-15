package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CampaignDto(
    val id: Long,
    @SerialName("unidade_id") val centerId: Long,
    @SerialName("titulo") val title: String,
    @SerialName("descricao") val description: String,
    @SerialName("inicio") val startsAt: String,
    @SerialName("fim") val endsAt: String,
    @SerialName("fonte") val source: String,
    @SerialName("unidade_nome") val centerName: String,
    @SerialName("fuso_horario") val timeZoneId: String,
    @SerialName("tipos_sanguineos") val bloodTypes: List<String> = emptyList(),
)
