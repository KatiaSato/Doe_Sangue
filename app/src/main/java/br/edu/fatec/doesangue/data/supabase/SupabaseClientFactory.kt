package br.edu.fatec.doesangue.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

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
        install(Postgrest)
    }
}
