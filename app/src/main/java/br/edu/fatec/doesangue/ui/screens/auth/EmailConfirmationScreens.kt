package br.edu.fatec.doesangue.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import br.edu.fatec.doesangue.presentation.auth.ConfirmationLinkState
import br.edu.fatec.doesangue.presentation.auth.EmailConfirmationUiState
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.ScreenHeading
import br.edu.fatec.doesangue.ui.components.SecondaryButton

@Composable
fun EmailVerificationScreen(
    state: EmailConfirmationUiState,
    onEmailChange: (String) -> Unit,
    onResend: () -> Unit,
    onUnderstood: () -> Unit,
) {
    BackHandler { if (!state.isSending) onUnderstood() }
    DsScreen(title = "Confirme seu e-mail", onBack = if (state.isSending) null else onUnderstood) {
        Spacer(Modifier.height(24.dp))
        ScreenHeading("Verifique seu e-mail", "Abra o link recebido para confirmar o cadastro. Depois, entre na sua conta.")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(state.email, onEmailChange, label = { Text("E-mail") },
            singleLine = true, enabled = !state.isSending, isError = state.emailError != null,
            supportingText = state.emailError?.let { message -> { Text(message) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false, keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth().semantics {
                contentType = ContentType.Username + ContentType.EmailAddress
            })
        Spacer(Modifier.height(12.dp))
        Text("Não recebeu? Confira o spam ou solicite outro link.", style = MaterialTheme.typography.bodyMedium)
        state.sendMessage?.let { Text(it, Modifier.padding(vertical = 12.dp)) }
        state.sendError?.let { Text(it, Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(when {
            state.isSending -> "Reenviando…"
            state.waitSeconds > 0 -> "Aguarde ${state.waitSeconds} s"
            else -> "Reenviar confirmação"
        }, onResend, enabled = !state.isSending && state.waitSeconds == 0)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Entendi", onUnderstood, enabled = !state.isSending)
    }
}

@Composable
fun EmailConfirmationDialog(state: ConfirmationLinkState, onRetry: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(when (state) {
                    ConfirmationLinkState.Confirmed -> "E-mail confirmado"
                    ConfirmationLinkState.Checking -> "Conferindo confirmação…"
                    else -> "Não foi possível confirmar"
                }, style = MaterialTheme.typography.titleLarge)
                when (state) {
                    ConfirmationLinkState.Confirmed -> Text("Seu e-mail foi confirmado. Você já pode entrar com essa conta.")
                    ConfirmationLinkState.Checking -> Text("Aguarde a validação do link.")
                    is ConfirmationLinkState.Error -> {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        if (state.canRetry) PrimaryButton("Tentar novamente", onRetry)
                    }
                    ConfirmationLinkState.Idle -> Unit
                }
                SecondaryButton(if (state == ConfirmationLinkState.Checking) "Cancelar" else "Continuar", onClose)
            }
        }
    }
}
