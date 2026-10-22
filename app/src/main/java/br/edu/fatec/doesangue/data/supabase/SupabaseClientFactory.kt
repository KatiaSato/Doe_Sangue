package br.edu.fatec.doesangue.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.MemoryCodeVerifierCache

// Cria o cliente usado para acessar a API do Supabase.
fun createDonationSupabaseClient(
    url: String,
    publishableKey: String,
): SupabaseClient {
    require(url.startsWith("https://")) {
        "Configure uma URL HTTPS válida para o Supabase."
    }

    require(publishableKey.startsWith("sb_publishable_")) {
        "Configure a chave publicável do Supabase."
    }

    return createSupabaseClient(
        supabaseUrl = url,
        supabaseKey = publishableKey,
    ) {
        install(Auth) {
            flowType = FlowType.PKCE
        }
        install(Postgrest)
    }
}

// O retorno de confirmação não deve autenticar nem substituir a conta do app.
internal fun createConfirmationSupabaseClient(url: String, key: String): SupabaseClient =
    createSupabaseClient(url, key) {
        install(Auth) {
            flowType = FlowType.PKCE
            sessionManager = MemorySessionManager()
            codeVerifierCache = MemoryCodeVerifierCache()
            autoLoadFromStorage = false
            autoSaveToStorage = false
            alwaysAutoRefresh = false
            enableLifecycleCallbacks = false
        }
    }
