package br.edu.fatec.doesangue.data.supabase

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.data.repository.SupabaseDonationTimeSlotRepository
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
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
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** PostgREST real com resposta interceptada antes da rede; nenhum dado/cliente real. */
@RunWith(AndroidJUnit4::class)
class SupabaseAvailabilityTest {
    private lateinit var client: SupabaseClient
    private lateinit var repository: SupabaseDonationTimeSlotRepository
    private val requests = mutableListOf<Pair<String, JsonObject>>()
    private var status = 200
    private var body = "[]"
    @Before fun prepare() {
        client = createSupabaseClient("https://availability.example.test", "sb_publishable_synthetic") {
            install(Postgrest)
            httpEngine = OkHttp.create { config { addInterceptor { chain ->
                val request = chain.request()
                assertEquals("POST", request.method)
                assertTrue(request.url.encodedPath in listOf("/rest/v1/rpc/consultar_horarios_disponiveis", "/rest/v1/rpc/consultar_dias_disponiveis"))
                val buffer = Buffer(); request.body!!.writeTo(buffer)
                requests += request.url.encodedPath to Json.parseToJsonElement(buffer.readUtf8()).jsonObject
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(status).message("Synthetic response")
                    .body(body.toResponseBody("application/json".toMediaType())).build()
            } } }
        }
        repository = SupabaseDonationTimeSlotRepository(SupabaseDonationTimeSlotDataSource(client))
    }
    @After fun close() = runBlocking { client.close() }

    @Test fun slotRpcSendsOnlyUnitAndBoundsAndMapsAvailability() = runBlocking {
        body = """[{"id":9,"unidade_id":1,"inicio":"2026-11-20T08:00:00-03:00","capacidade":3,"vagas_disponiveis":1}]"""
        val start = Instant.parse("2026-11-20T03:00:00Z"); val end = start.plusSeconds(86_400)
        val slot = repository.getTimeSlots("1", start, end).getOrThrow().single()
        assertEquals(1, slot.availableSpots); assertEquals(Instant.parse("2026-11-20T11:00:00Z"), slot.startsAt)
        assertEquals(setOf("p_unidade_id", "p_inicio", "p_fim"), requests.single().second.keys)
        assertEquals(JsonPrimitive(1), requests.single().second["p_unidade_id"])
        assertEquals(JsonPrimitive(start.toString()), requests.single().second["p_inicio"])
        assertEquals(JsonPrimitive(end.toString()), requests.single().second["p_fim"])
    }

    @Test fun calendarRpcSendsFirstOfMonthAndDeduplicatesDates() = runBlocking {
        body = """[{"dia":"2026-11-20"},{"dia":"2026-11-20"},{"dia":"2026-11-21"}]"""
        assertEquals(setOf(LocalDate.of(2026,11,20), LocalDate.of(2026,11,21)),
            repository.getAvailableDates("1", YearMonth.of(2026,11)).getOrThrow())
        assertEquals(setOf("p_unidade_id", "p_mes"), requests.single().second.keys)
        assertEquals(JsonPrimitive("2026-11-01"), requests.single().second["p_mes"])
        body = "[]"
        assertTrue(repository.getAvailableDates("1", YearMonth.of(2026,12)).getOrThrow().isEmpty())
    }

    @Test fun absentMigrationFailsWithoutFallingBackToRawCapacityTable() = runBlocking {
        status = 404; body = """{"code":"PGRST202","message":"Synthetic missing function","details":null,"hint":null}"""
        assertTrue(repository.getTimeSlots("1", Instant.parse("2026-11-20T03:00:00Z"), Instant.parse("2026-11-21T03:00:00Z")).isFailure)
        assertEquals(1, requests.size)
        assertTrue(repository.getAvailableDates("1", YearMonth.of(2026,11)).isFailure)
        assertEquals(2, requests.size)
    }

    @Test fun malformedAvailabilityIsNotInvented() = runBlocking {
        for (extra in listOf("", ",\"vagas_disponiveis\":4")) {
            body = """[{"id":9,"unidade_id":1,"inicio":"2026-11-20T11:00:00Z","capacidade":3$extra}]"""
            assertTrue(repository.getTimeSlots("1", Instant.parse("2026-11-20T03:00:00Z"), Instant.parse("2026-11-21T03:00:00Z")).isFailure)
        }
    }
}
