package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.coroutines.cancellation.CancellationException

internal fun AuthRestException.toAuthError(): AuthError =
    when (errorCode) {
        AuthErrorCode.EmailAddressInvalid ->
            AuthError.INVALID_EMAIL

        AuthErrorCode.EmailNotConfirmed ->
            AuthError.EMAIL_NOT_CONFIRMED

        AuthErrorCode.InvalidCredentials ->
            AuthError.INVALID_CREDENTIALS

        AuthErrorCode.EmailExists,
        AuthErrorCode.UserAlreadyExists ->
            AuthError.ACCOUNT_ALREADY_EXISTS

        AuthErrorCode.WeakPassword ->
            AuthError.WEAK_PASSWORD

        AuthErrorCode.OverRequestRateLimit,
        AuthErrorCode.OverEmailSendRateLimit ->
            AuthError.RATE_LIMITED

        AuthErrorCode.EmailAddressNotAuthorized ->
            AuthError.EMAIL_NOT_AUTHORIZED

        AuthErrorCode.SamePassword ->
            AuthError.SAME_PASSWORD

        AuthErrorCode.SessionNotFound,
        AuthErrorCode.SessionExpired,
        AuthErrorCode.BadJwt,
        AuthErrorCode.FlowStateNotFound,
        AuthErrorCode.FlowStateExpired,
        AuthErrorCode.BadCodeVerifier ->
            AuthError.SESSION_UNAVAILABLE

        else -> when (error) {
            "current_password_mismatch",
            "current_password_invalid" ->
                AuthError.CURRENT_PASSWORD_INCORRECT

            else -> AuthError.UNKNOWN
        }
    }

internal fun Throwable.toAuthFailure(): AuthFailure =
    when (this) {
        is CancellationException -> throw this
        is AuthFailure -> this
        is SessionRequiredException ->
            AuthFailure(AuthError.SESSION_UNAVAILABLE)
        is AuthRestException -> AuthFailure(toAuthError())

        is HttpRequestException,
        is HttpRequestTimeoutException ->
            AuthFailure(AuthError.NETWORK)

        else -> AuthFailure(AuthError.UNKNOWN)
    }
