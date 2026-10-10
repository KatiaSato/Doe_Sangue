package br.edu.fatec.doesangue.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import br.edu.fatec.doesangue.presentation.profile.ProfileUiState
import br.edu.fatec.doesangue.ui.components.PrimaryButton

// Exibe o resultado da consulta ao perfil.
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onRetry: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onCreateProfile: () -> Unit,
) {
    Column {
        when (uiState) {
            ProfileUiState.Loading -> {
                Text("Carregando perfil...")
            }

            is ProfileUiState.Loaded -> {
                Text(uiState.profile.displayName)
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