package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.BloodStockDto
import br.edu.fatec.doesangue.data.supabase.dto.DonationRecordDto
import br.edu.fatec.doesangue.domain.model.BloodStock
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.DonationRecord
import br.edu.fatec.doesangue.domain.model.StockLevel
import java.time.OffsetDateTime
import java.time.ZoneId

fun BloodStockDto.toDomain(): BloodStock {
    require(synthetic && centerId > 0)
    ZoneId.of(timeZoneId)
    val type = BloodType.fromDatabase(bloodType)
    require(type != BloodType.UNKNOWN)
    return BloodStock(centerId.toString(), centerName, type, StockLevel.fromDatabase(level),
        source, OffsetDateTime.parse(updatedAt).toInstant(), timeZoneId)
}

fun DonationRecordDto.toDomain(): DonationRecord {
    require(synthetic && id > 0)
    ZoneId.of(timeZoneId)
    return DonationRecord(id.toString(), centerName, OffsetDateTime.parse(occurredAt).toInstant(), timeZoneId, source)
}
