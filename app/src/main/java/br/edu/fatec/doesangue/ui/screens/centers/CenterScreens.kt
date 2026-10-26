package br.edu.fatec.doesangue.ui.screens.centers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.presentation.centers.CenterDetailUiState
import br.edu.fatec.doesangue.presentation.centers.CentersUiState
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.InfoCard
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.SecondaryButton
import br.edu.fatec.doesangue.ui.components.SectionHeader
import br.edu.fatec.doesangue.ui.components.VisualField
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.Ink
import br.edu.fatec.doesangue.ui.theme.InkSecondary

@Composable
fun CentersScreen(
    state: CentersUiState,
    onSearchQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onDetail: (String) -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {    DsScreen(title = "Unidades", onBack = onBack, bottomRoute = AppRoute.Centers, onNavigate = onNavigate) {
    OutlinedTextField(
        value = state.searchQuery,
        onValueChange = onSearchQueryChange,
        label = { Text("Buscar por nome ou cidade") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
    Spacer(Modifier.height(16.dp))
    if (state.isLoading) {
        Text("Carregando unidades…")
        return@DsScreen
    }

    val errorMessage = state.errorMessage
    if (errorMessage != null) {
        Text(errorMessage, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Tentar novamente", onRetry)
        return@DsScreen
    }

    if (state.centers.isEmpty()) {
        Text(
            if (state.searchQuery.isBlank()) {
                "Nenhuma unidade disponível no momento."
            } else {
                "Nenhuma unidade encontrada para esta busca."
            },
        )
        return@DsScreen
    }
    state.centers.forEach { center ->
        InfoCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDetail(center.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        center.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                    )
                    Text(center.city, color = InkSecondary)
                    Text(
                        center.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                    )
                }
                Text(
                    "›",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BloodRed,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun CenterDetailScreen(onBack: () -> Unit, onSchedule: () -> Unit, onNavigate: (AppRoute) -> Unit) {
    DsScreen(title = "Hemocentro Campinas", onBack = onBack, bottomRoute = AppRoute.Centers, onNavigate = onNavigate) {
        Spacer(Modifier.height(12.dp))
        InfoCard {
            Text("Hemocentro Campinas", style = MaterialTheme.typography.titleLarge)
            Text("Rua Carlos Chagas, 480\nCampinas/SP", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("(19) XXXX-XXXX", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text("4,8 km", style = MaterialTheme.typography.bodyMedium, color = BloodRed)
            }
        }
        Spacer(Modifier.height(30.dp))
        SectionHeader("Horários de atendimento")
        InfoCard {
            ScheduleRow("Segunda a sexta", "07:30 — 15:00")
            ScheduleRow("Sábado", "07:30 — 12:00")
        }
        Spacer(Modifier.height(22.dp))
        InfoCard {
            Text("Tipos de atendimento", style = MaterialTheme.typography.titleMedium)
            Text("Doação de sangue total", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        }
        Spacer(Modifier.height(50.dp))
        PrimaryButton("Agendar nesta unidade", onSchedule)
    }
}

@Composable
private fun ScheduleRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun CenterDetailReadScreen(
    state: CenterDetailUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onSchedule: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onOpenMap: (DonationCenter) -> Unit,
) {
    DsScreen(
        title = "Detalhe da unidade",
        onBack = onBack,
        bottomRoute = AppRoute.Centers,
        onNavigate = onNavigate,
    ) {
        when (state) {
            CenterDetailUiState.Loading ->
                Text("Carregando unidade…")

            CenterDetailUiState.NotFound ->
                Text("Unidade não encontrada ou indisponível.")

            is CenterDetailUiState.Error -> {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Tentar novamente", onRetry)
            }

            is CenterDetailUiState.Loaded -> {
                val center = state.center

                InfoCard {
                    Text(
                        center.name,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(center.city, color = InkSecondary)
                    Text(center.address, color = InkSecondary)

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Telefone",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(center.phone.ifBlank { "Não informado." })
                }

                Spacer(Modifier.height(20.dp))
                SectionHeader("Horários de atendimento")
                InfoCard {
                    Text(
                        center.openingHours.ifBlank {
                            "Horários não informados."
                        },
                    )
                }

                Spacer(Modifier.height(20.dp))
                SectionHeader("Localização")

                if (center.coordinates != null) {
                    Text(
                        "Posição fictícia para demonstração.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary,
                    )
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton(
                        "Abrir no mapa",
                        { onOpenMap(center) },
                    )
                } else {
                    Text("Localização não cadastrada.")
                }

                Spacer(Modifier.height(24.dp))
                PrimaryButton("Agendar doação", onSchedule)
            }
        }
    }
}
