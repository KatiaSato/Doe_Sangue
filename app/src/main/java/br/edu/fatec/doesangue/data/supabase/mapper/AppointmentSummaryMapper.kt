package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.AppointmentSummaryDto
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import java.time.OffsetDateTime

fun AppointmentSummaryDto.toDomain(): AppointmentSummary {
    return AppointmentSummary(
        appointment = appointment.toDomain(),
        centerName = centerName,
        startsAt = OffsetDateTime.parse(startsAt).toInstant(),
        timeZoneId = timeZoneId,
    )
}