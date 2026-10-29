package br.edu.fatec.doesangue.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.domain.model.BloodStock
import br.edu.fatec.doesangue.domain.model.StockLevel
import br.edu.fatec.doesangue.ui.components.foreground
import br.edu.fatec.doesangue.ui.components.softBackground
import br.edu.fatec.doesangue.ui.theme.InkSecondary

@Composable
internal fun HomeStockPreview(stocks: List<BloodStock>, onLevel: (StockLevel) -> Unit = {}) {
    if (stocks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Consulte as unidades por nível de estoque.", style = HomeStyle.Supporting, color = InkSecondary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(StockLevel.CRITICAL, StockLevel.ALERT, StockLevel.STABLE).forEach { level ->
                val resource = when (level) {
                    StockLevel.CRITICAL -> R.drawable.refine_stock_bag_critical
                    StockLevel.ALERT -> R.drawable.refine_stock_bag_alert
                    StockLevel.STABLE -> R.drawable.refine_stock_bag_stable
                }
                Column(Modifier.weight(1f).background(level.softBackground(), HomeStyle.TileShape)
                    .clickable(role = Role.Button, onClickLabel = "Consultar estoques: ${level.label}", onClick = { onLevel(level) })
                    .padding(horizontal = 6.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Image(painterResource(resource), null, Modifier.size(32.dp))
                    Text(level.label, style = HomeStyle.Label, color = level.foreground())
                    Text("Ver unidades", style = HomeStyle.Caption, color = level.foreground())
                }
            }
        }
        Text("Dados demonstrativos · níveis por tipo sanguíneo", style = HomeStyle.Caption, color = InkSecondary)
    }
}
