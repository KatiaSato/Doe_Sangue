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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
fun CentersScreen(onBack: () -> Unit, onDetail: () -> Unit, onNavigate: (AppRoute) -> Unit) {
    DsScreen(title = "Unidades", onBack = onBack, bottomRoute = AppRoute.Centers, onNavigate = onNavigate) {
        VisualField("", "⌕  Buscar unidade")
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Ver no mapa", {})
        Spacer(Modifier.height(20.dp))
        listOf(
            listOf("Hemocentro Campinas", "Rua Carlos Chagas, 480", "(19) XXXX-XXXX", "4,8 km"),
            listOf("Hemocentro Paulínia", "Av. José Paulino, 123", "(19) XXXX-XXXX", "12,3 km"),
            listOf("Hemocentro Americana", "Rua Vital Brasil, 200", "(19) XXXX-XXXX", "15,7 km"),
        ).forEach { center ->
            InfoCard {
                Row(Modifier.fillMaxWidth().clickable(onClick = onDetail), verticalAlignment = Alignment.CenterVertically) {
                    Text("○", color = BloodRed, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(center[0], style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(center[1], style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                        Text(center[2], style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(center[3], style = MaterialTheme.typography.bodySmall, color = BloodRed)
                        Text("›", style = MaterialTheme.typography.headlineMedium, color = BloodRed)
                    }
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
