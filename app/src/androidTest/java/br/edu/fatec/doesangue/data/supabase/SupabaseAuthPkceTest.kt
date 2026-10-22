package br.edu.fatec.doesangue.data.supabase

import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.data.repository.SupabaseAuthRepository
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.model.BloodType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.okhttp.OkHttp
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

/** SDK e DataSource reais; interceptor responde antes da rede e storage só em memória. */
@RunWith(AndroidJUnit4::class)
class SupabaseAuthPkceTest {
    private data class Captured(
        val path: String,
        val redirect: String?,
        val grant: String?,
        val body: JsonObject,
        val method: String,
        val usesSyntheticSession: Boolean,
    )
    private val captured = CopyOnWriteArrayList<Captured>()
    private val verifierCache = MemoryCodeVerifierCache()
    private lateinit var client: SupabaseClient
    private lateinit var source: SupabaseAuthDataSource
    private var tokenStatus = 200
    private var tokenResponse: String? = null
    private var userStatus = 200
    private var userResponse: String? = null
    private val syntheticId = "00000000-0000-4000-8000-000000000001"
    private val userJson get() = """{"id":"$syntheticId","aud":"authenticated","role":"authenticated","email":"cadastro@example.com","created_at":"2026-10-21T00:00:00Z","app_metadata":{},"user_metadata":{}}"""

    @Before fun createIsolatedClient() {
        client = createSupabaseClient("https://auth.example.test", "sb_publishable_synthetic") {
            httpEngine = OkHttp.create {
                config {
                    addInterceptor { chain ->
                        val request = chain.request()
                        val buffer = Buffer()
                        request.body?.writeTo(buffer)
                        captured += Captured(
                            request.url.encodedPath,
                            request.url.queryParameter("redirect_to"),
                            request.url.queryParameter("grant_type"),
                            Json.parseToJsonElement(buffer.readUtf8().ifBlank { "{}" }).jsonObject,
                            request.method,
                            request.header("Authorization") == "Bearer synthetic-access",
                        )
                        val json = when (request.url.encodedPath) {
                            "/auth/v1/recover" -> "{}"
                            "/auth/v1/signup" -> userJson
                            "/auth/v1/token" -> tokenResponse ?: sessionJson()
                            "/auth/v1/user" -> userResponse ?: userJson
                            "/auth/v1/logout" -> "{}"
                            else -> error("Pedido inesperado no teste isolado.")
                        }
                        Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                            .code(when (request.url.encodedPath) {
                                "/auth/v1/token" -> tokenStatus
                                "/auth/v1/user" -> userStatus
                                else -> 200
                            })
                            .message("Synthetic response")
                            .body(json.toResponseBody("application/json".toMediaType())).build()
                    }
                }
            }
            install(Auth) {
                flowType = FlowType.PKCE
                sessionManager = MemorySessionManager()
                codeVerifierCache = verifierCache
                autoLoadFromStorage = false
                autoSaveToStorage = false
                alwaysAutoRefresh = false
                enableLifecycleCallbacks = false
            }
        }
        source = SupabaseAuthDataSource(client)
    }

    @After fun closeClient() = runBlocking { client.close() }

    @Test fun settingsPasswordChangeUsesAuthenticatedPutAndExactFieldsWithoutTrimming() = runBlocking {
        source.signIn("cadastro@example.com", "SyntheticOld1!")
        captured.clear()
        verifierCache.saveCodeVerifier("synthetic-pending-verifier")
        val currentPassword = " old "
        val newPassword = " NewSynthetic1! "

        source.changePassword(currentPassword, newPassword)

        val request = captured.single()
        assertEquals("/auth/v1/user", request.path)
        assertEquals("PUT", request.method)
        assertTrue(request.usesSyntheticSession)
        assertEquals(setOf("password", "current_password"), request.body.keys)
        assertEquals(currentPassword, request.body["current_password"]?.jsonPrimitive?.content)
        assertEquals(newPassword, request.body["password"]?.jsonPrimitive?.content)
        assertEquals(syntheticId, client.auth.currentUserOrNull()?.id)
        assertTrue(verifierCache.loadCodeVerifier() == "synthetic-pending-verifier")
    }

    @Test fun settingsPasswordChangeMapsAuthErrorsAndDoesNotMistakeMissingProtectionForWrongPassword() = runBlocking {
        source.signIn("cadastro@example.com", "SyntheticOld1!")
        val repository = SupabaseAuthRepository(source)
        for ((code, expected) in listOf(
            "current_password_mismatch" to AuthError.CURRENT_PASSWORD_INCORRECT,
            "current_password_invalid" to AuthError.CURRENT_PASSWORD_INCORRECT,
            "current_password_required" to AuthError.UNKNOWN,
            "weak_password" to AuthError.WEAK_PASSWORD,
            "same_password" to AuthError.SAME_PASSWORD,
            "over_request_rate_limit" to AuthError.RATE_LIMITED,
            "future_code" to AuthError.UNKNOWN,
            // O SDK limpa a sessão após essa resposta; deve ser o último caso.
            "session_not_found" to AuthError.SESSION_UNAVAILABLE,
        )) {
            userStatus = 400
            userResponse = """{"error_code":"$code","msg":"Synthetic internal detail"}"""
            val result = repository.changePassword("SyntheticOld1!", "SyntheticNew1!")
            assertTrue(result.isFailure)
            assertEquals(code, expected, (result.exceptionOrNull() as? AuthFailure)?.reason)
            assertNull(result.exceptionOrNull()?.message)
        }
    }

    @Test fun settingsPasswordChangeWithoutSessionDoesNotSendARequest() = runBlocking {
        val result = SupabaseAuthRepository(source)
            .changePassword("SyntheticOld1!", "SyntheticNew1!")
        assertTrue(result.isFailure)
        assertEquals(AuthError.SESSION_UNAVAILABLE, (result.exceptionOrNull() as? AuthFailure)?.reason)
        assertTrue(captured.isEmpty())
    }

    @Test fun passwordUpdatePreservesValueAndLocalSignOutClearsRecoveredSession() = runBlocking {
        source.requestPasswordReset("cadastro@example.com")
        source.exchangeRecoveryCode("synthetic-code")
        val password = " Synthetic123! "
        source.updatePassword(password)
        val update = captured.single { it.path == "/auth/v1/user" }
        assertEquals(password, update.body["password"]?.jsonPrimitive?.content)
        assertEquals(syntheticId, client.auth.currentUserOrNull()?.id)
        source.signOut()
        assertNull(client.auth.currentUserOrNull())
        assertNull(verifierCache.loadCodeVerifier())
    }

    @Test fun passwordUpdateClassifiesStructuredFailuresFromRealSdk() = runBlocking {
        source.requestPasswordReset("cadastro@example.com")
        source.exchangeRecoveryCode("synthetic-code")
        val repository = SupabaseAuthRepository(source)
        for ((code, expected) in listOf(
            "same_password" to AuthError.SAME_PASSWORD,
            "weak_password" to AuthError.WEAK_PASSWORD,
            "session_not_found" to AuthError.SESSION_UNAVAILABLE,
        )) {
            userStatus = 400
            userResponse = """{"error_code":"$code","msg":"Synthetic failure"}"""
            val failure = repository.updatePassword("Synthetic123!").exceptionOrNull()
            assertTrue(failure is AuthFailure)
            assertEquals(expected, (failure as AuthFailure).reason)
        }
    }

    @Test fun recoveryRequestSendsChallengeAndDedicatedRedirectWithoutVerifier() = runBlocking {
        source.requestPasswordReset("  cadastro@example.com  ")
        assertEquals(1, captured.size)
        val request = captured.single()
        assertEquals("/auth/v1/recover", request.path)
        assertEquals("br.edu.fatec.doesangue://auth/recovery", request.redirect)
        assertEquals("cadastro@example.com", request.body["email"]?.jsonPrimitive?.content)
        assertPkceChallenge(request.body)
        assertNull(client.auth.currentUserOrNull())
    }

    @Test fun registrationKeepsMetadataAndPendingConfirmationWhileUsingPkce() = runBlocking {
        val type = BloodType.entries.first()
        val result = source.signUp(" Doador Sintético ", " cadastro@example.com ", "Sintetica123!", type)
        assertEquals(SupabaseAuthDataSource.CONFIRMATION_REDIRECT, captured.single().redirect)
        assertEquals(1, captured.size)
        val request = captured.single()
        assertEquals("/auth/v1/signup", request.path)
        assertEquals("cadastro@example.com", request.body["email"]?.jsonPrimitive?.content)
        val metadata = request.body.getValue("data").jsonObject
        assertEquals("Doador Sintético", metadata["nome_exibicao"]?.jsonPrimitive?.content)
        assertEquals(type.databaseValue, metadata["tipo_sanguineo"]?.jsonPrimitive?.content)
        assertPkceChallenge(request.body)
        assertNull(result.authenticatedUser)
    }

    @Test fun passwordLoginStillUsesPasswordGrantWithoutRecoveryChallenge() = runBlocking {
        val result = source.signIn("cadastro@example.com", "Sintetica123!")
        assertEquals(syntheticId, result.id)
        assertEquals(1, captured.size)
        val request = captured.single()
        assertEquals("/auth/v1/token", request.path)
        assertEquals("password", request.grant)
        assertEquals("cadastro@example.com", request.body["email"]?.jsonPrimitive?.content)
        assertNull(request.body["code_challenge"])
        assertNull(verifierCache.loadCodeVerifier())
    }

    @Test fun blankCodeAndMissingVerifierFailWithoutHttpRequests() = runBlocking {
        val repository = SupabaseAuthRepository(source)
        verifierCache.saveCodeVerifier("synthetic-verifier")
        val blank = repository.exchangeRecoveryCode(" ")
        assertEquals(AuthError.SESSION_UNAVAILABLE, (blank.exceptionOrNull() as? AuthFailure)?.reason)
        assertTrue(captured.isEmpty())
        assertTrue(verifierCache.loadCodeVerifier() != null)
        verifierCache.deleteCodeVerifier()
        val missing = repository.exchangeRecoveryCode("synthetic-code")
        assertEquals(AuthError.SESSION_UNAVAILABLE, (missing.exceptionOrNull() as? AuthFailure)?.reason)
        assertTrue(captured.isEmpty())
        assertNull(client.auth.currentUserOrNull())
    }

    @Test fun existingSessionDoesNotValidateCodeWithoutVerifier() = runBlocking {
        source.signIn("cadastro@example.com", "Sintetica123!")
        val result = SupabaseAuthRepository(source).exchangeRecoveryCode("synthetic-code")
        assertTrue(result.isFailure)
        assertEquals(AuthError.SESSION_UNAVAILABLE, (result.exceptionOrNull() as? AuthFailure)?.reason)
        assertEquals(1, captured.size)
        assertEquals(syntheticId, client.auth.currentUserOrNull()?.id)
    }

    @Test fun exchangeUsesPkceVerifierAndResponseUserAndRejectsRepeat() = runBlocking {
        source.signIn("cadastro@example.com", "Sintetica123!")
        source.requestPasswordReset("cadastro@example.com")
        val verifier = verifierCache.loadCodeVerifier()
        val newId = "00000000-0000-4000-8000-000000000002"
        tokenResponse = sessionJson(userJson.replace(syntheticId, newId))
        val repository = SupabaseAuthRepository(source)
        val result = repository.exchangeRecoveryCode("synthetic-code")
        assertEquals(newId, result.getOrNull()?.id)
        assertEquals(newId, client.auth.currentUserOrNull()?.id)
        val exchange = captured.last()
        assertEquals("pkce", exchange.grant)
        assertEquals("synthetic-code", exchange.body["auth_code"]?.jsonPrimitive?.content)
        assertTrue(verifier != null && verifier == exchange.body["code_verifier"]?.jsonPrimitive?.content)
        assertNull(verifierCache.loadCodeVerifier())
        val count = captured.size
        val repeated = repository.exchangeRecoveryCode("synthetic-code")
        assertEquals(AuthError.SESSION_UNAVAILABLE, (repeated.exceptionOrNull() as? AuthFailure)?.reason)
        assertEquals(count, captured.size)
        assertEquals(newId, client.auth.currentUserOrNull()?.id)
    }

    @Test fun rejectedExchangePreservesVerifierAndOldSessionAndAllowsRetry() = runBlocking {
        source.signIn("cadastro@example.com", "Sintetica123!")
        source.requestPasswordReset("cadastro@example.com")
        tokenStatus = 429
        tokenResponse = """{"error_code":"over_request_rate_limit","msg":"detalhe-interno-ficticio"}"""
        val repository = SupabaseAuthRepository(source)
        val failure = repository.exchangeRecoveryCode("synthetic-code")
        assertEquals(AuthError.RATE_LIMITED, (failure.exceptionOrNull() as? AuthFailure)?.reason)
        assertNull(failure.exceptionOrNull()?.message)
        assertTrue(verifierCache.loadCodeVerifier() != null)
        assertEquals(syntheticId, client.auth.currentUserOrNull()?.id)
        tokenStatus = 200
        tokenResponse = null
        val success = repository.exchangeRecoveryCode("synthetic-code")
        assertEquals(syntheticId, success.getOrNull()?.id)
        assertNull(verifierCache.loadCodeVerifier())
    }

    @Test fun sessionResponseWithoutUserCannotValidateRecovery() = runBlocking {
        source.requestPasswordReset("cadastro@example.com")
        tokenResponse = sessionJson("null")
        val result = SupabaseAuthRepository(source).exchangeRecoveryCode("synthetic-code")
        assertTrue(result.isFailure)
        assertEquals(AuthError.SESSION_UNAVAILABLE, (result.exceptionOrNull() as? AuthFailure)?.reason)
        assertNull(client.auth.currentUserOrNull())
    }

    private fun sessionJson(user: String = userJson) =
        """{"access_token":"synthetic-access","refresh_token":"synthetic-refresh","token_type":"bearer","expires_in":3600,"user":$user}"""

    private suspend fun assertPkceChallenge(body: JsonObject) {
        val verifier = verifierCache.loadCodeVerifier()
        assertTrue(!verifier.isNullOrBlank())
        val expected = Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(requireNotNull(verifier).toByteArray(Charsets.UTF_8)),
        )
        // Não imprimir o verificador gerado em mensagens de asserção.
        assertTrue(expected == body["code_challenge"]?.jsonPrimitive?.content)
        assertEquals("s256", body["code_challenge_method"]?.jsonPrimitive?.content)
        assertNull(body["code_verifier"])
    }
}
