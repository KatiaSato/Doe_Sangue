@file:OptIn(kotlin.time.ExperimentalTime::class, io.github.jan.supabase.annotations.SupabaseInternal::class)

package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.repository.*
import br.edu.fatec.doesangue.data.supabase.mapper.toSessionPresence
import br.edu.fatec.doesangue.domain.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.*
import io.github.jan.supabase.auth.status.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** SDK real; transporte totalmente interceptado e armazenamento somente em memória. */
class SupabaseSessionMonitorTest {
    private lateinit var client: SupabaseClient
    private lateinit var source: SupabaseAuthDataSource
    private val id = "00000000-0000-4000-8000-000000000001"
    @Volatile private var refreshMode = "success"
    @Volatile private var logoutFails = false
    private val paths = java.util.Collections.synchronizedList(mutableListOf<String>())
    private fun sessionJson() = """{"access_token":"synthetic-access","refresh_token":"synthetic-refresh","token_type":"bearer","expires_in":3600,"user":{"id":"$id","aud":"authenticated","role":"authenticated","created_at":"2026-10-26T00:00:00Z","app_metadata":{},"user_metadata":{}}}"""
    @Before fun setup() {
        client = createSupabaseClient("https://session.example.test", "sb_publishable_synthetic") {
            install(Auth) {
                sessionManager = MemorySessionManager(); codeVerifierCache = MemoryCodeVerifierCache()
                autoLoadFromStorage = false; autoSaveToStorage = false; alwaysAutoRefresh = true
                enableLifecycleCallbacks = false; retryDelay = 200.milliseconds
            }
            install(Postgrest)
            httpEngine = OkHttp.create { config { addInterceptor { chain ->
                val request = chain.request(); val path = request.url.encodedPath
                paths += path
                val refresh = request.url.queryParameter("grant_type") == "refresh_token"
                if (refresh && refreshMode == "offline") throw IOException("Synthetic transport interruption")
                val rejected = refresh && refreshMode == "rejected"
                val failedLogout = path.endsWith("logout") && logoutFails
                val body = when {
                    rejected -> """{"code":"refresh_token_not_found","msg":"Synthetic refresh rejected"}"""
                    failedLogout -> """{"code":"unexpected_failure","msg":"Synthetic logout failure"}"""
                    path.endsWith("logout") -> "{}"
                    path.startsWith("/auth/") -> sessionJson()
                    else -> "[]"
                }
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(if (rejected) 400 else if (failedLogout) 500 else 200).message("Synthetic response")
                    .body(body.toResponseBody("application/json".toMediaType())).build()
            } } }
        }
        source = SupabaseAuthDataSource(client)
    }
    @After fun close() = runBlocking { client.close() }
    private suspend fun login() = source.signIn("synthetic@example.test", "Synthetic-only-1!")
    private suspend fun expire() {
        val old = checkNotNull(client.auth.currentSessionOrNull())
        client.auth.importSession(old.copy(expiresIn = 1, expiresAt = kotlin.time.Clock.System.now() - 1.seconds), autoRefresh = true)
    }
    private suspend fun awaitPresence(predicate: (SessionPresence) -> Boolean) =
        withTimeout(8_000) { source.observeSession().first(predicate) }

    @Test fun rejectedExpiredRefreshEndsSessionWithoutClaimingVoluntaryLogout() = runBlocking<Unit> {
        login(); refreshMode = "rejected"; expire()
        assertEquals(SessionPresence.NotAuthenticated(false), awaitPresence { it is SessionPresence.NotAuthenticated })
        assertTrue((client.auth.sessionStatus.value as SessionStatus.NotAuthenticated).isSignOut)
        assertFalse(paths.any { it.endsWith("logout") })
    }
    @Test fun offlineExpiredRefreshRetriesAndRecoversSameIdentityWithoutClearingVerifier() = runBlocking<Unit> {
        login(); client.auth.codeVerifierCache.saveCodeVerifier("synthetic-verifier")
        refreshMode = "offline"; expire()
        awaitPresence { it == SessionPresence.RefreshPending }
        assertEquals("synthetic-verifier", client.auth.codeVerifierCache.loadCodeVerifier())
        refreshMode = "success"
        assertEquals(SessionPresence.Authenticated(AuthenticatedUser(id)), awaitPresence { it is SessionPresence.Authenticated })
        assertFalse(paths.any { it.endsWith("logout") })
    }
    @Test fun explicitLogoutIsRecognizedAndNewLoginClearsItsIntent() = runBlocking<Unit> {
        login(); source.signOut()
        assertEquals(SessionPresence.NotAuthenticated(true), awaitPresence { it is SessionPresence.NotAuthenticated })
        login(); client.auth.clearSession()
        assertEquals(SessionPresence.NotAuthenticated(false), awaitPresence { it is SessionPresence.NotAuthenticated })
    }
    @Test fun failedLogoutWithSessionStillPresentDoesNotBecomeDefiniteLoss() = runBlocking<Unit> {
        login(); logoutFails = true
        assertTrue(runCatching { source.signOut() }.isFailure)
        assertEquals(SessionPresence.Authenticated(AuthenticatedUser(id)), awaitPresence { it is SessionPresence.Authenticated })
        client.auth.clearSession()
        assertEquals(SessionPresence.NotAuthenticated(false), awaitPresence { it is SessionPresence.NotAuthenticated })
    }
    @Test fun pendingRefreshStopsPrivateRequestsWithRetryCategoryIncludingProfileAndHistory() = runBlocking<Unit> {
        login()
        client.auth.setSessionStatus(SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IOException("synthetic"))))
        val before = paths.size
        val appointments = SupabaseAppointmentRepository(SupabaseAppointmentDataSource(client))
        assertEquals(DataError.SESSION_RETRYING, (appointments.getMyAppointments().exceptionOrNull() as DataFailure).reason)
        assertEquals(DataError.SESSION_RETRYING, (appointments.createAppointment("7", "00000000-0000-4000-8000-000000000099").exceptionOrNull() as DataFailure).reason)
        assertEquals(DataError.SESSION_RETRYING, (appointments.cancelAppointment("9").exceptionOrNull() as DataFailure).reason)
        val profile = SupabaseDonorProfileRepository(SupabaseDonorProfileDataSource(client))
        assertEquals(ProfileError.SESSION_RETRYING, (profile.getCurrentProfile().exceptionOrNull() as ProfileFailure).reason)
        assertEquals(ProfileError.SESSION_RETRYING, (profile.updateCurrentBloodType(BloodType.UNKNOWN).exceptionOrNull() as ProfileFailure).reason)
        val history = SupabaseDonationHistoryRepository(SupabaseDonationHistoryDataSource(client))
        assertEquals(DataError.SESSION_RETRYING, (history.getMyDonations().exceptionOrNull() as DataFailure).reason)
        assertEquals(before, paths.size)
    }
    @Test fun mapperRejectsMissingUserAndDoesNotTrustSdkSignOutFlag() = runBlocking<Unit> {
        login(); val session = checkNotNull(client.auth.currentSessionOrNull())
        assertEquals(SessionPresence.Initializing, SessionStatus.Initializing.toSessionPresence(false))
        assertEquals(SessionPresence.NotAuthenticated(false), SessionStatus.Authenticated(session.copy(user = null)).toSessionPresence(false))
        assertEquals(SessionPresence.NotAuthenticated(false), SessionStatus.NotAuthenticated(true).toSessionPresence(false))
        assertEquals(SessionPresence.NotAuthenticated(true), SessionStatus.NotAuthenticated(false).toSessionPresence(true))
        assertEquals(SessionPresence.Authenticated(AuthenticatedUser(id)), SessionStatus.Authenticated(session).toSessionPresence(false))
    }
    @Test fun privateGuardWaitsForSdkInitializationBeforeSendingItsRead() = runBlocking<Unit> {
        login(); val session = checkNotNull(client.auth.currentSessionOrNull())
        client.auth.setSessionStatus(SessionStatus.Initializing)
        val count = paths.size
        val query = async { SupabaseDonorProfileRepository(SupabaseDonorProfileDataSource(client)).getCurrentProfile() }
        yield()
        assertFalse(query.isCompleted); assertEquals(count, paths.size)
        client.auth.setSessionStatus(SessionStatus.Authenticated(session))
        assertNull(withTimeout(5_000) { query.await().getOrThrow() })
        assertEquals(count + 1, paths.size)
    }
}
