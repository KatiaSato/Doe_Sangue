package br.edu.fatec.doesangue.ui.screens.donations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.InfoCard
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.SectionHeader
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.BloodRedSoft
import br.edu.fatec.doesangue.ui.theme.Ink
import br.edu.fatec.doesangue.ui.theme.InkSecondary

@Composable
fun DonationHistoryScreen(onBack: () -> Unit, onAppointments: () -> Unit, onNavigate: (AppRoute) -> Unit) {
    DsScreen(title = "Minhas doações", onBack = onBack, bottomRoute = AppRoute.DonationsHistory, onNavigate = onNavigate) {
        DonationTabs(history = true, onHistory = {}, onAppointments = onAppointments)
        Spacer(Modifier.height(18.dp))
        listOf(
            "20/06/2026" to "Hemocentro Campinas",
            "15/03/2024" to "Hemocentro Paulínia",
            "10/10/2023" to "Hemocentro Americana",
            "12/06/2023" to "Hemocentro Campinas",
        ).forEach { (date, center) ->
            InfoCard {
                Row(Modifier.fillMaxWidth()) {
                    Text("♦", color = BloodRed)
                    Spacer(Modifier.padding(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(date, style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(center, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                    }
                    Text("Doação realizada", style = MaterialTheme.typography.bodySmall, color = BloodRed)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Card(colors = CardDefaults.cardColors(containerColor = BloodRedSoft), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Total de doações", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text("04", style = MaterialTheme.typography.headlineMedium, color = BloodRed)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun DonationAppointmentsScreen(onBack: () -> Unit, onHistory: () -> Unit, onDetails: () -> Unit, onNavigate: (AppRoute) -> Unit) {
    DsScreen(title = "Minhas doações", onBack = onBack, bottomRoute = AppRoute.DonationsAppointments, onNavigate = onNavigate) {
        DonationTabs(history = false, onHistory = onHistory, onAppointments = {})
        Spacer(Modifier.height(22.dp))
        Text("PRÓXIMA DOAÇÃO", style = MaterialTheme.typography.labelSmall, color = BloodRed)
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = BloodRed),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onDetails),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("12 de setembro • 14:00", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("Hemocentro Campinas", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text("Rotina: a cada 3 meses", style = MaterialTheme.typography.bodySmall, color = Color.White)
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Ver detalhes", onDetails)
            }
        }
        Spacer(Modifier.height(28.dp))
        SectionHeader("Próximos lembretes")
        InfoCard {
            Text("12/12/2026", style = MaterialTheme.typography.titleMedium)
            Text("Próxima data sugerida", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        }
        Spacer(Modifier.height(10.dp))
        InfoCard {
            Text("12/03/2027", style = MaterialTheme.typography.titleMedium)
            Text("Próxima data sugerida", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        }
    }
}

@Composable
private fun DonationTabs(history: Boolean, onHistory: () -> Unit, onAppointments: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            "Histórico",
            Modifier.weight(1f).clickable(onClick = onHistory).padding(12.dp),
            textAlign = TextAlign.Center,
            color = if (history) BloodRed else InkSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            "Agendamentos",
            Modifier.weight(1f).clickable(onClick = onAppointments).padding(12.dp),
            textAlign = TextAlign.Center,
            color = if (!history) BloodRed else InkSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
