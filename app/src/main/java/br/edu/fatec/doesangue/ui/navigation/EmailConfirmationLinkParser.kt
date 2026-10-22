package br.edu.fatec.doesangue.ui.navigation

import android.net.Uri
import br.edu.fatec.doesangue.domain.model.EmailConfirmationCredential

/** Retorna apenas a credencial esperada, nunca importa a URI como uma sessão. */
internal fun confirmationCredentialFromUri(uri: Uri): EmailConfirmationCredential? {
    if (!uri.isHierarchical || uri.scheme != "br.edu.fatec.doesangue" ||
        uri.encodedAuthority != "auth" || uri.encodedPath != "/confirmation") return null
    fun String.valid() = isNotBlank() && length <= 16_384 && none { it.isWhitespace() || it.isISOControl() }
    if (uri.fragment == null) {
        if (uri.queryParameterNames != setOf("code")) return null
        return uri.getQueryParameters("code").singleOrNull()?.takeIf { it.valid() }
            ?.let(EmailConfirmationCredential::Code)
    }
    if (uri.encodedQuery != null) return null
    val fragment = Uri.Builder().encodedQuery(uri.encodedFragment).build()
    val allowed = setOf("access_token", "refresh_token", "token_type", "expires_in", "expires_at", "type")
    if (fragment.queryParameterNames.any { it !in allowed || fragment.getQueryParameters(it).size != 1 } ||
        fragment.getQueryParameter("type") != "signup" ||
        fragment.getQueryParameter("token_type") != "bearer") return null
    return fragment.getQueryParameters("access_token").singleOrNull()?.takeIf { it.valid() }
        ?.let(EmailConfirmationCredential::AccessToken)
}
