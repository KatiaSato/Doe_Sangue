package br.edu.fatec.doesangue.data.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ReminderRoutineDto(
    val id: Long,
    @SerialName("doador_id") val ownerId: String,
    @SerialName("inicio") val startsOn: String,
    @SerialName("intervalo_dias") val intervalDays: Int,
    @SerialName("fuso_horario") val timeZoneId: String,
    @SerialName("ativa") val active: Boolean,
    @SerialName("versao") val version: Long,
    @SerialName("agendamento_origem_id") val originId: Long? = null,
)
