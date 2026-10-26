package br.edu.fatec.doesangue.data.supabase

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.data.repository.*
import br.edu.fatec.doesangue.domain.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.*

/** SDK/Repositories reais, transporte interceptado antes da rede e sessão somente em memória. */
@RunWith(AndroidJUnit4::class)
class SupabaseDataFailuresTest {
    private lateinit var client:SupabaseClient
    private val user="00000000-0000-4000-8000-000000000001"
    private val key="00000000-0000-4000-8000-000000000099"
    private var code="P0001"
    private var detail:String?="doesangue.slot_full.v1"
    private var offline=false
    private val requests=mutableListOf<Pair<String,String>>()
    @Before fun prepare() {
        client=createSupabaseClient("https://data.example.test","sb_publishable_synthetic") {
            install(Auth) {
                sessionManager=MemorySessionManager(); autoLoadFromStorage=false; autoSaveToStorage=false
                alwaysAutoRefresh=false; enableLifecycleCallbacks=false
            }
            install(Postgrest)
            httpEngine=OkHttp.create { config { addInterceptor { chain ->
                val request=chain.request()
                val auth=request.url.encodedPath=="/auth/v1/token"
                val payload=if(auth) """{"access_token":"synthetic-access","refresh_token":"synthetic-refresh","token_type":"bearer","expires_in":3600,"user":{"id":"$user","aud":"authenticated","role":"authenticated","created_at":"2026-10-26T00:00:00Z","app_metadata":{},"user_metadata":{}}}"""
                    else {
                        if(offline) throw IOException("Synthetic transport failure")
                        val buffer=Buffer(); request.body?.writeTo(buffer)
                        requests+=request.url.encodedPath to buffer.readUtf8()
                        buildJsonObject {
                            put("code",code); put("message","Synthetic private SQL or profile data")
                            put("details",detail?.let(::JsonPrimitive)?:JsonNull); put("hint",JsonNull)
                        }.toString()
                    }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(if(auth)200 else 400)
                    .message("Synthetic response").body(payload.toResponseBody("application/json".toMediaType())).build()
            } } }
        }
    }
    @After fun close()=runBlocking { client.close() }
    private suspend fun login() { client.auth.signInWith(Email) { email="synthetic@example.test"; password="Synthetic-only-1!" } }
    private fun appointments()=SupabaseAppointmentRepository(SupabaseAppointmentDataSource(client))

    @Test fun creationCancellationAndRetryKeepStructuredDetailAndSameKey()=runBlocking<Unit> {
        login()
        val repository=appointments()
        assertEquals(DataError.SLOT_FULL,(repository.createAppointment("7",key).exceptionOrNull() as DataFailure).reason)
        detail="doesangue.slot_started.v1"
        assertEquals(DataError.SLOT_STARTED,(repository.createAppointment("7",key).exceptionOrNull() as DataFailure).reason)
        val keys=requests.map { Json.parseToJsonElement(it.second).jsonObject["p_chave_idempotencia"] }
        assertEquals(listOf(JsonPrimitive(key),JsonPrimitive(key)),keys)
        detail="doesangue.cancellation_closed.v1"
        assertEquals(DataError.CANCELLATION_CLOSED,(repository.cancelAppointment("9").exceptionOrNull() as DataFailure).reason)
        code="42501"; detail="doesangue.appointment_unavailable.v1"
        assertEquals(DataError.APPOINTMENT_UNAVAILABLE,(repository.cancelAppointment("10").exceptionOrNull() as DataFailure).reason)
        detail=null
        assertEquals(DataError.ACCESS_DENIED,(repository.cancelAppointment("10").exceptionOrNull() as DataFailure).reason)
    }

    @Test fun missingSessionAndInvalidIdsStopBeforeAnyRestRequest()=runBlocking<Unit> {
        val repository=appointments()
        assertEquals(DataError.SESSION_UNAVAILABLE,(repository.createAppointment("7",key).exceptionOrNull() as DataFailure).reason)
        assertEquals(DataError.SESSION_UNAVAILABLE,(repository.getMyAppointments().exceptionOrNull() as DataFailure).reason)
        assertEquals(DataError.SESSION_UNAVAILABLE,(repository.cancelAppointment("9").exceptionOrNull() as DataFailure).reason)
        for(id in listOf("","bad","0","-1","99999999999999999999999999")) {
            assertEquals(DataError.INVALID_DATA,(repository.createAppointment(id,key).exceptionOrNull() as DataFailure).reason)
            assertEquals(DataError.INVALID_DATA,(repository.cancelAppointment(id).exceptionOrNull() as DataFailure).reason)
        }
        assertTrue(requests.isEmpty())
    }

    @Test fun allPublicAndPrivateReadsPreserveCommonCategories()=runBlocking<Unit> {
        login()
        val slots=SupabaseDonationTimeSlotRepository(SupabaseDonationTimeSlotDataSource(client))
        val readers=listOf<suspend () -> Result<*>>(
            { SupabaseDonationCenterRepository(SupabaseDonationCenterDataSource(client)).getCenters() },
            { slots.getTimeSlots("1",Instant.parse("2026-11-20T00:00:00Z"),Instant.parse("2026-11-21T00:00:00Z")) },
            { slots.getAvailableDates("1",YearMonth.of(2026,11)) },
            { SupabaseCampaignRepository(SupabaseCampaignDataSource(client)).getCampaigns(null) },
            { SupabaseBloodStockRepository(SupabaseBloodStockDataSource(client)).getBloodStocks() },
            { SupabaseDonationHistoryRepository(SupabaseDonationHistoryDataSource(client)).getMyDonations() },
            { appointments().getMyAppointments() })
        for((value,expected) in listOf("PGRST301" to DataError.SESSION_UNAVAILABLE,"42501" to DataError.ACCESS_DENIED,
            "PGRST202" to DataError.SERVICE_UNAVAILABLE,"future_code" to DataError.UNKNOWN)) {
            code=value; detail=null
            for(read in readers) assertEquals(value,expected,(read().exceptionOrNull() as DataFailure).reason)
        }
        offline=true
        assertEquals(DataError.NETWORK,(slots.getAvailableDates("1",YearMonth.of(2026,11)).exceptionOrNull() as DataFailure).reason)
    }

    @Test fun allProfileOperationsUseExistingTypedContract()=runBlocking<Unit> {
        val repository=SupabaseDonorProfileRepository(SupabaseDonorProfileDataSource(client))
        assertEquals(ProfileError.SESSION_UNAVAILABLE,(repository.updateCurrentBloodType(BloodType.UNKNOWN).exceptionOrNull() as ProfileFailure).reason)
        assertEquals(ProfileError.SESSION_UNAVAILABLE,(repository.createCurrentProfile("Doador Sintético").exceptionOrNull() as ProfileFailure).reason)
        login(); code="42501"; detail=null
        assertEquals(ProfileError.ACCESS_DENIED,(repository.getCurrentProfile().exceptionOrNull() as ProfileFailure).reason)
        assertEquals(ProfileError.ACCESS_DENIED,(repository.updateCurrentBloodType(BloodType.UNKNOWN).exceptionOrNull() as ProfileFailure).reason)
        assertEquals(ProfileError.ACCESS_DENIED,(repository.createCurrentProfile("Doador Sintético").exceptionOrNull() as ProfileFailure).reason)
    }
}
