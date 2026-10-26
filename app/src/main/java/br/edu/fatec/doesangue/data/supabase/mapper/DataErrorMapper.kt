package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.domain.model.DataError
import br.edu.fatec.doesangue.domain.model.DataFailure
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.SessionRequiredException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

internal fun Throwable.toDataFailure(): DataFailure = when (this) {
    is CancellationException -> throw this
    is DataFailure -> this
    is SessionRequiredException -> DataFailure(DataError.SESSION_UNAVAILABLE)
    is AuthRestException -> DataFailure(when (errorCode) {
        AuthErrorCode.SessionExpired, AuthErrorCode.SessionNotFound, AuthErrorCode.BadJwt -> DataError.SESSION_UNAVAILABLE
        else -> DataError.UNKNOWN
    })
    is HttpRequestException, is HttpRequestTimeoutException, is IOException -> DataFailure(DataError.NETWORK)
    is PostgrestRestException -> DataFailure(when (code) {
        "PGRST301", "PGRST302", "PGRST303", "28000", "28P01" -> DataError.SESSION_UNAVAILABLE
        "42501" -> DataError.ACCESS_DENIED
        "PGRST000", "PGRST001", "PGRST002", "PGRST003", "PGRST202", "PGRST204", "PGRST205",
        "42883", "42703", "42P01" -> DataError.SERVICE_UNAVAILABLE
        "22023", "22P02", "23514", "23502", "22007", "22008" -> DataError.INVALID_DATA
        "40001", "40P01", "55P03" -> DataError.BUSY
        else -> DataError.UNKNOWN
    })
    else -> DataFailure(DataError.UNKNOWN)
}

/** DETAIL é um identificador versionado, nunca a frase de erro nem detalhes de constraint. */
internal fun Throwable.toAppointmentFailure(): DataFailure {
    if (this is PostgrestRestException) {
        val identifier = (details as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
        val reason = when (code to identifier) {
            "P0001" to "doesangue.session_required.v1", "42501" to "doesangue.session_required.v1" -> DataError.SESSION_UNAVAILABLE
            "P0001" to "doesangue.profile_required.v1", "42501" to "doesangue.profile_required.v1" -> DataError.PROFILE_REQUIRED
            "P0001" to "doesangue.slot_unavailable.v1" -> DataError.SLOT_UNAVAILABLE
            "P0001" to "doesangue.slot_started.v1" -> DataError.SLOT_STARTED
            "P0001" to "doesangue.slot_full.v1" -> DataError.SLOT_FULL
            "P0001" to "doesangue.already_booked.v1" -> DataError.ALREADY_BOOKED
            "P0001" to "doesangue.request_conflict.v1" -> DataError.REQUEST_CONFLICT
            "42501" to "doesangue.appointment_unavailable.v1", "P0001" to "doesangue.appointment_unavailable.v1" -> DataError.APPOINTMENT_UNAVAILABLE
            "P0001" to "doesangue.cancellation_closed.v1" -> DataError.CANCELLATION_CLOSED
            "P0001" to "doesangue.invalid_request.v1", "22023" to "doesangue.invalid_request.v1" -> DataError.INVALID_DATA
            "P0001" to "doesangue.isolation_unsupported.v1", "25000" to "doesangue.isolation_unsupported.v1" -> DataError.SERVICE_UNAVAILABLE
            else -> null
        }
        if (reason != null) return DataFailure(reason)
    }
    return toDataFailure()
}
