package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.domain.model.DataError
import br.edu.fatec.doesangue.domain.model.DataFailure
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.api.authenticatedSupabaseApi
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.result.PostgrestResult
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonObject

/** ID e credencial são capturados juntos. A credencial nunca sai da camada data. */
@OptIn(SupabaseInternal::class)
internal suspend fun SupabaseClient.ownedDataRpc(ownerId: String, function: String, parameters: JsonObject): PostgrestResult {
    val session = requireDataSession()
    if (ownerId.isBlank() || session.user?.id != ownerId) throw DataFailure(DataError.SESSION_UNAVAILABLE)
    val api = authenticatedSupabaseApi(postgrest, requireSession = true) { jwtToken = session.accessToken }
    val response = api.postJson("rpc/$function", parameters.toString()) {
        headers["Content-Profile"] = postgrest.config.defaultSchema
    }
    return PostgrestResult(response.bodyAsText(), response.headers, postgrest)
}
