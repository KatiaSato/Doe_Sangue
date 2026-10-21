package br.edu.fatec.doesangue.ui.navigation

import android.net.Uri

internal fun recoveryCodeFromUri(uri: Uri): String? {
    if (!uri.isHierarchical ||
        uri.scheme != "br.edu.fatec.doesangue" ||
        uri.encodedAuthority != "auth" ||
        uri.encodedPath != "/recovery" ||
        uri.fragment != null
    ) return null

    if (uri.queryParameterNames.any {
            it in setOf("error", "error_code", "error_description")
        }
    ) return null

    return uri.getQueryParameters("code")
        .singleOrNull()
        ?.takeIf { code ->
            code.isNotBlank() && code.none { it.isWhitespace() }
        }
}