package br.edu.fatec.doesangue.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.presentation.profile.EditProfileUiState
import br.edu.fatec.doesangue.ui.components.BloodTypePicker
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.SecondaryButton

@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onBloodTypeChange: (BloodType) -> Unit,
    onPhoneChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onRetry: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    val ready = state as? EditProfileUiState.Ready
    val busy = ready?.isSaving == true || ready?.saved == true
    val context = LocalContext.current
    val finish by rememberUpdatedState(onBack)
    LaunchedEffect(ready?.saved) {
        if (ready?.saved == true) {
            Toast.makeText(context, "Perfil atualizado com sucesso.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    BackHandler(enabled = busy) { }
    DsScreen(title = "Editar perfil", onBack = if (busy) null else onBack) {
        Spacer(Modifier.height(12.dp))
        when (state) {
            EditProfileUiState.Loading -> Text("Carregando perfil…")
            EditProfileUiState.Missing -> Text("Seu perfil não foi encontrado. Volte ao Perfil para criá-lo.")
            is EditProfileUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                PrimaryButton("Tentar novamente", onRetry)
            }
            is EditProfileUiState.Ready -> {
                ProfileTextField("Nome de exibição", state.displayName, onNameChange,
                    state.nameError, !busy, KeyboardType.Text)
                Spacer(Modifier.height(16.dp))
                Text("Tipo sanguíneo", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                BloodTypePicker(state.bloodType, onBloodTypeChange, enabled = !busy)
                Text("Informação autodeclarada; não determina aptidão.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                ProfileTextField("Telefone com DDD (opcional)", state.phone, onPhoneChange,
                    state.phoneError, !busy, KeyboardType.Phone)
                Spacer(Modifier.height(16.dp))
                ProfileTextField("Data de nascimento (opcional)", birthDateDigits(state.birthDate),
                    { onBirthDateChange(formatBirthDateInput(it)) },
                    state.birthDateError, !busy, KeyboardType.Number, "DD/MM/AAAA", BirthDateMask)
                Spacer(Modifier.height(16.dp))
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                PrimaryButton(if (state.isSaving) "Salvando perfil…" else "Salvar alterações",
                    onSave, enabled = !busy && state.hasChanges)
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Cancelar", onBack, enabled = !busy)
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    enabled: Boolean,
    keyboardType: KeyboardType,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(value = value, onValueChange = onChange,
        label = { Text(label) }, placeholder = { Text(placeholder) },
        singleLine = true, enabled = enabled, isError = error != null,
        visualTransformation = visualTransformation,
        supportingText = error?.let { message -> { Text(message) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth())
}
