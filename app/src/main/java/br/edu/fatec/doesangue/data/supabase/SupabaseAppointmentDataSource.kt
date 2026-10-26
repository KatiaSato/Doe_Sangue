package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.domain.model.DataFailure

import br.edu.fatec.doesangue.domain.model.DataError

import br.edu.fatec.doesangue.data.supabase.dto.AppointmentDto
import br.edu.fatec.doesangue.data.supabase.dto.AppointmentSummaryDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.api.authenticatedSupabaseApi
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Chama a função de reserva; identidade e validação de capacidade ficam no servidor.
class SupabaseAppointmentDataSource(
    private val client: SupabaseClient,
) {
    @OptIn(SupabaseInternal::class)
    suspend fun createAppointment(
        timeSlotId: String,
        idempotencyKey: String,
        expectedOwnerId: String? = null,
    ): Appointment {
        val slotId = timeSlotId.toLongOrNull()?.takeIf { it > 0 }
            ?: throw DataFailure(DataError.INVALID_DATA)
        if (idempotencyKey.isBlank()) throw DataFailure(DataError.INVALID_DATA)

        val session = client.requireDataSession()
        if (expectedOwnerId != null && session.user?.id != expectedOwnerId) throw DataFailure(DataError.SESSION_UNAVAILABLE)

        // A chave recebida é preservada para recuperar o mesmo pedido nas tentativas.
        val parameters = buildJsonObject {
            put("p_horario_id", slotId)
            put("p_chave_idempotencia", idempotencyKey)
        }

        val result = if (expectedOwnerId == null) client.postgrest.rpc("criar_agendamento", parameters)
        else {
            // A credencial capturada com o ID fica só em memória durante esta chamada.
            val api = client.authenticatedSupabaseApi(client.postgrest, requireSession = true) {
                jwtToken = session.accessToken
            }
            val response = api.postJson("rpc/criar_agendamento", parameters.toString()) {
                headers["Content-Profile"] = client.postgrest.config.defaultSchema
            }
            PostgrestResult(response.bodyAsText(), response.headers, client.postgrest)
        }
        val appointments = result.decodeList<AppointmentDto>()

        check(appointments.size == 1) {
            "A reserva deve retornar exatamente um agendamento."
        }
        return appointments.single().toDomain()
    }

    suspend fun getMyAppointments(): List<AppointmentSummary> {
        client.requireDataUser()

        return client.postgrest
            .rpc("consultar_meus_agendamentos", buildJsonObject {})
            .decodeList<AppointmentSummaryDto>()
            .map { it.toDomain() }
    }

    /*
    Esse metodo envia somente o ID da reserva à função do banco.
    O servidor identifica o doador e verifica o prazo e a autorização
     */
    suspend fun cancelAppointment(
        appointmentId: String,
    ): Appointment {
        val reservationId = appointmentId.toLongOrNull()?.takeIf { it > 0 }
            ?: throw DataFailure(DataError.INVALID_DATA)

        client.requireDataUser()

        val parameters = buildJsonObject {
            put("p_agendamento_id", reservationId)
        }

        val appointments = client.postgrest
            .rpc("cancelar_agendamento", parameters)
            .decodeList<AppointmentDto>()

        check(appointments.size == 1) {
            "O cancelamento deve retornar exatamente um agendamento."
        }

        return appointments.single().toDomain()
    }
}
