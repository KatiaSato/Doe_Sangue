package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.domain.model.ProfileError
import br.edu.fatec.doesangue.domain.model.ProfileFailure
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.coroutines.cancellation.CancellationException

internal fun Throwable.toProfileFailure(): ProfileFailure = when (this) {
    is CancellationException -> throw this
    is ProfileFailure -> this
    is HttpRequestException, is HttpRequestTimeoutException -> ProfileFailure(ProfileError.NETWORK)
    is PostgrestRestException -> ProfileFailure(when (code) {
        "42501" -> ProfileError.ACCESS_DENIED
        "23514", "23502", "22007", "22008" -> ProfileError.INVALID_DATA
        "PGRST301", "PGRST302", "PGRST303" -> ProfileError.SESSION_UNAVAILABLE
        "PGRST204", "42703" -> ProfileError.SCHEMA_NOT_READY
        else -> ProfileError.UNKNOWN
    })
    else -> ProfileFailure(ProfileError.UNKNOWN)
}
