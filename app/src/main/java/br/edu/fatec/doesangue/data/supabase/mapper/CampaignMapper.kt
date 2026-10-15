package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.CampaignDto
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.Campaign
import java.time.OffsetDateTime

fun CampaignDto.toDomain(): Campaign {
    val types = bloodTypes.map(BloodType::fromDatabase)
    require(BloodType.UNKNOWN !in types) { "Campanha não pode anunciar tipo desconhecido." }
    return Campaign(
        id = id.toString(), centerId = centerId.toString(), title = title,
        description = description, startsAt = OffsetDateTime.parse(startsAt).toInstant(),
        endsAt = OffsetDateTime.parse(endsAt).toInstant(), source = source,
        centerName = centerName, timeZoneId = timeZoneId, bloodTypes = types,
    )
}
