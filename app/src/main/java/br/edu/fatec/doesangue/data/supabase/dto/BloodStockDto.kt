package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BloodStockDto(
    @SerialName("unidade_id") val centerId: Long,
    @SerialName("unidade_nome") val centerName: String,
    @SerialName("tipo_sanguineo") val bloodType: String,
    @SerialName("nivel") val level: String,
    @SerialName("fonte") val source: String,
    @SerialName("atualizado_em") val updatedAt: String,
    @SerialName("fuso_horario") val timeZoneId: String,
    @SerialName("sintetico") val synthetic: Boolean,
)
