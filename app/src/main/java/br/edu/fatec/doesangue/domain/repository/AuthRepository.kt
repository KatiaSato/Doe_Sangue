package br.edu.fatec.doesangue.domain.repository

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.RegistrationResult
import br.edu.fatec.doesangue.domain.model.EmailConfirmationCredential

// Define a operação de login sem depender do Supabase.
/*
  Esse contrato diz: “recebo e-mail e senha e devolvo o usuário autenticado ou uma falha”.
- suspend permite aguardar a operação de autenticação dentro de uma corrotina.
- Result<AuthenticatedUser> representa sucesso com o usuário ou falha.
- interface define a operação; a implementação na camada data fará a comunicação com o Supabase.
*/
interface AuthRepository {

    suspend fun resendEmailConfirmation(email: String): Result<Unit>

    // Valida sem importar a sessão recebida sobre a conta conectada.
    suspend fun verifyEmailConfirmation(credential: EmailConfirmationCredential): Result<Unit>

    suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
        bloodType: BloodType,
    ): Result<RegistrationResult>

    suspend fun signIn(
        email: String,
        password: String,
    ): Result<AuthenticatedUser>

    suspend fun requestPasswordReset(
        email: String,
    ): Result<Unit>

    // Esse contrato permitirá trocar o código recebido pelo link por uma sessão validada pelo SDK
    suspend fun exchangeRecoveryCode(
        code: String,
    ): Result<AuthenticatedUser>

    // Esse contrato permite solicitar a alteração sem expor o Supabase ao ViewModel.
    suspend fun updatePassword(
        password: String,
    ): Result<Unit>

    // Esse contrato representa a alteração em Configurações: recebe a senha atual e a nova, devolvendo sucesso ou falha
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): Result<Unit>

    // Encerra a sessão da conta no aplicativo.
    suspend fun signOut(): Result<Unit>

    // Aguarda a restauração e informa se existe uma conta conectada.
    suspend fun restoreSession(): Result<AuthenticatedUser?>
}
