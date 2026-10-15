package br.edu.fatec.doesangue.ui.screens.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import br.edu.fatec.doesangue.domain.model.Campaign
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.theme.BloodRedStrong
import br.edu.fatec.doesangue.ui.theme.Border
import br.edu.fatec.doesangue.ui.theme.Ink
import br.edu.fatec.doesangue.ui.theme.InkSecondary
import br.edu.fatec.doesangue.ui.theme.SurfaceSoft
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val HomeLocale: Locale = Locale.forLanguageTag("pt-BR")

@Composable
internal fun HomeHeader(name: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HomeIcon(R.drawable.home_ic_drop, Modifier.size(18.dp))
                Text("DOE SANGUE", style = HomeStyle.Caption, color = BloodRedStrong)
            }
            Surface(color = SurfaceSoft, shape = HomeStyle.CardShape) {
                Text("Demonstração", Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    style = HomeStyle.Caption, color = InkSecondary)
            }
        }
        Text(if (name.isNullOrBlank()) "Olá!" else "Olá, $name",
            style = HomeStyle.Greeting, color = Ink)
        Text("Que bom ter você por aqui.", style = HomeStyle.Supporting, color = InkSecondary)
    }
}

@Composable
internal fun HomeScheduleCard(onSchedule: () -> Unit) {
    Card(shape = HomeStyle.CardShape,
        colors = CardDefaults.cardColors(containerColor = HomeStyle.Soft)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Um gesto seu.\nUma nova chance.", style = HomeStyle.Hero, color = Ink)
                    Text("Encontre uma unidade\ne agende sua visita.", style = HomeStyle.Supporting, color = InkSecondary)
                }
                Image(painterResource(R.drawable.figma_raw_4), contentDescription = null,
                    modifier = Modifier.size(width = 104.dp, height = 112.dp), contentScale = ContentScale.Fit)
            }
            PrimaryButton("Agendar doação", onSchedule, shape = HomeStyle.TileShape, containerColor = BloodRedStrong)
        }
    }
}

@Composable
internal fun HomeAppointmentCard(appointment: AppointmentSummary, onOpen: () -> Unit) {
    val local = appointment.startsAt.atZone(ZoneId.of(appointment.timeZoneId))
    HomeLinkedCard(onOpen, "Ver detalhes do agendamento") {
        Column(
            Modifier.widthIn(min = 48.dp).background(SurfaceSoft, HomeStyle.TileShape)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .semantics { contentDescription = local.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", HomeLocale)) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(local.format(DateTimeFormatter.ofPattern("dd")), style = HomeStyle.Hero, color = BloodRedStrong)
            Text(local.format(DateTimeFormatter.ofPattern("MMM", HomeLocale)).removeSuffix(".").uppercase(HomeLocale),
                style = HomeStyle.Caption, color = InkSecondary)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("Próximo agendamento", style = HomeStyle.Caption, color = InkSecondary)
            Text(appointment.centerName, style = HomeStyle.Label, color = Ink)
            Text(local.format(DateTimeFormatter.ofPattern("HH:mm")) + " · Ver detalhes",
                style = HomeStyle.Supporting, color = BloodRedStrong)
        }
        HomeIcon(R.drawable.home_ic_arrow_action, Modifier.size(20.dp))
    }
}

@Composable
internal fun HomePreparationCard(onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(SurfaceSoft, HomeStyle.CardShape)
            .clickable(role = Role.Button, onClickLabel = "Consultar restrições", onClick = onOpen)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomeIcon(R.drawable.home_ic_book)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("Antes de doar", style = HomeStyle.Label, color = Ink)
            Text("Veja orientações de preparo.", style = HomeStyle.Supporting, color = InkSecondary)
        }
        HomeIcon(R.drawable.home_ic_arrow, Modifier.size(20.dp))
    }
}

@Composable
internal fun HomeCampaignCard(campaign: Campaign, onOpen: () -> Unit) {
    HomeLinkedCard(onOpen, "Ver campanha") {
        Box(Modifier.width(72.dp).heightIn(min = 88.dp).background(HomeStyle.Soft, HomeStyle.TileShape),
            contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.figma_raw_6), contentDescription = null,
                modifier = Modifier.size(width = 72.dp, height = 80.dp), contentScale = ContentScale.Fit)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            val category = if (campaign.bloodTypes.isEmpty()) "Campanha geral"
                else "Tipos: " + campaign.bloodTypes.joinToString { it.label }
            Text(category.uppercase(HomeLocale), style = HomeStyle.Caption, color = BloodRedStrong)
            Text(campaign.title, style = HomeStyle.Title, color = Ink)
            Text(campaign.centerName, style = HomeStyle.Supporting, color = InkSecondary)
        }
        HomeIcon(R.drawable.home_ic_arrow, Modifier.size(20.dp))
    }
}

@Composable
private fun HomeLinkedCard(onOpen: () -> Unit, actionLabel: String, content: @Composable RowScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onOpen),
        shape = HomeStyle.CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Border),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun HomeSectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.weight(1f), style = HomeStyle.Title, color = Ink)
        TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
            Text(action, style = HomeStyle.Supporting, color = BloodRedStrong)
        }
    }
}

@Composable
internal fun HomeIcon(@DrawableRes resource: Int, modifier: Modifier = Modifier.size(24.dp)) {
    Icon(painterResource(resource), contentDescription = null, modifier = modifier, tint = Color.Unspecified)
}
