package br.edu.fatec.doesangue.ui.screens.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.presentation.auth.RegistrationUiState
import br.edu.fatec.doesangue.presentation.auth.RegistrationViewModel
import br.edu.fatec.doesangue.ui.components.BloodTypePicker
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.ScreenHeading
import br.edu.fatec.doesangue.ui.components.StepProgress
import br.edu.fatec.doesangue.ui.components.TextAction

@Composable
fun RegisterStep1Screen(
    state: RegistrationUiState, onNameChange: (String) -> Unit, onEmailChange: (String) -> Unit,
    onBack: () -> Unit, onContinue: () -> Unit, onLogin: () -> Unit,
) {
    DsScreen(onBack = onBack) {
        StepProgress(1)
        Spacer(Modifier.height(24.dp))
        ScreenHeading("Crie sua conta", "Informe os dados que serão usados no aplicativo.")
        OutlinedTextField(state.displayName, onNameChange, label = { Text("Nome de exibição") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(state.email, onEmailChange, label = { Text("E-mail") },
            singleLine = true, keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Email,
            ),
            modifier = Modifier.fillMaxWidth())
        RegistrationError(state.errorMessage)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Continuar", onContinue)
        TextAction("Já tenho uma conta", onLogin)
    }
}

@Composable
fun RegisterStep2Screen(
    state: RegistrationUiState, onSelect: (BloodType) -> Unit, onBack: () -> Unit, onContinue: () -> Unit,
) {
    DsScreen(onBack = onBack) {
        StepProgress(2)
        Spacer(Modifier.height(24.dp))
        ScreenHeading("Seu tipo sanguíneo", "Escolha uma opção. Se não souber, selecione Não sei.")
        BloodTypePicker(state.bloodType, onSelect)
        Text("Informação autodeclarada; não substitui a avaliação do serviço de saúde.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Continuar", onContinue, enabled = state.bloodType != null)
    }
}

@Composable
fun RegisterStep3Screen(state: RegistrationUiState, onBack: () -> Unit, onContinue: () -> Unit) {
    DsScreen(onBack = onBack) {
        StepProgress(3)
        Spacer(Modifier.height(24.dp))
        ScreenHeading("Revise seus dados")
        Text("Nome: ${state.displayName.trim()}")
        Text("E-mail: ${state.email.trim()}")
        Text("Tipo sanguíneo: ${state.bloodType?.label.orEmpty()}")
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Continuar", onContinue, enabled = state.bloodType != null)
    }
}

@Composable
fun RegisterStep4Screen(
    state: RegistrationUiState, onBack: () -> Unit, onCreate: (String, String) -> Unit,
) {
    // Credenciais apenas na memória local da tela; não salvar/restaurar em disco.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    BackHandler(enabled = state.isLoading) {}
    DsScreen(onBack = if (state.isLoading) null else onBack) {
        StepProgress(4)
        Spacer(Modifier.height(24.dp))
        ScreenHeading("Proteja sua conta")
        RegistrationPassword("Senha", password, { password = it }, !state.isLoading)
        Spacer(Modifier.height(12.dp))
        RegistrationPassword("Confirmar senha", confirmation, { confirmation = it }, !state.isLoading)
        Text(RegistrationViewModel.PASSWORD_RULE, style = MaterialTheme.typography.bodySmall,
            color = if (state.errorMessage == RegistrationViewModel.PASSWORD_RULE)
                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        RegistrationError(state.errorMessage?.takeUnless { it == RegistrationViewModel.PASSWORD_RULE })
        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            if (state.isLoading) "Criando conta…" else "Criar minha conta",
            onClick = { onCreate(password, confirmation) },
            enabled = !state.isLoading && state.result == null,
        )
    }
}

@Composable
private fun RegistrationPassword(label: String, value: String, onChange: (String) -> Unit, enabled: Boolean) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        enabled = enabled, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { TextButton(onClick = { visible = !visible }, enabled = enabled) {
            Text(if (visible) "Ocultar" else "Mostrar")
        } },
    )
}

@Composable
private fun RegistrationError(message: String?) {
    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
