package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.ReminderRoutineDto
import br.edu.fatec.doesangue.domain.model.ReminderRoutine
import br.edu.fatec.doesangue.domain.model.RoutineChoice
import java.time.LocalDate

internal fun ReminderRoutineDto.toDomain(expectedOwner: String): ReminderRoutine {
    check(ownerId == expectedOwner && id > 0 && version > 0)
    val choice = RoutineChoice(LocalDate.parse(startsOn), intervalDays, timeZoneId, version).validated()
    return ReminderRoutine(id.toString(), ownerId, choice.startsOn, intervalDays, timeZoneId, active, version, originId?.toString())
}
