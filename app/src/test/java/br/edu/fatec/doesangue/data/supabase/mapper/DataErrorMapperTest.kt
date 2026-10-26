package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.domain.model.*
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.url
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.*
import io.ktor.utils.io.InternalAPI
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

class DataErrorMapperTest {
    private val client=HttpClient(OkHttp)
    private fun request()=HttpRequestBuilder().apply { url("https://data.example.test/rest/v1/rpc/synthetic") }
    @After fun close() { client.close() }
    @OptIn(InternalAPI::class)
    private fun error(code:String?, detail:JsonElement?=null, status:HttpStatusCode=HttpStatusCode.BadRequest,
                      message:String="Synthetic private detail")=PostgrestRestException(message,null,detail,code,
        HttpClientCall(client,request().build(),HttpResponseData(status,GMTDate(0),Headers.Empty,
            HttpProtocolVersion.HTTP_1_1,ByteReadChannel.Empty,EmptyCoroutineContext)).response)

    @Test fun stableIdentifiersDistinguishBusinessFailuresWithSameSqlstateAndHttp400() {
        val cases=mapOf("session_required" to DataError.SESSION_UNAVAILABLE,"profile_required" to DataError.PROFILE_REQUIRED,
            "slot_unavailable" to DataError.SLOT_UNAVAILABLE,"slot_started" to DataError.SLOT_STARTED,
            "slot_full" to DataError.SLOT_FULL,"already_booked" to DataError.ALREADY_BOOKED,
            "request_conflict" to DataError.REQUEST_CONFLICT,"appointment_unavailable" to DataError.APPOINTMENT_UNAVAILABLE,
            "cancellation_closed" to DataError.CANCELLATION_CLOSED,"invalid_request" to DataError.INVALID_DATA,
            "isolation_unsupported" to DataError.SERVICE_UNAVAILABLE)
        for((id,expected) in cases) {
            val failure=error("P0001",JsonPrimitive("doesangue.$id.v1")).toAppointmentFailure()
            assertEquals(id,expected,failure.reason)
            assertNull(failure.message); assertNull(failure.cause)
        }
        assertEquals(DataError.APPOINTMENT_UNAVAILABLE,
            error("42501",JsonPrimitive("doesangue.appointment_unavailable.v1")).toAppointmentFailure().reason)
    }

    @Test fun identifiersAreScopedExactVersionedAndNeverInferredFromPhraseOrStatus() {
        for(detail in listOf(null,JsonNull,JsonPrimitive("doesangue.slot_full.v2"),JsonPrimitive("slot_full"),
            JsonPrimitive("prefix doesangue.slot_full.v1"),buildJsonObject { put("app_code","slot_full") })) {
            assertEquals(DataError.UNKNOWN,error("P0001",detail,message="Este horário está sem vagas.").toAppointmentFailure().reason)
        }
        val detail=JsonPrimitive("doesangue.slot_full.v1")
        assertEquals(DataError.UNKNOWN,error("P0001",detail).toDataFailure().reason)
        assertEquals(DataError.UNKNOWN,error("23505",detail).toAppointmentFailure().reason)
        assertEquals(DataError.UNKNOWN,error(null,null,HttpStatusCode.Unauthorized).toDataFailure().reason)
        assertEquals(DataError.UNKNOWN,error("future_code",null,HttpStatusCode.ServiceUnavailable,"network error").toDataFailure().reason)
    }

    @Test fun genericPermissionDoesNotPretendThatSessionExpired() {
        assertEquals(DataError.ACCESS_DENIED,error("42501").toAppointmentFailure().reason)
        assertEquals(DataError.SESSION_UNAVAILABLE,error("PGRST301").toDataFailure().reason)
        assertEquals(DataError.SESSION_UNAVAILABLE,SessionRequiredException("https://data.example.test").toDataFailure().reason)
        assertEquals(DataError.SESSION_UNAVAILABLE,error("42501",JsonPrimitive("doesangue.session_required.v1")).toAppointmentFailure().reason)
    }

    @Test fun commonStructuredCodesDistinguishSchemaDataBusyAndUnknown() {
        val cases=mapOf("PGRST202" to DataError.SERVICE_UNAVAILABLE,"42703" to DataError.SERVICE_UNAVAILABLE,
            "42P01" to DataError.SERVICE_UNAVAILABLE,"PGRST003" to DataError.SERVICE_UNAVAILABLE,
            "22023" to DataError.INVALID_DATA,"22P02" to DataError.INVALID_DATA,"23514" to DataError.INVALID_DATA,
            "40P01" to DataError.BUSY,"40001" to DataError.BUSY,"23505" to DataError.UNKNOWN,"P0001" to DataError.UNKNOWN)
        cases.forEach { (code,expected) -> assertEquals(code,expected,error(code).toDataFailure().reason) }
    }

    @Test fun transportTypesAreRecognizedWithoutReadingTheirDescription() {
        for(failure in listOf(IOException("Synthetic offline"),HttpRequestTimeoutException(request()),
            HttpRequestException("Synthetic connection",request()))) assertEquals(DataError.NETWORK,failure.toDataFailure().reason)
        assertEquals(DataError.UNKNOWN,IllegalStateException("network offline slot_full").toDataFailure().reason)
    }

    @Test fun cancellationAndAlreadyTypedFailuresArePreserved() {
        val cancelled=CancellationException("Synthetic cancellation")
        assertSame(cancelled,assertThrows(CancellationException::class.java) { cancelled.toDataFailure() })
        assertSame(cancelled,assertThrows(CancellationException::class.java) { cancelled.toAppointmentFailure() })
        val typed=DataFailure(DataError.SLOT_FULL)
        assertSame(typed,typed.toDataFailure()); assertSame(typed,typed.toAppointmentFailure())
        assertEquals(ProfileError.NETWORK,IOException().toProfileFailure().reason)
        assertEquals(ProfileError.SESSION_UNAVAILABLE,SessionRequiredException("synthetic").toProfileFailure().reason)
    }
}
