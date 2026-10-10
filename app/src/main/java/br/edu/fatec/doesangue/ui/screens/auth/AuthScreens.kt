package br.edu.fatec.doesangue.ui.screens.auth

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import br.edu.fatec.doesangue.presentation.auth.LoginUiState
import br.edu.fatec.doesangue.R
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
            visualTransformation = PasswordVisualTransformation(),
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
fun RegisterStep1Screen(onBack: () -> Unit, onContinue: () -> Unit, onLogin: () -> Unit) {
    DsScreen(onBack = onBack) {
        StepProgress(1)
        Spacer(Modifier.height(34.dp))
        ScreenHeading("Crie sua conta", "Vamos começar com algumas informações.")
        Spacer(Modifier.height(34.dp))
        VisualField("Nome completo", "Digite seu nome completo")
        Spacer(Modifier.height(16.dp))
        VisualField("E-mail", "seu@email.com")
        Spacer(Modifier.height(16.dp))
        VisualField("Data de nascimento", "dd/mm/aaaa")
        Spacer(Modifier.height(38.dp))
        PrimaryButton("Continuar", onContinue)
        Text("●  ○  ○  ○", Modifier.fillMaxWidth().padding(18.dp), color = BloodRed, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Já tem uma conta?", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            TextAction("Entrar", onLogin)
        }
    }
}

@Composable
fun RegisterStep2Screen(onBack: () -> Unit, onContinue: () -> Unit) {
    DsScreen(onBack = onBack) {
        StepProgress(2)
        Spacer(Modifier.height(34.dp))
        ScreenHeading("Como podemos falar com você?")
        Spacer(Modifier.height(34.dp))
        VisualField("Telefone", "(19) 99999-9999")
        Spacer(Modifier.height(18.dp))
        VisualField("Cidade", "Americana")
        Spacer(Modifier.height(18.dp))
        VisualField("Estado", "São Paulo")
        Spacer(Modifier.height(56.dp))
        PrimaryButton("Continuar", onContinue)
        Text("●  ●  ○  ○", Modifier.fillMaxWidth().padding(18.dp), color = BloodRed, textAlign = TextAlign.Center)
    }
}

@Composable
fun RegisterStep3Screen(onBack: () -> Unit, onContinue: () -> Unit) {
    DsScreen(onBack = onBack) {
        StepProgress(3)
        Spacer(Modifier.height(26.dp))
        ScreenHeading("Sobre sua doação")
        Spacer(Modifier.height(22.dp))
        Text("Tipo sanguíneo", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        val types = listOf("O+", "O−", "A+", "A−", "B+", "B−", "AB+", "AB−")
        types.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { type ->
                    SecondaryButton(type, {}, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Já doou sangue antes?", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth()) {
            SecondaryButton("Sim", {}, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            SecondaryButton("Ainda não", {}, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        VisualField("Data da última doação", "20/06/2026")
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Continuar", onContinue)
    }
}

@Composable
fun RegisterStep4Screen(onBack: () -> Unit, onCreate: () -> Unit) {
    DsScreen(onBack = onBack) {
        StepProgress(4)
        Spacer(Modifier.height(42.dp))
        ScreenHeading("Quase pronto!")
        Spacer(Modifier.height(36.dp))
        SelectionCard("Li e aceito os Termos de Uso", selected = true)
        Spacer(Modifier.height(14.dp))
        SelectionCard("Li e aceito a Política de Privacidade", selected = true)
        Spacer(Modifier.height(28.dp))
        SelectionCard("Importante", "O aplicativo não substitui a triagem realizada pelo hemocentro.", selected = false)
        Spacer(Modifier.height(70.dp))
        PrimaryButton("Criar minha conta", onCreate)
    }
}

@Composable
fun EmailVerificationScreen(onUnderstood: () -> Unit) {
    DsScreen {
        Spacer(Modifier.height(150.dp))
        CenteredMessage("✉", "Verifique seu e-mail", "Enviamos um link de confirmação para o endereço informado.")
        Spacer(Modifier.height(100.dp))
        PrimaryButton("Entendi", onUnderstood)
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    DsScreen(title = "Recuperar senha", onBack = onBack) {
        Spacer(Modifier.height(42.dp))
        CenteredMessage("✉", "Recuperar senha", "Informe seu e-mail para receber o link de recuperação.")
        Spacer(Modifier.height(36.dp))
        VisualField("E-mail", "seu@email.com")
        Spacer(Modifier.height(28.dp))
        PrimaryButton("Enviar link", {})
        TextAction("Voltar para o login", onBack, Modifier.align(Alignment.CenterHorizontally))
    }
}
