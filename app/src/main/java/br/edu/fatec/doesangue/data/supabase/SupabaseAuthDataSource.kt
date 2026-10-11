package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus

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

    // Encerra somente a sessão atual.
    suspend fun signOut() {
        client.auth.awaitInitialization()
        client.auth.signOut(scope = SignOutScope.LOCAL)
    }

    suspend fun restoreSession(): AuthenticatedUser? {
        client.auth.awaitInitialization()       // awaitInitialization() aguarda o SDK terminar a inicialização

        return when (val status = client.auth.sessionStatus.value) {
            is SessionStatus.Authenticated -> {
                val user = checkNotNull(status.session.user) {
                    "A sessão não contém o usuário."
                }

                AuthenticatedUser(id = user.id)
            }

            is SessionStatus.NotAuthenticated -> null

            else -> error("Não foi possível restaurar a sessão.")
        }
    }

}