package br.edu.fatec.doesangue.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.presentation.common.ReadUiState
import br.edu.fatec.doesangue.presentation.home.HomeUiState
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRedStrong
import br.edu.fatec.doesangue.ui.theme.InkSecondary

@Composable
fun HomeScreen(state: HomeUiState, onRetry: () -> Unit, onNavigate: (AppRoute) -> Unit) {
    DsScreen(
        bottomRoute = AppRoute.Home,
        onNavigate = onNavigate,
        bottomBarContent = { HomeBottomBar(onNavigate) },
    ) {
        Column(Modifier.padding(top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val profile = state.profile
            HomeHeader((profile as? ReadUiState.Loaded)?.data?.displayName)
            if (profile is ReadUiState.Error) HomeError(profile.message, onRetry)
            HomeScheduleCard { onNavigate(AppRoute.ScheduleCenter) }
            when (val appointment = state.appointment) {
                ReadUiState.Loading -> HomeSupportingText("Consultando agendamentos…")
                is ReadUiState.Error -> HomeError(appointment.message, onRetry)
                is ReadUiState.Loaded -> {
                    val next = appointment.data
                    if (next == null) HomeSupportingText("Você não possui agendamentos futuros ativos.")
                    else HomeAppointmentCard(next) { onNavigate(AppRoute.AppointmentDetailById(next.appointment.id)) }
                }
            }
            HomePreparationCard { onNavigate(AppRoute.Restrictions) }
            Column {
                HomeSectionHeader("Estoque de sangue", "Ver todos") { onNavigate(AppRoute.BloodStock) }
                when (val stocks = state.stocks) {
                    ReadUiState.Loading -> HomeSupportingText("Carregando estoque…")
                    is ReadUiState.Error -> HomeError(stocks.message, onRetry)
                    is ReadUiState.Loaded -> {
                        if (stocks.data.isEmpty()) HomeSupportingText("Nenhum nível de estoque publicado.")
                        else HomeStockPreview(stocks.data)
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeSectionHeader("Campanhas", "Ver todas") { onNavigate(AppRoute.Campaigns) }
                when (val campaigns = state.campaigns) {
                    ReadUiState.Loading -> HomeSupportingText("Carregando campanhas…")
                    is ReadUiState.Error -> HomeError(campaigns.message, onRetry)
                    is ReadUiState.Loaded -> {
                        if (campaigns.data.isEmpty()) HomeSupportingText("Nenhuma campanha publicada.")
                        campaigns.data.take(2).forEach { campaign ->
                            HomeCampaignCard(campaign) { onNavigate(AppRoute.CampaignDetailById(campaign.id)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSupportingText(text: String) {
    Text(text, style = HomeStyle.Supporting, color = InkSecondary)
}

@Composable
private fun HomeError(message: String, onRetry: () -> Unit) {
    Column {
        Text(message, style = HomeStyle.Supporting, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) {
            Text("Tentar novamente", style = HomeStyle.Supporting, color = BloodRedStrong)
        }
    }
}
