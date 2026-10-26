package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.domain.model.ProfileError
import br.edu.fatec.doesangue.domain.model.ProfileFailure
import br.edu.fatec.doesangue.domain.model.DataError
import kotlin.coroutines.cancellation.CancellationException

internal fun Throwable.toProfileFailure(): ProfileFailure = when (this) {
    is CancellationException -> throw this
    is ProfileFailure -> this
    else -> ProfileFailure(when (toDataFailure().reason) {
        DataError.NETWORK -> ProfileError.NETWORK
        DataError.SESSION_UNAVAILABLE -> ProfileError.SESSION_UNAVAILABLE
        DataError.ACCESS_DENIED -> ProfileError.ACCESS_DENIED
        DataError.INVALID_DATA -> ProfileError.INVALID_DATA
        DataError.SERVICE_UNAVAILABLE -> ProfileError.SCHEMA_NOT_READY
        else -> ProfileError.UNKNOWN
    })
}
