package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.RegistrationResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.api.authenticatedSupabaseApi
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

    /*
    Essa função faz o pedido pelo SDK.
    O trim() remove espaços nas extremidades do endereço antes do envio.
    redirectUrl é o endereço de retorno ao aplicativo após abrir o link recebido.
     */
    suspend fun requestPasswordReset(email: String) {
        client.auth.awaitInitialization()

        client.auth.resetPasswordForEmail(
            email = email.trim(),
            redirectUrl = "br.edu.fatec.doesangue://auth/recovery",
        )
    }

    suspend fun exchangeRecoveryCode(code: String): AuthenticatedUser {
        client.auth.awaitInitialization()

        if (code.isBlank() ||
            client.auth.codeVerifierCache.loadCodeVerifier().isNullOrBlank()
        ) {
            throw AuthFailure(AuthError.SESSION_UNAVAILABLE)
        }

        val session = client.auth.exchangeCodeForSession(code)
        val user = session.user
            ?: throw AuthFailure(AuthError.SESSION_UNAVAILABLE)

        return AuthenticatedUser(id = user.id)
    }

    suspend fun updatePassword(password: String) {
        client.auth.awaitInitialization()

        client.auth.updateUser {
            this.password = password
        }
    }

    @OptIn(SupabaseInternal::class)
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ) {
        client.auth.awaitInitialization()

        val body = buildJsonObject {
            put("password", newPassword)
            put("current_password", currentPassword)
        }.toString()

        val api = client.authenticatedSupabaseApi(
            client.auth,
            requireSession = true,
        )

        api.putJson("user", body)
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
