package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.DonationTimeSlotDto
import br.edu.fatec.doesangue.domain.model.DonationTimeSlot
import java.time.OffsetDateTime

// Converte o horário recebido da API para o modelo do aplicativo.
fun DonationTimeSlotDto.toDomain(): DonationTimeSlot {
    return DonationTimeSlot(
        id = id.toString(),
        centerId = centerId.toString(),
        startsAt = OffsetDateTime.parse(startsAt).toInstant(),  // Lê uma data/hora com deslocamento UTC e transforma em um Instante, um momento preciso no tempo
        capacity = capacity,
    )
}