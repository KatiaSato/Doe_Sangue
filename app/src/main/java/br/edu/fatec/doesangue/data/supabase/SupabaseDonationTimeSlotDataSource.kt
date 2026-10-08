package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.DonationTimeSlotDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.DonationTimeSlot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant

// Consulta os horários de uma unidade dentro de um período.
class SupabaseDonationTimeSlotDataSource(
    private val client: SupabaseClient,
) {
    suspend fun getTimeSlots(
        centerId: String,
        fromInclusive: Instant,
        untilExclusive: Instant,
    ): List<DonationTimeSlot> {
        val slotsDto = client
            .from("horario_disponivel")
            .select {
                filter {
                    eq("unidade_id", centerId.toLong())

                    // Agrupa os dois limites para que ambos sejam enviados à API.
                    and {
                        gte("inicio", fromInclusive.toString())
                        lt("inicio", untilExclusive.toString())
                    }
                }

                order("inicio", Order.ASCENDING)                       // Horarios do mais cedo para o mais tarde
            }
            .decodeList<DonationTimeSlotDto>()

        return slotsDto.map { dto ->
            dto.toDomain()
        }
    }
}