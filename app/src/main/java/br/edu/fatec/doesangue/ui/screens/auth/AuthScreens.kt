package br.edu.fatec.doesangue.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import br.edu.fatec.doesangue.presentation.auth.LoginUiState
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.presentation.auth.PasswordRecoveryUiState
import br.edu.fatec.doesangue.presentation.auth.ResetPasswordUiState
import br.edu.fatec.doesangue.ui.components.CenteredMessage
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.ScreenHeading
import br.edu.fatec.doesangue.ui.components.SecondaryButton
import br.edu.fatec.doesangue.ui.components.SelectionCard
import br.edu.fatec.doesangue.ui.components.StepProgress
import br.edu.fatec.doesangue.ui.components.TextAction
import br.edu.fatec.doesangue.ui.components.VisualField
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.InkSecondary

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onSignIn: (String, String) -> Unit,
    onForgot: () -> Unit,
    onRegister: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    DsScreen {
        Spacer(Modifier.height(54.dp))
        ScreenHeading("Bem-vindo!", "Que bom ter você por aqui.")
        Image(
            painter = painterResource(R.drawable.figma_raw_2),
            contentDescription = "Bolsa de sangue",
            modifier = Modifier.fillMaxWidth().height(190.dp),
            contentScale = ContentScale.Fit,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            placeholder = { Text("seu@email.com") },
            singleLine = true,
            enabled = !uiState.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            placeholder = { Text("Digite sua senha") },
            singleLine = true,
            enabled = !uiState.isLoading,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(
                    onClick = { passwordVisible = !passwordVisible },
                    enabled = !uiState.isLoading,
                ) {
                    Text(if (passwordVisible) "Ocultar" else "Mostrar")
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        TextAction("Esqueci minha senha", onForgot, Modifier.align(Alignment.End))
        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
        }

        PrimaryButton(
            text = if (uiState.isLoading) "Entrando..." else "Entrar",
            onClick = { onSignIn(email, password) },
            enabled = !uiState.isLoading &&
                    uiState.authenticatedUser == null,
        )
        Spacer(Modifier.height(12.dp))
        Text("ou continue com", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = InkSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            SecondaryButton("G  Google", {}, Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            SecondaryButton("●  Facebook", {}, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Não tem uma conta?", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            TextAction("Cadastre-se", onRegister)
        }
    }
}

@Composable
fun EmailVerificationScreen(onUnderstood: () -> Unit) {
    DsScreen {
        Spacer(Modifier.height(150.dp))
        CenteredMessage("✉", "Verifique seu e-mail", "Confira sua caixa de entrada. Se houver confirmação pendente, use o link recebido e depois entre na sua conta.")
        Spacer(Modifier.height(100.dp))
        PrimaryButton("Entendi", onUnderstood)
    }
}

@Composable
fun ForgotPasswordScreen(
    state: PasswordRecoveryUiState,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
) {
    DsScreen(title = "Recuperar senha", onBack = onBack) {
        Spacer(Modifier.height(42.dp))
        CenteredMessage("✉", "Recuperar senha", "Informe seu e-mail para receber o link de recuperação.")
        Spacer(Modifier.height(36.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isSending,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
            ),
        )
        val errorMessage = state.errorMessage
        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (state.requestAccepted) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Solicitação recebida. Se este e-mail estiver cadastrado, " +
                        "confira sua caixa de entrada e o spam para as instruções.",
            )
        }
        Spacer(Modifier.height(28.dp))
        PrimaryButton(
            text = if (state.isSending) "Enviando…" else "Enviar link",
            onClick = onSend,
            enabled = !state.isSending && !state.requestAccepted,
        )
        TextAction("Voltar para o login", onBack, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordUiState,
    onSave: (String, String) -> Unit,
    onFinish: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val finish by rememberUpdatedState(onFinish)
    val fieldsEnabled = !state.isSaving && !state.passwordChanged
    LaunchedEffect(state.passwordChanged) {
        if (state.passwordChanged) {
            password = ""
            confirmation = ""
            passwordVisible = false
            confirmationVisible = false
            Toast.makeText(context, "Senha salva com sucesso.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    DsScreen(title = "Criar nova senha") {
        Text("Digite sua nova senha e confirme abaixo.")
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Nova senha") },
            singleLine = true,
            enabled = fieldsEnabled,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }, enabled = fieldsEnabled) {
                    Text(if (passwordVisible) "Ocultar" else "Mostrar")
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmation,
            onValueChange = { confirmation = it },
            label = { Text("Confirmar nova senha") },
            singleLine = true,
            enabled = fieldsEnabled,
            visualTransformation = if (confirmationVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { confirmationVisible = !confirmationVisible }, enabled = fieldsEnabled) {
                    Text(if (confirmationVisible) "Ocultar" else "Mostrar")
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Use ao menos 8 caracteres com maiúscula, minúscula, número e símbolo.",
            style = MaterialTheme.typography.bodySmall,
        )

        state.errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(24.dp))

        if (state.passwordChanged) {
            Text("Voltando para o login…")
        } else {
            PrimaryButton(
                text = if (state.isSaving) "Salvando…" else "Salvar nova senha",
                onClick = { onSave(password, confirmation) },
                enabled = fieldsEnabled,
            )
            TextButton(onClick = onFinish, enabled = !state.isSaving) {
                Text("Cancelar recuperação")
            }
        }
    }
}

@Composable
fun RecoveryStatusScreen(
    message: String,
    onRetry: (() -> Unit)? = null,
    onExit: (() -> Unit)? = null,
) {
    DsScreen(title = "Recuperar senha") {
        Text(message)
        Spacer(Modifier.height(24.dp))
        onRetry?.let { PrimaryButton("Tentar novamente", it) }
        onExit?.let { TextButton(onClick = it) { Text("Voltar para o login") } }
    }
}
