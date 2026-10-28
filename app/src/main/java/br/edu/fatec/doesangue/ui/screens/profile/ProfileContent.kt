package br.edu.fatec.doesangue.ui.screens.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.util.Locale
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.ui.theme.BloodRedSoft
import br.edu.fatec.doesangue.ui.theme.BloodRedStrong
import br.edu.fatec.doesangue.ui.theme.Border
import br.edu.fatec.doesangue.ui.theme.Ink
import br.edu.fatec.doesangue.ui.theme.InkSecondary
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import br.edu.fatec.doesangue.presentation.profile.ProfileUiState
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.BloodTypePicker
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.validation.ProfileValidator
import br.edu.fatec.doesangue.ui.components.SecondaryButton

// Exibe o resultado da consulta ao perfil.
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onRetry: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onCreateProfile: () -> Unit,
    onBloodTypeChange: (BloodType) -> Unit,
    onSaveBloodType: () -> Unit,
    onStartBloodTypeEdit: () -> Unit,
    onCancelBloodTypeEdit: () -> Unit,
    enabled: Boolean = true,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (uiState) {
            ProfileUiState.Loading -> {
                Text("Carregando perfil...")
            }

            is ProfileUiState.Loaded -> {
                Surface(color = BloodRedSoft, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val words = uiState.profile.displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                        val initials = listOfNotNull(words.firstOrNull(), words.lastOrNull()?.takeIf { words.size > 1 })
                            .joinToString("") { it.take(1) }.uppercase(Locale.ROOT)
                        Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text(initials, color = BloodRedStrong, style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Text(uiState.profile.displayName, style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold, color = Ink, textAlign = TextAlign.Center)
                        Text("Tipo sanguíneo informado: ${uiState.profile.bloodType.label}",
                            color = BloodRedStrong, style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center)
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Seus dados", style = MaterialTheme.typography.titleSmall, color = Ink)
                    Text("Telefone: ${uiState.profile.phone ?: "Não informado"}",
                        style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                    Text("Nascimento: ${uiState.profile.birthDate?.format(ProfileValidator.birthDateFormat) ?: "Não informado"}",
                        style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
                }
                if (uiState.isEditingBloodType) {
                    BloodTypePicker(uiState.selectedBloodType, onBloodTypeChange,
                        enabled = enabled && !uiState.isSavingBloodType)
                    Text("Informação autodeclarada; não determina aptidão.",
                        style = MaterialTheme.typography.bodySmall)
                    uiState.bloodTypeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    PrimaryButton(
                        if (uiState.isSavingBloodType) "Salvando tipo…" else "Salvar tipo sanguíneo",
                        onSaveBloodType,
                        enabled = enabled && !uiState.isSavingBloodType && uiState.selectedBloodType != uiState.profile.bloodType,
                    )
                    SecondaryButton("Cancelar", onCancelBloodTypeEdit, enabled = enabled && !uiState.isSavingBloodType)
                } else {
                    ProfileMenuRow("Alterar tipo sanguíneo", R.drawable.home_ic_drop, onStartBloodTypeEdit, enabled = enabled)
                }
            }

            is ProfileUiState.Missing -> {
                Text("Você ainda não criou seu perfil.")

                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text("Nome de exibição") },
                    singleLine = true,
                    enabled = enabled && !uiState.isSaving,
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
                    enabled = enabled && !uiState.isSaving,
                )
            }

            is ProfileUiState.Error -> {
                Text(uiState.message)
                PrimaryButton(
                    text = "Tentar novamente",
                    onClick = onRetry,
                    enabled = enabled,
                )
            }
        }
    }
}

@Composable
internal fun ProfileMenuRow(
    label: String,
    icon: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column {
        Row(Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp).padding(vertical = 16.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(painterResource(icon), contentDescription = null,
                modifier = Modifier.size(24.dp), tint = if (enabled) BloodRedStrong else InkSecondary)
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) Ink else InkSecondary)
            Text("›", color = InkSecondary, style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(color = Border)
    }
}
