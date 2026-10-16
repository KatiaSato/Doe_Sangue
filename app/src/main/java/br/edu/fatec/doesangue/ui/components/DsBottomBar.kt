package br.edu.fatec.doesangue.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRedStrong
import br.edu.fatec.doesangue.ui.theme.Border
import br.edu.fatec.doesangue.ui.theme.InkSecondary
import br.edu.fatec.doesangue.ui.theme.Poppins

private val NavigationSelectedBackground = Color(0xFFFFF1F1)
private val NavigationItemShape = RoundedCornerShape(14.dp)
private val NavigationLabelStyle = TextStyle(
    fontFamily = Poppins,
    fontSize = 10.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Medium,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
fun DsBottomBar(current: AppRoute, onNavigate: (AppRoute) -> Unit) {
    val horizontalPadding = if (LocalDensity.current.fontScale > 1.2f) 0.dp else 20.dp
    val items = listOf(
        Triple(R.drawable.home_ic_house, "Início", AppRoute.Home),
        Triple(R.drawable.home_ic_calendar, "Agendar", AppRoute.ScheduleCenter),
        Triple(R.drawable.home_ic_heart, "Doações", AppRoute.DonationsHistory),
        Triple(R.drawable.home_ic_pin, "Unidades", AppRoute.Centers),
        Triple(R.drawable.home_ic_person, "Perfil", AppRoute.Profile),
    )
    Surface(color = Color.White) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = Border, thickness = 1.dp)
            Row(Modifier.fillMaxWidth().selectableGroup()
                .padding(horizontal = horizontalPadding, vertical = 8.dp)) {
                items.forEach { (icon, label, route) ->
                    val selected = current == route ||
                        (route == AppRoute.DonationsHistory && current == AppRoute.DonationsAppointments)
                    val color = if (selected) BloodRedStrong else InkSecondary
                    Column(
                        Modifier.weight(1f).background(if (selected) NavigationSelectedBackground else Color.Transparent, NavigationItemShape)
                            .selectable(selected = selected, role = Role.Tab, onClick = { onNavigate(route) })
                            .heightIn(min = 56.dp).padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        Icon(painterResource(icon), contentDescription = null,
                            modifier = Modifier.size(24.dp), tint = color)
                        Text(label, style = NavigationLabelStyle, color = color)
                    }
                }
            }
        }
    }
}
