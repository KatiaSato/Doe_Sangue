package br.edu.fatec.doesangue.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import br.edu.fatec.doesangue.presentation.profile.ProfileUiState
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.BloodTypePicker
import br.edu.fatec.doesangue.domain.model.BloodType

// Exibe o resultado da consulta ao perfil.
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onRetry: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onBloodTypeChange: (BloodType) -> Unit,
    onSaveBloodType: () -> Unit,
) {
    Column {
        when (uiState) {
            ProfileUiState.Loading -> {
                Text("Carregando perfil...")
            }

            is ProfileUiState.Loaded -> {
                Text(uiState.profile.displayName)
                Text("Tipo sanguíneo informado: ${uiState.profile.bloodType.label}")
                BloodTypePicker(uiState.selectedBloodType, onBloodTypeChange,
                    enabled = !uiState.isSavingBloodType)
                Text("Informação autodeclarada; não determina aptidão.",
                    style = MaterialTheme.typography.bodySmall)
                uiState.bloodTypeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                PrimaryButton(
                    if (uiState.isSavingBloodType) "Salvando tipo…" else "Salvar tipo sanguíneo",
                    onSaveBloodType,
                    enabled = !uiState.isSavingBloodType && uiState.selectedBloodType != uiState.profile.bloodType,
                )
            }

            is ProfileUiState.Missing -> {
                Text("Você ainda não criou seu perfil.")

                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Nome de exibição") },
                    singleLine = true,
                    enabled = !uiState.isSaving,
                    isError = uiState.errorMessage != null,
                )

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                PrimaryButton(
                    text = if (uiState.isSaving) "Salvando..." else "Criar perfil",
                    onClick = onCreateProfile,
                    enabled = !uiState.isSaving,
                )
            }

            is ProfileUiState.Error -> {
                Text(uiState.message)
                PrimaryButton(
                    text = "Tentar novamente",
                    onClick = onRetry,
                )
            }
        }
    }
}
