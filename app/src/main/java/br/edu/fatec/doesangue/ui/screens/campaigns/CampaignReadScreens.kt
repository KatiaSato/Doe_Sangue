package br.edu.fatec.doesangue.ui.screens.campaigns

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.Campaign
import br.edu.fatec.doesangue.presentation.campaigns.CampaignDetailUiState
import br.edu.fatec.doesangue.presentation.campaigns.CampaignsUiState
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.InfoCard
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CampaignsReadScreen(
    state: CampaignsUiState, onSelectBloodType: (BloodType?) -> Unit, onRetry: () -> Unit,
    onBack: () -> Unit, onDetail: (String) -> Unit, onNavigate: (AppRoute) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    DsScreen(title = "Campanhas", onBack = onBack, bottomRoute = AppRoute.Home, onNavigate = onNavigate) {
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("Tipo sanguíneo: ${state.selectedBloodType?.label ?: "Todos"}")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Todos") }, onClick = {
                    expanded = false; onSelectBloodType(null)
                })
                BloodType.knownTypes.forEach { type ->
                    DropdownMenuItem(text = { Text(type.label) }, onClick = {
                        expanded = false; onSelectBloodType(type)
                    })
                }
            }
        }
        Text("Campanhas gerais também aparecem em cada filtro.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        when {
            state.isLoading -> Text("Carregando campanhas…")
            state.errorMessage != null -> {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
                PrimaryButton("Tentar novamente", onRetry)
            }
            state.campaigns.isEmpty() -> Text("Nenhuma campanha encontrada para este filtro.")
            else -> state.campaigns.forEach { campaign ->
                InfoCard {
                    Text(campaign.title, style = MaterialTheme.typography.titleMedium)
                    Text(campaign.centerName)
                    CampaignPeriod(campaign)
                    Text(typeLabel(campaign))
                    PrimaryButton("Ver campanha", { onDetail(campaign.id) })
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun CampaignDetailReadScreen(
    state: CampaignDetailUiState, onRetry: () -> Unit, onBack: () -> Unit,
    onSchedule: () -> Unit, onNavigate: (AppRoute) -> Unit,
) {
    DsScreen(title = "Detalhe da campanha", onBack = onBack,
        bottomRoute = AppRoute.Home, onNavigate = onNavigate) {
        when (state) {
            CampaignDetailUiState.Loading -> Text("Carregando campanha…")
            CampaignDetailUiState.NotFound -> Text("Campanha não encontrada ou indisponível.")
            is CampaignDetailUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                PrimaryButton("Tentar novamente", onRetry)
            }
            is CampaignDetailUiState.Loaded -> {
                val campaign = state.campaign
                Text(campaign.title, style = MaterialTheme.typography.headlineSmall)
                Text(campaign.centerName)
                CampaignPeriod(campaign)
                Text(typeLabel(campaign))
                Spacer(Modifier.height(16.dp))
                Text(campaign.description)
                Text("Fonte: ${campaign.source}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Quero agendar uma doação", onSchedule)
            }
        }
    }
}

@Composable
private fun CampaignPeriod(campaign: Campaign) {
    val zone = ZoneId.of(campaign.timeZoneId)
    val format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    Text("${campaign.startsAt.atZone(zone).format(format)} até ${campaign.endsAt.atZone(zone).format(format)}")
}

private fun typeLabel(campaign: Campaign): String = if (campaign.bloodTypes.isEmpty()) {
    "Campanha geral"
} else "Tipos divulgados: ${campaign.bloodTypes.joinToString { it.label }}"
