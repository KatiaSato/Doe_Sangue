package br.edu.fatec.doesangue.ui.screens.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.presentation.auth.LogoutUiState
import br.edu.fatec.doesangue.presentation.profile.ProfileUiState
import br.edu.fatec.doesangue.ui.components.BloodTypePicker
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.SecondaryButton

@Composable
fun CompleteGoogleProfileScreen(
    state: ProfileUiState,
    logoutState: LogoutUiState,
    onNameChange: (String) -> Unit,
    onSave: (BloodType) -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
) {
    var bloodType by rememberSaveable { mutableStateOf(BloodType.UNKNOWN) }
    val busy = logoutState.isLoading || (state as? ProfileUiState.Missing)?.isSaving == true
    DsScreen(title = "Complete seu perfil") {
        Spacer(Modifier.height(24.dp))
        when (state) {
            ProfileUiState.Loading, is ProfileUiState.Loaded -> Text("Verificando seu perfil…")
            is ProfileUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Tentar novamente", onRetry, enabled = !busy)
            }
            is ProfileUiState.Missing -> {
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = onNameChange,
                    label = { Text("Nome de exibição") },
                    enabled = !busy,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Tipo sanguíneo")
                BloodTypePicker(bloodType, { bloodType = it }, enabled = !busy)
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    if (state.isSaving) "Salvando…" else "Salvar e continuar",
                    { onSave(bloodType) },
                    enabled = !busy,
                )
                if (state.errorMessage != null) {
                    SecondaryButton("Verificar perfil novamente", onRetry, enabled = !busy)
                }
            }
        }
        logoutState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Sair da conta", onSignOut, enabled = !busy && state !is ProfileUiState.Loaded)
        Spacer(Modifier.height(24.dp))
    }
}
