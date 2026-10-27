package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppointmentSummaryDto(
    @SerialName("agendamento")
    val appointment: AppointmentDto,

    @SerialName("nome_unidade")
    val centerName: String,

    @SerialName("inicio")
    val startsAt: String,

    @SerialName("fuso_horario")
    val timeZoneId: String,
    @SerialName("unidade_id") val centerId: Long? = null,
    @SerialName("remarcado_para_id") val rescheduledToId: Long? = null,
    @SerialName("remarcado_de_id") val rescheduledFromId: Long? = null,
)
