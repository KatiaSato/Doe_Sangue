package br.edu.fatec.doesangue.ui.screens.stock

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.BloodStock
import br.edu.fatec.doesangue.domain.model.StockLevel
import br.edu.fatec.doesangue.presentation.common.ReadUiState
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.InfoCard
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.Success
import br.edu.fatec.doesangue.ui.theme.Warning
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BloodStockScreen(
    state: ReadUiState<List<BloodStock>>, onRetry: () -> Unit,
    onBack: () -> Unit, onNavigate: (AppRoute) -> Unit,
) {
    DsScreen(title = "Estoque de sangue", onBack = onBack,
        bottomRoute = AppRoute.Home, onNavigate = onNavigate) {
        Text("Demonstração: níveis fictícios por unidade e tipo sanguíneo.")
        Text("Não representam disponibilidade real. Fonte e atualização são informadas em cada registro.",
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        when (state) {
            ReadUiState.Loading -> Text("Carregando estoque…")
            is ReadUiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                PrimaryButton("Tentar novamente", onRetry)
            }
            is ReadUiState.Loaded -> {
                if (state.data.isEmpty()) Text("Nenhum nível de estoque publicado.")
                state.data.forEach { StockCard(it) }
            }
        }
    }
}

@Composable
fun StockCard(stock: BloodStock) {
    InfoCard {
        Text(stock.centerName, style = MaterialTheme.typography.titleMedium)
        val color = when (stock.level) {
            StockLevel.STABLE -> Success
            StockLevel.ALERT -> Warning
            StockLevel.CRITICAL -> BloodRed
        }
        Text("${stock.bloodType.label} • ${stock.level.label}", color = color)
        Text("Dado fictício • Fonte: ${stock.source}", style = MaterialTheme.typography.bodySmall)
        Text("Atualizado em " + stock.updatedAt.atZone(ZoneId.of(stock.timeZoneId))
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(10.dp))
}
