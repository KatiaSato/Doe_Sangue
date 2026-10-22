package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthErrorMapperTest {
    // Builds response objects in memory. No request is executed by this client.
    private val client = HttpClient(OkHttp)
    private fun request() = HttpRequestBuilder().apply {
        url("https://auth.example.test/auth/v1/token")
    }

    @After fun closeClient() { client.close() }

    @OptIn(InternalAPI::class)
    private fun response(): HttpResponse {
        val response = HttpClientCall(
            client,
            request().build(),
            HttpResponseData(
                statusCode = HttpStatusCode.BadRequest,
                requestTime = GMTDate(0),
                headers = Headers.Empty,
                version = HttpProtocolVersion.HTTP_1_1,
                body = ByteReadChannel.Empty,
                callContext = EmptyCoroutineContext,
            ),
        ).response
        return response
    }

    private fun restError(code: String, description: String = "Synthetic failure") =
        AuthRestException(code, description, response())

    @Test fun structuredCodesDistinguishFailuresSharingHttp400() {
        val cases = mapOf(
            "email_address_invalid" to AuthError.INVALID_EMAIL,
            "email_not_confirmed" to AuthError.EMAIL_NOT_CONFIRMED,
            "invalid_credentials" to AuthError.INVALID_CREDENTIALS,
            "current_password_mismatch" to AuthError.CURRENT_PASSWORD_INCORRECT,
            "current_password_invalid" to AuthError.CURRENT_PASSWORD_INCORRECT,
            "email_exists" to AuthError.ACCOUNT_ALREADY_EXISTS,
            "user_already_exists" to AuthError.ACCOUNT_ALREADY_EXISTS,
            "weak_password" to AuthError.WEAK_PASSWORD,
            "over_request_rate_limit" to AuthError.RATE_LIMITED,
            "over_email_send_rate_limit" to AuthError.RATE_LIMITED,
            "email_address_not_authorized" to AuthError.EMAIL_NOT_AUTHORIZED,
            "same_password" to AuthError.SAME_PASSWORD,
            "session_not_found" to AuthError.SESSION_UNAVAILABLE,
            "session_expired" to AuthError.SESSION_UNAVAILABLE,
            "bad_jwt" to AuthError.SESSION_UNAVAILABLE,
            "flow_state_not_found" to AuthError.SESSION_UNAVAILABLE,
            "flow_state_expired" to AuthError.SESSION_UNAVAILABLE,
            "bad_code_verifier" to AuthError.SESSION_UNAVAILABLE,
        )
        cases.forEach { (code, expected) ->
            val failure = restError(code).toAuthFailure()
            assertEquals(code, expected, failure.reason)
            assertNull(failure.message)
        }
    }

    @Test fun classificationUsesCodeInsteadOfEnglishDescription() {
        val failure = restError("invalid_credentials", "Email not confirmed").toAuthFailure()
        assertEquals(AuthError.INVALID_CREDENTIALS, failure.reason)
    }

    @Test fun currentPasswordClassificationUsesRawCodeAndDoesNotGuessOtherCauses() {
        assertEquals(AuthError.CURRENT_PASSWORD_INCORRECT,
            restError("current_password_mismatch", "Synthetic unrelated description").toAuthFailure().reason)
        assertEquals(AuthError.CURRENT_PASSWORD_INCORRECT,
            restError("current_password_invalid", "Synthetic unrelated description").toAuthFailure().reason)
        assertEquals(AuthError.UNKNOWN,
            restError("current_password_required", "current_password_mismatch").toAuthFailure().reason)
        assertEquals(AuthError.UNKNOWN,
            restError("future_code", "current_password_mismatch").toAuthFailure().reason)
    }

    @Test fun sdkMissingSessionExceptionUsesStructuredSessionCategory() {
        val failure = AuthSessionMissingException(response()).toAuthFailure()
        assertEquals(AuthError.SESSION_UNAVAILABLE, failure.reason)
        assertNull(failure.message)
    }

    @Test fun sdkRequiredSessionExceptionUsesSessionCategoryWithoutExposingItsUrl() {
        val exception = io.github.jan.supabase.auth.exception.SessionRequiredException(
            "https://auth.example.test/auth/v1/user",
        )
        val failure = exception.toAuthFailure()
        assertEquals(AuthError.SESSION_UNAVAILABLE, failure.reason)
        assertNull(failure.message)
    }

    @Test fun unknownCodeDoesNotInferInvalidEmailFromDescriptionOrStatus() {
        val failure = restError("future_server_code", "Email address is invalid").toAuthFailure()
        assertEquals(AuthError.UNKNOWN, failure.reason)
    }

    @Test fun unhandledKnownServerCodeUsesFallbackInsteadOfNetworkOrAccountDiagnosis() {
        assertEquals(AuthError.UNKNOWN, restError("unexpected_failure").toAuthFailure().reason)
    }

    @Test fun requestFailureAndTimeoutAreRecognizedAsTransportFailures() {
        assertEquals(AuthError.NETWORK,
            HttpRequestException("Synthetic connection failure", request()).toAuthFailure().reason)
        assertEquals(AuthError.NETWORK, HttpRequestTimeoutException(request()).toAuthFailure().reason)
    }

    @Test fun cancellationPropagatesTheOriginalException() {
        val cancellation = CancellationException("Synthetic cancellation")
        val thrown = assertThrows(CancellationException::class.java) { cancellation.toAuthFailure() }
        assertSame(cancellation, thrown)
    }

    @Test fun alreadyClassifiedFailureIsPreserved() {
        val failure = AuthFailure(AuthError.EMAIL_NOT_CONFIRMED)
        assertSame(failure, failure.toAuthFailure())
    }

    @Test fun arbitraryExceptionDoesNotInferNetworkFromMessage() {
        assertEquals(AuthError.UNKNOWN,
            IllegalStateException("network email_exists").toAuthFailure().reason)
    }
}
