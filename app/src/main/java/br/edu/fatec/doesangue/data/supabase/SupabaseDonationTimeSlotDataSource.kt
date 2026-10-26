package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.DonationTimeSlotDto
import br.edu.fatec.doesangue.data.supabase.dto.AvailableDonationDateDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.DonationTimeSlot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

// Consulta os horários de uma unidade dentro de um período.
class SupabaseDonationTimeSlotDataSource(
    private val client: SupabaseClient,
) {
    suspend fun getAvailableDates(centerId: String, month: YearMonth): Set<LocalDate> =
        client.postgrest.rpc("consultar_dias_disponiveis", buildJsonObject {
            put("p_unidade_id", centerId.toLong())
            put("p_mes", month.atDay(1).toString())
        }).decodeList<AvailableDonationDateDto>().map { LocalDate.parse(it.date) }.toSet()

    suspend fun getTimeSlots(
        centerId: String,
        fromInclusive: Instant,
        untilExclusive: Instant,
    ): List<DonationTimeSlot> {
        val slotsDto = client.postgrest.rpc("consultar_horarios_disponiveis", buildJsonObject {
            put("p_unidade_id", centerId.toLong())
            put("p_inicio", fromInclusive.toString())
            put("p_fim", untilExclusive.toString())
        }).decodeList<DonationTimeSlotDto>()

        return slotsDto.map { dto ->
            dto.toDomain()
        }
    }
}
