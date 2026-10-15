package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.RegistrationResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// Realiza a autenticação usando o cliente Supabase.
class SupabaseAuthDataSource(
    private val client: SupabaseClient,
) {
    suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
        bloodType: BloodType,
    ): RegistrationResult {
        require(displayName.isNotBlank()) { "Informe seu nome." }
        client.auth.awaitInitialization()
        check(client.auth.currentUserOrNull() == null) { "Já existe uma sessão conectada." }
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject {
                put("nome_exibicao", displayName.trim())
                put("tipo_sanguineo", bloodType.databaseValue)
            }
        }
        return RegistrationResult(
            authenticatedUser = client.auth.currentUserOrNull()?.let { AuthenticatedUser(it.id) },
        )
    }

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
