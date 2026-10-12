package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.AppointmentDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.Appointment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Chama a função de reserva; identidade e validação de capacidade ficam no servidor.
class SupabaseAppointmentDataSource(
    private val client: SupabaseClient,
) {
    suspend fun createAppointment(
        timeSlotId: String,
        idempotencyKey: String,
    ): Appointment {
        val slotId = timeSlotId.toLong()
        require(slotId > 0) { "Informe um horário válido." }
        require(idempotencyKey.isNotBlank()) { "Informe a chave do pedido." }

        client.auth.awaitInitialization()
        checkNotNull(client.auth.currentUserOrNull()) {
            "É necessário entrar para agendar."
        }

        // A chave recebida é preservada para recuperar o mesmo pedido nas tentativas.
        val parameters = buildJsonObject {
            put("p_horario_id", slotId)
            put("p_chave_idempotencia", idempotencyKey)
        }

        val appointments = client.postgrest
            .rpc("criar_agendamento", parameters)
            .decodeList<AppointmentDto>()

        check(appointments.size == 1) {
            "A reserva deve retornar exatamente um agendamento."
        }
        return appointments.single().toDomain()
    }
}
