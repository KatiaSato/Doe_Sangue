package br.edu.fatec.doesangue.ui.screens.campaigns

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
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
fun CampaignsScreen(onBack: () -> Unit, onDetail: () -> Unit, onDonate: () -> Unit, onNavigate: (AppRoute) -> Unit) {
    DsScreen(title = "Campanhas", onBack = onBack, bottomRoute = AppRoute.Home, onNavigate = onNavigate) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BloodRedSoft),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("♦", color = BloodRed, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Precisamos de doadores O−", style = MaterialTheme.typography.titleMedium, color = BloodRed)
                    Text("O estoque está em nível crítico.", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
                }
                Text("Quero doar", Modifier.clickable(onClick = onDonate), style = MaterialTheme.typography.labelSmall, color = BloodRed)
            }
        }
        Spacer(Modifier.height(20.dp))
        CampaignCard("Doe sangue, doe vida!", "Campanha de incentivo à doação de sangue.", onDetail)
        Spacer(Modifier.height(12.dp))
        CampaignCard("Junho Vermelho", "Mês de conscientização sobre a doação.", onDetail)
        Spacer(Modifier.height(12.dp))
        CampaignCard("Universidade pela vida", "Campanha especial para novos doadores.", onDetail)
    }
}

@Composable
private fun CampaignCard(title: String, body: String, onClick: () -> Unit) {
    InfoCard {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.figma_raw_6),
                contentDescription = "Campanha de doação",
                modifier = Modifier.size(82.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(body, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
        }
    }
}

@Composable
fun CampaignDetailScreen(onBack: () -> Unit, onSchedule: () -> Unit, onNavigate: (AppRoute) -> Unit) {
    DsScreen(title = "Campanha", onBack = onBack, bottomRoute = AppRoute.Home, onNavigate = onNavigate) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BloodRed),
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Doe sangue, doe vida!", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("Campanha de incentivo à doação de sangue.", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(Modifier.height(14.dp))
                    Text("05/09/2026 • Hemocentro Campinas", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                Image(
                    painter = painterResource(R.drawable.figma_raw_1),
                    contentDescription = "Ícone Doe Sangue",
                    modifier = Modifier.size(74.dp),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(Modifier.height(30.dp))
        SectionHeader("Sobre a campanha")
        Text(
            "Uma ação para incentivar novos doadores e reforçar os estoques do hemocentro. Compartilhe com amigos e familiares.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
        )
        Spacer(Modifier.height(24.dp))
        Card(colors = CardDefaults.cardColors(containerColor = BloodRedSoft), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Estoque prioritário", style = MaterialTheme.typography.titleMedium)
                Text("O− • nível crítico", style = MaterialTheme.typography.bodyMedium, color = BloodRed)
            }
        }
        Spacer(Modifier.height(52.dp))
        PrimaryButton("Quero agendar uma doação", onSchedule)
    }
}
