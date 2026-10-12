package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.AppointmentDto
import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import java.time.OffsetDateTime

// Preserva o estado retornado pelo servidor; um cancelado não vira reserva ativa.
fun AppointmentDto.toDomain(): Appointment {
    return Appointment(
        id = id.toString(),
        donorId = donorId,
        timeSlotId = timeSlotId.toString(),
        status = when (status) {
            "agendado" -> AppointmentStatus.SCHEDULED
            "cancelado" -> AppointmentStatus.CANCELLED
            else -> throw IllegalArgumentException("Estado de agendamento desconhecido.")
        },
        createdAt = OffsetDateTime.parse(createdAt).toInstant(),
    )
}
