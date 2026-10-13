package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.AppointmentSummaryDto
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Verifica o contrato JSON da leitura, sem acessar rede ou autenticação. */
class AppointmentSummaryMapperTest {
    private val response = """
        [{
            "agendamento": {
                "id": 4294967296,
                "doador_id": "00000000-0000-4000-8000-000000000001",
                "horario_id": 2147483648,
                "estado": "agendado",
                "chave_idempotencia": "00000000-0000-4000-8000-000000000002",
                "criado_em": "2026-10-12T23:14:35.586383+00:00"
            },
            "nome_unidade": "Unidade Sintética",
            "inicio": "2026-10-19T08:00:00-03:00",
            "fuso_horario": "America/Sao_Paulo"
        }]
    """.trimIndent()

    private fun decode(json: String) =
        Json.decodeFromString<List<AppointmentSummaryDto>>(json).single().toDomain()

    @Test
    fun decodesNestedResponsePreservingBigintIdsOffsetsAndPrecision() {
        val summary = decode(response)

        assertEquals("4294967296", summary.appointment.id)
        assertEquals("2147483648", summary.appointment.timeSlotId)
        assertEquals("00000000-0000-4000-8000-000000000001", summary.appointment.donorId)
        assertEquals(AppointmentStatus.SCHEDULED, summary.appointment.status)
        assertEquals(Instant.parse("2026-10-12T23:14:35.586383Z"), summary.appointment.createdAt)
        assertEquals("Unidade Sintética", summary.centerName)
        assertEquals(Instant.parse("2026-10-19T11:00:00Z"), summary.startsAt)
        assertEquals("America/Sao_Paulo", summary.timeZoneId)
    }

    @Test
    fun cancelledResponseKeepsCancelledStatus() {
        val summary = decode(response.replace("\"agendado\"", "\"cancelado\""))

        assertEquals(AppointmentStatus.CANCELLED, summary.appointment.status)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownStatusFailsInsteadOfBecomingScheduled() {
        decode(response.replace("\"agendado\"", "\"estado_desconhecido\""))
    }
}
