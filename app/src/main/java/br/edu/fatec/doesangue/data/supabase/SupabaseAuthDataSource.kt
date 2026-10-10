package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

// Realiza a autenticação usando o cliente Supabase.
class SupabaseAuthDataSource(
    private val client: SupabaseClient,
) {
    suspend fun signIn(
        email: String,
        password: String,
    ): AuthenticatedUser {
        client.auth.awaitInitialization()

        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }

        val user = checkNotNull(client.auth.currentUserOrNull()) {
            "Não foi possível obter o usuário autenticado."
        }

        return AuthenticatedUser(id = user.id)
    }
}