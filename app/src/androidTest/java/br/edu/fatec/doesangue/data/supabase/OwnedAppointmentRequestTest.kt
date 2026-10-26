@file:OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.repository.SupabaseAppointmentRepository
import br.edu.fatec.doesangue.domain.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.status.*
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** Identidade e tokens fictícios; nenhuma comunicação fora do interceptor. */
class OwnedAppointmentRequestTest {
    private lateinit var client: SupabaseClient
    private val a = "00000000-0000-4000-8000-000000000001"
    private val b = "00000000-0000-4000-8000-000000000002"
    private val key = "00000000-0000-4000-8000-000000000099"
    private val requests = mutableListOf<Pair<String?,String>>()
    private var reject = false
    private var changeDuringTransport = false
    @Before fun setup() {
        client = createSupabaseClient("https://owner.example.test", "sb_publishable_synthetic") {
            install(Auth) {
                sessionManager = MemorySessionManager(); codeVerifierCache = MemoryCodeVerifierCache()
                autoLoadFromStorage = false; autoSaveToStorage = false; alwaysAutoRefresh = false; enableLifecycleCallbacks = false
            }
            install(Postgrest)
            httpEngine = OkHttp.create { config { addInterceptor { chain ->
                val request = chain.request(); val auth = request.url.encodedPath.startsWith("/auth/")
                val body = if (auth) """{"access_token":"synthetic-access-a","refresh_token":"synthetic-refresh","token_type":"bearer","expires_in":3600,"user":{"id":"$a","aud":"authenticated","role":"authenticated","created_at":"2026-10-26T00:00:00Z","app_metadata":{},"user_metadata":{}}}"""
                else {
                    assertEquals("/rest/v1/rpc/criar_agendamento", request.url.encodedPath)
                    val buffer = Buffer(); request.body?.writeTo(buffer)
                    requests += request.header("Authorization") to buffer.readUtf8()
                    assertEquals("public", request.header("Content-Profile"))
                    if (changeDuringTransport) {
                        val session = checkNotNull(client.auth.currentSessionOrNull())
                        client.auth.setSessionStatus(SessionStatus.Authenticated(session.copy(accessToken = "synthetic-access-b", user = session.user!!.copy(id = b))))
                    }
                    if (reject) """{"code":"P0001","message":"Synthetic detail","details":"doesangue.slot_full.v1","hint":null}"""
                    else """[{"id":91,"doador_id":"$a","horario_id":7,"estado":"agendado","chave_idempotencia":"$key","criado_em":"2026-10-26T12:00:00Z"}]"""
                }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(if (!auth && reject) 400 else 200)
                    .message("Synthetic response").body(body.toResponseBody("application/json".toMediaType())).build()
            } } }
        }
    }
    @After fun close() = runBlocking { client.close() }
    private suspend fun login() { client.auth.signInWith(Email) { email = "synthetic@example.test"; password = "Synthetic-only-1!" } }
    private fun repo() = SupabaseAppointmentRepository(SupabaseAppointmentDataSource(client))
    @Test fun requestUsesCapturedIdentityAndFollowingCallCannotUseTheNewAccount() = runBlocking<Unit> {
        login(); changeDuringTransport = true
        val result = repo().createAppointmentForOwner(a, "7", key).getOrThrow()
        assertEquals(a, result.donorId)
        assertEquals("Bearer synthetic-access-a", requests.single().first)
        assertEquals(b, client.auth.currentUserOrNull()!!.id)
        assertEquals(DataError.SESSION_UNAVAILABLE,
            (repo().createAppointmentForOwner(a, "7", key).exceptionOrNull() as DataFailure).reason)
        assertEquals(1, requests.size)
    }
    @Test fun missingWrongOrRetryingIdentityStopsBeforeRest() = runBlocking<Unit> {
        assertTrue(repo().createAppointmentForOwner(a, "7", key).isFailure)
        login()
        assertTrue(repo().createAppointmentForOwner(b, "7", key).isFailure)
        client.auth.setSessionStatus(SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IOException("synthetic"))))
        assertEquals(DataError.SESSION_RETRYING,
            (repo().createAppointmentForOwner(a, "7", key).exceptionOrNull() as DataFailure).reason)
        assertTrue(requests.isEmpty())
    }
    @Test fun boundPathPreservesStructuredSqlErrorAndRequestKeyAcrossRetries() = runBlocking<Unit> {
        login(); reject = true
        repeat(2) {
            assertEquals(DataError.SLOT_FULL,
                (repo().createAppointmentForOwner(a, "7", key).exceptionOrNull() as DataFailure).reason)
        }
        assertEquals(listOf(JsonPrimitive(key), JsonPrimitive(key)), requests.map {
            Json.parseToJsonElement(it.second).jsonObject["p_chave_idempotencia"]
        })
    }
}
