package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.BloodStockDto
import br.edu.fatec.doesangue.data.supabase.dto.DonationRecordDto
import br.edu.fatec.doesangue.domain.model.StockLevel
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReadRecordsMapperTest {
    private val stock = """{"unidade_id":4294967296,"unidade_nome":"Demo","tipo_sanguineo":"O-",
        "nivel":"critico","fonte":"Teste","atualizado_em":"2026-10-15T09:00:00-03:00",
        "fuso_horario":"America/Sao_Paulo","sintetico":true}"""
    private val record = """{"id":4294967297,"unidade_nome":"Demo","ocorrido_em":"2026-05-10T10:00:00-03:00",
        "fuso_horario":"America/Sao_Paulo","fonte":"Teste","sintetico":true}"""

    @Test fun stockPreservesBigintTypeLevelAndInstant() {
        val domain = Json.decodeFromString<BloodStockDto>(stock).toDomain()
        assertEquals("4294967296", domain.centerId)
        assertEquals(StockLevel.CRITICAL, domain.level)
        assertEquals("O-", domain.bloodType.label)
        assertEquals(Instant.parse("2026-10-15T12:00:00Z"), domain.updatedAt)
    }
    @Test fun historyPreservesBigintAndOffset() {
        val domain = Json.decodeFromString<DonationRecordDto>(record).toDomain()
        assertEquals("4294967297", domain.id)
        assertEquals(Instant.parse("2026-05-10T13:00:00Z"), domain.occurredAt)
    }
    @Test(expected = IllegalArgumentException::class) fun unknownBloodTypeCannotBecomeStock() {
        Json.decodeFromString<BloodStockDto>(stock.replace("O-", "NAO_SEI")).toDomain()
    }
    @Test(expected = IllegalArgumentException::class) fun unknownLevelCannotBecomeStable() {
        Json.decodeFromString<BloodStockDto>(stock.replace("critico", "invalido")).toDomain()
    }
    @Test(expected = IllegalArgumentException::class) fun nonSyntheticHistoryIsRejected() {
        Json.decodeFromString<DonationRecordDto>(record.replace("true", "false")).toDomain()
    }
    @Test(expected = IllegalArgumentException::class) fun nonSyntheticStockIsRejected() {
        Json.decodeFromString<BloodStockDto>(stock.replace("true", "false")).toDomain()
    }
}
