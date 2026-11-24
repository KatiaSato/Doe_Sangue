package br.edu.fatec.doesangue.ui.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import br.edu.fatec.doesangue.domain.model.GoogleSignInCredential
import br.edu.fatec.doesangue.presentation.auth.GoogleSignInResult
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.util.UUID

class GoogleSignInLauncher(private val webClientId: String) {

    suspend fun launch(activity: Activity): GoogleSignInResult {
        if (webClientId.isBlank()) return GoogleSignInResult.Failed

        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = MessageDigest.getInstance("SHA-256")
            .digest(rawNonce.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(hashedNonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = response.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return GoogleSignInResult.Failed
            }

            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            if (token.isBlank()) return GoogleSignInResult.Failed
            GoogleSignInResult.Success(GoogleSignInCredential(token, rawNonce))
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInResult.NoCredential
        } catch (_: GetCredentialException) {
            GoogleSignInResult.Failed
        } catch (_: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failed
        }
    }
}
