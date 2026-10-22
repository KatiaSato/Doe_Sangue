package br.edu.fatec.doesangue.data.supabase

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.data.repository.SupabaseDonorProfileRepository
import br.edu.fatec.doesangue.domain.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import java.time.LocalDate
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

/** Auth/PostgREST reais, com interceptor antes da rede e sessão apenas em memória. */
@RunWith(AndroidJUnit4::class)
class SupabaseProfileEditTest {
    private lateinit var client: SupabaseClient
    private lateinit var repository: SupabaseDonorProfileRepository
    private val id = "00000000-0000-4000-8000-000000000001"
    private var profileRequests = 0
    private var code: String? = null
    private var emptyResponse = false
    private var captured: JsonObject? = null
    private var ownFilter: String? = null
    private var syntheticSession = false

    @Before fun prepare() {
        client = createSupabaseClient("https://profile.example.test", "sb_publishable_synthetic") {
            install(Auth) {
                sessionManager = MemorySessionManager()
                autoLoadFromStorage = false; autoSaveToStorage = false
                alwaysAutoRefresh = false; enableLifecycleCallbacks = false
            }
            install(Postgrest)
            httpEngine = OkHttp.create {
                config {
                    addInterceptor { chain ->
                        val request = chain.request()
                        val body: String
                        val status: Int
                        when (request.url.encodedPath) {
                            "/auth/v1/token" -> {
                                body = """{"access_token":"synthetic-access","refresh_token":"synthetic-refresh","token_type":"bearer","expires_in":3600,"user":{"id":"$id","aud":"authenticated","role":"authenticated","created_at":"2026-10-22T00:00:00Z","app_metadata":{},"user_metadata":{}}}"""
                                status = 200
                            }
                            "/rest/v1/perfil_doador" -> {
                                profileRequests++
                                assertTrue(request.method in listOf("PATCH", "POST"))
                                ownFilter = request.url.queryParameter("id")
                                syntheticSession = request.header("Authorization") == "Bearer synthetic-access"
                                val buffer = Buffer(); request.body!!.writeTo(buffer)
                                val payload = Json.parseToJsonElement(buffer.readUtf8())
                                // O SDK representa INSERT como lista e UPDATE como objeto.
                                captured = if (payload is JsonArray) payload.single().jsonObject else payload.jsonObject
                                status = if (code == null) 200 else 400
                                body = if (code != null) """{"code":"$code","message":"Synthetic internal detail","details":null,"hint":null}"""
                                else if (emptyResponse) "[]" else "[${JsonObject(captured!! + ("id" to JsonPrimitive(id)))}]"
                            }
                            else -> error("Pedido inesperado no teste de perfil.")
                        }
                        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(status)
                            .message("Synthetic response").body(body.toResponseBody("application/json".toMediaType())).build()
                    }
                }
            }
        }
        repository = SupabaseDonorProfileRepository(SupabaseDonorProfileDataSource(client))
    }
    @After fun close() = runBlocking { client.close() }
    private suspend fun login() = SupabaseAuthDataSource(client).signIn("doador@example.com", "SyntheticPassword1!")

    @Test fun completeUpdateUsesOwnIdentityOneRequestAndExplicitNullToClear() = runBlocking {
        login()
        val first = repository.updateCurrentProfile("  Doador sintético  ", BloodType.AB_NEGATIVE,
            "(00) 90000-0000", LocalDate.of(2000, 2, 29)).getOrThrow()
        assertEquals(id, first.id); assertEquals("Doador sintético", first.displayName)
        assertEquals("00900000000", first.phone); assertEquals(LocalDate.of(2000, 2, 29), first.birthDate)
        assertEquals("eq.$id", ownFilter); assertTrue(syntheticSession)
        assertEquals(setOf("nome_exibicao", "tipo_sanguineo", "telefone", "data_nascimento"), captured!!.keys)
        assertEquals(1, profileRequests)
        val cleared = repository.updateCurrentProfile("Doador sintético", BloodType.UNKNOWN, null, null).getOrThrow()
        assertNull(cleared.phone); assertNull(cleared.birthDate)
        assertEquals(JsonNull, captured!!["telefone"]); assertEquals(JsonNull, captured!!["data_nascimento"])
    }

    @Test fun creatingMissingProfileDoesNotSendOptionalColumnsWithoutInsertPrivilege() = runBlocking {
        login()
        val profile = repository.createCurrentProfile(" Doador sintético ").getOrThrow()
        assertEquals("Doador sintético", profile.displayName)
        assertEquals(setOf("id", "nome_exibicao"), captured!!.keys)
        assertNull(profile.phone); assertNull(profile.birthDate)
    }

    @Test fun absentSessionInvalidInputAndMissingProfileDoNotInventSuccess() = runBlocking {
        val noSession = repository.updateCurrentProfile("Doador", BloodType.UNKNOWN, null, null)
        assertEquals(ProfileError.SESSION_UNAVAILABLE, (noSession.exceptionOrNull() as ProfileFailure).reason)
        assertEquals(0, profileRequests)
        login()
        val invalid = repository.updateCurrentProfile("Doador", BloodType.UNKNOWN, "abc", null)
        assertEquals(ProfileError.INVALID_DATA, (invalid.exceptionOrNull() as ProfileFailure).reason)
        assertEquals(0, profileRequests)
        emptyResponse = true
        val missing = repository.updateCurrentProfile("Doador", BloodType.UNKNOWN, null, null)
        assertEquals(ProfileError.MISSING, (missing.exceptionOrNull() as ProfileFailure).reason)
    }

    @Test fun structuredCodesPreserveKnownCausesWithoutExposingServerDetails() = runBlocking {
        login()
        for ((value, expected) in listOf("42501" to ProfileError.ACCESS_DENIED,
            "23514" to ProfileError.INVALID_DATA, "PGRST204" to ProfileError.SCHEMA_NOT_READY,
            "42703" to ProfileError.SCHEMA_NOT_READY, "PGRST301" to ProfileError.SESSION_UNAVAILABLE,
            "future_code" to ProfileError.UNKNOWN)) {
            code = value
            val result = repository.updateCurrentProfile("Doador", BloodType.UNKNOWN, null, null)
            assertEquals(value, expected, (result.exceptionOrNull() as ProfileFailure).reason)
            assertNull(result.exceptionOrNull()?.message)
        }
    }
}
