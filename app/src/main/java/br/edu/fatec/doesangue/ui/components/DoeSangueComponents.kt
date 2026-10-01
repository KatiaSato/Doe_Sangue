package br.edu.fatec.doesangue.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.Border
import br.edu.fatec.doesangue.ui.theme.Ink
import br.edu.fatec.doesangue.ui.theme.InkSecondary
import br.edu.fatec.doesangue.ui.theme.Muted

@Composable
fun DsScreen(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    bottomRoute: AppRoute? = null,
    onNavigate: (AppRoute) -> Unit = {},
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (bottomRoute != null) DsBottomBar(bottomRoute, onNavigate)
        },
    ) { padding ->
        val scroll = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .then(scroll)
                .padding(horizontal = 24.dp),
        ) {
            if (title != null || onBack != null) DsTopBar(title.orEmpty(), onBack)
            content()
        }
    }
}

@Composable
fun DsTopBar(title: String, onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().height(76.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (onBack == null) "" else "‹",
            modifier = Modifier.width(32.dp).clickable(enabled = onBack != null) { onBack?.invoke() },
            color = BloodRed,
            fontSize = 36.sp,
        )
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(8.dp),
    containerColor: Color = BloodRed,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Border),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = BloodRed)
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
        color = BloodRed,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
fun VisualField(label: String, value: String, modifier: Modifier = Modifier, tall: Boolean = false) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Ink)
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (tall) 112.dp else 48.dp)
                .border(1.dp, Border, RoundedCornerShape(7.dp))
                .padding(horizontal = 14.dp, vertical = 13.dp),
        ) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}

@Composable
fun StepProgress(current: Int, labels: List<String>? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        (1..4).forEach { step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = if (step <= current) BloodRed else Color(0xFFE6E6E6),
                    border = if (step == current && step > 1) BorderStroke(1.dp, BloodRed) else null,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "$step",
                            color = if (step <= current) Color.White else InkSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                if (labels != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(labels[step - 1], fontSize = 8.sp, color = if (step == current) BloodRed else Muted)
                }
            }
            if (step < 4) {
                Box(
                    Modifier
                        .padding(top = 11.dp)
                        .width(if (labels == null) 46.dp else 20.dp)
                        .height(2.dp)
                        .background(if (step < current) BloodRed else Color(0xFFD6D6D6)),
                )
            }
        }
    }
}

@Composable
fun SelectionCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFFFF5F5) else Color.White),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) BloodRed else Border),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
            Text(if (selected) "✓" else "○", color = BloodRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Border),
    ) { Column(Modifier.fillMaxWidth().padding(16.dp), content = content) }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = Ink)
        if (action != null) TextAction(action, onAction)
    }
}

@Composable
fun FigmaImage(painter: Painter, description: String?, modifier: Modifier) {
    Image(painter = painter, contentDescription = description, modifier = modifier)
}

@Composable
fun DsBottomBar(current: AppRoute, onNavigate: (AppRoute) -> Unit) {
    val items = listOf(
        Triple("⌂", "Início", AppRoute.Home),
        Triple("▣", "Agendar", AppRoute.ScheduleCenter),
        Triple("♡", "Doações", AppRoute.DonationsHistory),
        Triple("⌖", "Unidades", AppRoute.Centers),
        Triple("○", "Perfil", AppRoute.Profile),
    )
    Surface(color = Color.White, shadowElevation = 5.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(78.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { (icon, label, route) ->
                val selected = current == route || (route == AppRoute.DonationsHistory && current == AppRoute.DonationsAppointments)
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().clickable { onNavigate(route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(icon, fontSize = 20.sp, color = if (selected) BloodRed else InkSecondary)
                    Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) BloodRed else InkSecondary)
                }
            }
        }
    }
}

@Composable
fun ScreenHeading(title: String, subtitle: String? = null) {
    Text(title, style = MaterialTheme.typography.headlineLarge, color = BloodRed)
    if (subtitle != null) {
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = Ink)
    }
}

@Composable
fun CenteredMessage(symbol: String, title: String, body: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(Modifier.size(82.dp), shape = CircleShape, color = Color(0xFFFFE6E7)) {
            Box(contentAlignment = Alignment.Center) { Text(symbol, fontSize = 38.sp, color = BloodRed) }
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = BloodRed, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = InkSecondary, textAlign = TextAlign.Center)
    }
}
