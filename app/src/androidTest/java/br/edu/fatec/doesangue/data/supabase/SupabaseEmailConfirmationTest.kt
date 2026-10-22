package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.repository.SupabaseAuthRepository
import br.edu.fatec.doesangue.domain.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.*
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
import org.junit.Test
import org.junit.Assert.*

/** SDK real, clientes em memória e interceptor antes da rede; não envia e-mail. */
class SupabaseEmailConfirmationTest {
    private data class Request(val path: String, val redirect: String?, val body: JsonObject)
    private val requests = mutableListOf<Request>()
    private val clients = mutableListOf<SupabaseClient>()
    private val mainCache = MemoryCodeVerifierCache()
    private lateinit var main: SupabaseClient
    private lateinit var source: SupabaseAuthDataSource
    private var confirmed = true
    private var status = 200
    private var errorCode = "otp_expired"
    private var userId = "00000000-0000-4000-8000-000000000001"
    private fun user() = """{"id":"$userId","aud":"authenticated","role":"authenticated","email":"test@example.com","email_confirmed_at":${if(confirmed) "\"2026-10-22T00:00:00Z\"" else "null"},"created_at":"2026-10-22T00:00:00Z","app_metadata":{},"user_metadata":{}}"""
    private fun client(cache: MemoryCodeVerifierCache = MemoryCodeVerifierCache()) =
        createSupabaseClient("https://confirmation.example.test", "sb_publishable_synthetic") {
            httpEngine = OkHttp.create { config { addInterceptor { chain ->
                val request = chain.request()
                val buffer = Buffer(); request.body?.writeTo(buffer)
                requests += Request(request.url.encodedPath, request.url.queryParameter("redirect_to"),
                    Json.parseToJsonElement(buffer.readUtf8().ifBlank { "{}" }).jsonObject)
                val json = if (status != 200) """{"error_code":"$errorCode","msg":"internal synthetic detail"}"""
                    else when(request.url.encodedPath) {
                        "/auth/v1/resend" -> "{}"
                        "/auth/v1/token" -> """{"access_token":"synthetic-access","refresh_token":"synthetic-refresh","token_type":"bearer","expires_in":3600,"user":${user()}}"""
                        "/auth/v1/user" -> user()
                        else -> error("Unexpected isolated request")
                    }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(status)
                    .message("Synthetic response").body(json.toResponseBody("application/json".toMediaType())).build()
            } } }
            install(Auth) {
                flowType = FlowType.PKCE; sessionManager = MemorySessionManager(); codeVerifierCache = cache
                autoLoadFromStorage = false; autoSaveToStorage = false; alwaysAutoRefresh = false; enableLifecycleCallbacks = false
            }
        }.also { clients += it }
    @Before fun setup() {
        main = client(mainCache)
        source = SupabaseAuthDataSource(main) { client() }
    }
    @After fun close() = runBlocking { clients.forEach { it.close() } }
    @Test fun resendUsesSignupAndDedicatedRedirectWithoutTouchingRecoveryVerifier() = runBlocking {
        main.auth.awaitInitialization(); mainCache.saveCodeVerifier("pending-recovery")
        source.resendEmailConfirmation(" test@example.com ")
        val request = requests.single()
        assertEquals("/auth/v1/resend", request.path)
        assertEquals(SupabaseAuthDataSource.CONFIRMATION_REDIRECT, request.redirect)
        assertEquals("signup", request.body["type"]!!.jsonPrimitive.content)
        assertEquals("test@example.com", request.body["email"]!!.jsonPrimitive.content)
        assertFalse(request.body.containsKey("password")); assertFalse(request.body.containsKey("code_verifier"))
        assertEquals("pending-recovery", mainCache.loadCodeVerifier())
    }
    @Test fun codeValidationPreservesConnectedAccountAndSharedVerifier() = runBlocking {
        source.signIn("test@example.com", "Synthetic123!")
        val originalId = main.auth.currentUserOrNull()!!.id
        mainCache.saveCodeVerifier("pending-verifier")
        userId = "00000000-0000-4000-8000-000000000002"
        requests.clear()
        source.verifyEmailConfirmation(EmailConfirmationCredential.Code("synthetic-code"))
        assertEquals(listOf("/auth/v1/token", "/auth/v1/user"), requests.map { it.path })
        assertEquals("pending-verifier", requests.first().body["code_verifier"]!!.jsonPrimitive.content)
        assertEquals(originalId, main.auth.currentUserOrNull()!!.id)
        assertEquals("pending-verifier", mainCache.loadCodeVerifier())
        assertNull(clients.last().auth.currentUserOrNull())
    }
    @Test fun tokenIsCheckedAgainstServerAndNeverImportedAndUnconfirmedIsRejected() = runBlocking {
        source.verifyEmailConfirmation(EmailConfirmationCredential.AccessToken("synthetic-token"))
        assertEquals("/auth/v1/user", requests.single().path)
        assertNull(main.auth.currentUserOrNull())
        confirmed = false
        val result = SupabaseAuthRepository(source).verifyEmailConfirmation(EmailConfirmationCredential.AccessToken("synthetic-token"))
        assertEquals(AuthError.EMAIL_NOT_CONFIRMED, (result.exceptionOrNull() as AuthFailure).reason)
        assertNull(main.auth.currentUserOrNull())
    }
    @Test fun missingVerifierAndExpiredTokensHaveStructuredFailuresAndDoNotCreateSession() = runBlocking {
        val repo = SupabaseAuthRepository(source)
        val missing = repo.verifyEmailConfirmation(EmailConfirmationCredential.Code("synthetic-code"))
        assertEquals(AuthError.SESSION_UNAVAILABLE, (missing.exceptionOrNull() as AuthFailure).reason)
        assertTrue(requests.isEmpty())
        status = 400
        val expired = repo.verifyEmailConfirmation(EmailConfirmationCredential.AccessToken("synthetic-token"))
        assertEquals(AuthError.SESSION_UNAVAILABLE, (expired.exceptionOrNull() as AuthFailure).reason)
        assertNull(expired.exceptionOrNull()!!.message)
        assertNull(main.auth.currentUserOrNull())
    }
}
