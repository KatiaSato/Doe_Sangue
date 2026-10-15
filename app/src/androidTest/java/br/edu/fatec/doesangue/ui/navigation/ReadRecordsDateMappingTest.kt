package br.edu.fatec.doesangue.ui.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.data.supabase.dto.BloodStockDto
import br.edu.fatec.doesangue.data.supabase.dto.DonationRecordDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Executa no Android: o suporte do parser de datas pode diferir do JDK local. */
@RunWith(AndroidJUnit4::class)
class ReadRecordsDateMappingTest {
    @Test fun stockAndHistoryPreserveOffsetsAndFractionalSecondsOnAndroid() {
        val samples = listOf(
            "2026-10-15T12:00:00Z" to "2026-10-15T12:00:00Z",
            "2026-10-15T12:00:00+00:00" to "2026-10-15T12:00:00Z",
            "2026-10-15T09:00:00-03:00" to "2026-10-15T12:00:00Z",
            "2026-10-15T15:00:00+03:00" to "2026-10-15T12:00:00Z",
            "2026-10-15T12:00:00.123456+00:00" to "2026-10-15T12:00:00.123456Z",
        )
        samples.forEach { (received, utc) ->
            val stock = BloodStockDto(1, "Unidade sintética", "O-", "critico", "Teste sintético",
                received, "America/Sao_Paulo", true).toDomain()
            val history = DonationRecordDto(1, "Unidade sintética", received,
                "America/Sao_Paulo", "Teste sintético", true).toDomain()
            assertEquals("Estoque: $received", Instant.parse(utc), stock.updatedAt)
            assertEquals("Histórico: $received", Instant.parse(utc), history.occurredAt)
        }
    }
}
