package br.edu.fatec.doesangue.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.InfoCard
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.SectionHeader
import br.edu.fatec.doesangue.ui.screens.restrictions.BeforeDonationCard
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.BloodRedSoft
import br.edu.fatec.doesangue.ui.theme.Border
import br.edu.fatec.doesangue.ui.theme.Ink
import br.edu.fatec.doesangue.ui.theme.InkSecondary
import br.edu.fatec.doesangue.ui.theme.Success
import br.edu.fatec.doesangue.ui.theme.SurfaceSoft
import br.edu.fatec.doesangue.ui.theme.Warning

@Composable
fun HomeScreen(onNavigate: (AppRoute) -> Unit) {
    DsScreen(bottomRoute = AppRoute.Home, onNavigate = onNavigate) {
        HomeHeader()
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth().height(166.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BloodRed),
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.figma_raw_4),
                    contentDescription = "Bolsa de sangue",
                    modifier = Modifier.size(110.dp),
                    contentScale = ContentScale.Fit,
                )
                Column(Modifier.weight(1f)) {
                    Text("Sua próxima doação", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Encontre um hemocentro e agende sua visita.", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton("Agendar doação   ▣", { onNavigate(AppRoute.ScheduleCenter) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        BeforeDonationCard { onNavigate(AppRoute.Restrictions) }
        Spacer(Modifier.height(12.dp))
        SectionHeader("Estoque de sangue", "Ver todos")
        BloodStockRow(listOf("O+" to "Estável", "O−" to "Crítico", "A+" to "Alerta", "A−" to "Crítico"))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                modifier = Modifier.weight(1f).height(150.dp).clickable { onNavigate(AppRoute.ScheduleCenter) },
                colors = CardDefaults.cardColors(containerColor = BloodRedSoft),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("♦", color = BloodRed, style = MaterialTheme.typography.headlineMedium)
                    Text("Precisamos de\ndoadores O−", style = MaterialTheme.typography.labelMedium, color = BloodRed)
                    Text("O estoque está em nível crítico na sua região.", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                }
            }
            Card(
                modifier = Modifier.weight(1f).height(150.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSoft),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Dica rápida  ♡", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Text("Beba bastante água, se alimente bem e tenha uma boa noite de sono antes de doar.", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                    Text("Saiba mais", style = MaterialTheme.typography.labelSmall, color = BloodRed)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionHeader("Próximas campanhas", "Ver todas") { onNavigate(AppRoute.Campaigns) }
        CampaignPreview { onNavigate(AppRoute.CampaignDetail) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun HomeScheduledScreen(onNavigate: (AppRoute) -> Unit) {
    DsScreen(bottomRoute = AppRoute.Home, onNavigate = onNavigate) {
        HomeHeader()
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BloodRed),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Doação agendada", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text("12 SET • 14:00", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("Hemocentro Campinas", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Ver agendamento", { onNavigate(AppRoute.AppointmentDetail) })
            }
        }
        Spacer(Modifier.height(18.dp))
        BeforeDonationCard { onNavigate(AppRoute.Restrictions) }
        Spacer(Modifier.height(12.dp))
        SectionHeader("Estoque de sangue")
        BloodStockRow(listOf("O+" to "Estável", "O−" to "Crítico", "A+" to "Alerta", "A−" to "Crítico"))
        BloodStockRow(listOf("B+" to "Estável", "B−" to "Alerta", "AB+" to "Estável", "AB−" to "Alerta"))
        Spacer(Modifier.height(14.dp))
        InfoCard {
            Text("Precisamos de doadores O−", style = MaterialTheme.typography.titleMedium, color = BloodRed)
            Text("O estoque está em nível crítico.", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        }
        Spacer(Modifier.height(16.dp))
        SectionHeader("Próxima campanha")
        CampaignPreview { onNavigate(AppRoute.CampaignDetail) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun HomeHeader() {
    Spacer(Modifier.height(24.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("☰", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Text("♧", style = MaterialTheme.typography.headlineMedium)
            Box(Modifier.size(8.dp).background(BloodRed, CircleShape).align(Alignment.TopEnd))
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("Olá, Mariana! 👋", style = MaterialTheme.typography.headlineMedium, color = Ink)
    Text("Que bom ver você por aqui!", style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
}

@Composable
private fun BloodStockRow(values: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { (type, state) ->
            Card(
                modifier = Modifier.weight(1f).height(90.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            ) {
                Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("♦", color = BloodRed)
                    Text(type, fontWeight = FontWeight.Bold, color = Ink)
                    val color = when (state) { "Estável" -> Success; "Alerta" -> Warning; else -> BloodRed }
                    Text(state, style = MaterialTheme.typography.bodySmall, color = color, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun CampaignPreview(onClick: () -> Unit) {
    InfoCard {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.figma_raw_6),
                contentDescription = "Campanha de doação",
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Doe sangue, doe vida", style = MaterialTheme.typography.titleMedium)
                Text("Hemocentro Campinas", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                Text("▣  05/09/2026", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
        }
    }
}
