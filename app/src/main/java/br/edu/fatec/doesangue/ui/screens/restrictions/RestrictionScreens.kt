package br.edu.fatec.doesangue.ui.screens.restrictions

import android.content.ActivityNotFoundException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.presentation.restrictions.DonationRestrictions
import br.edu.fatec.doesangue.presentation.restrictions.RestrictionCategory
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.PrimaryButton

// Paleta do Figma aprovado; tipografia Poppins herdada do tema do aplicativo.
private val RestrictionRed = Color(0xFFE53935)
private val RestrictionSoft = Color(0xFFFFF1F1)
private val RestrictionInk = Color(0xFF1F1F24)
private val RestrictionMuted = Color(0xFF61616B)
private val RestrictionBorder = Color(0xFFE0E0E5)
private val CardShape = RoundedCornerShape(14.dp)

@Composable
fun BeforeDonationCard(onConsult: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, RestrictionBorder),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Antes de doar", fontSize = 20.sp, lineHeight = 30.sp,
                fontWeight = FontWeight.SemiBold, color = RestrictionInk,
                modifier = Modifier.semantics { heading() })
            Text("Conheça os requisitos e as situações que podem impedir ou adiar uma doação.",
                fontSize = 13.sp, lineHeight = 20.sp, color = RestrictionMuted)
            PrimaryButton("Consultar restrições", onConsult,
                shape = CardShape, containerColor = RestrictionRed)
        }
    }
}

@Composable
fun RestrictionsScreen(onBack: () -> Unit, onCategory: (RestrictionCategory) -> Unit) {
    RestrictionPage("Início", onBack, gap = 16) {
        Heading("Consultar restrições",
            "O que você gostaria de consultar antes de doar?", menu = true)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DonationRestrictions.categories.forEach { category ->
                Card(
                    onClick = { onCategory(category.category) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, RestrictionBorder),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(category.title, fontSize = 16.sp, lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold, color = RestrictionInk)
                        Text(category.summary, fontSize = 13.sp, lineHeight = 20.sp, color = RestrictionMuted)
                    }
                }
            }
        }
        Surface(shape = CardShape, color = RestrictionSoft) {
            Column(Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A avaliação é do hemocentro", fontSize = 13.sp, lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold, color = RestrictionInk)
                Text("Estas informações orientam sua consulta. A decisão sobre a doação é feita na triagem clínica.",
                    fontSize = 12.sp, lineHeight = 18.sp, color = RestrictionMuted)
            }
        }
    }
}

@Composable
fun RestrictionDetailScreen(category: RestrictionCategory, onBack: () -> Unit) {
    val content = DonationRestrictions.forCategory(category)
    val uriHandler = LocalUriHandler.current
    var sourceError by rememberSaveable(category) { mutableStateOf(false) }
    RestrictionPage("Restrições", onBack) {
        Heading(content.title, content.introduction)
        Surface(
            modifier = Modifier.fillMaxWidth(), shape = CardShape,
            color = Color.White, border = BorderStroke(1.dp, RestrictionBorder),
        ) {
            Column {
                content.items.forEach { item ->
                    Column(Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, fontSize = 14.sp, lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold, color = RestrictionInk,
                            modifier = Modifier.semantics { heading() })
                        Text(item.description, fontSize = 13.sp, lineHeight = 20.sp, color = RestrictionMuted)
                    }
                }
            }
        }
        Surface(Modifier.fillMaxWidth(), shape = CardShape, color = RestrictionSoft) {
            Text("Para mais informações, contate o hemocentro.",
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                fontSize = 13.sp, lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold, color = RestrictionInk)
        }
        TextButton(
            onClick = {
                sourceError = false
                try {
                    uriHandler.openUri(content.sourceUrl)
                } catch (_: ActivityNotFoundException) {
                    sourceError = true
                } catch (_: IllegalArgumentException) {
                    sourceError = true
                }
            },
            contentPadding = PaddingValues(0.dp),
        ) {
            Text("Fonte: ${content.sourceName} ↗", fontSize = 11.sp, lineHeight = 17.sp,
                textDecoration = TextDecoration.Underline, color = RestrictionRed)
        }
        if (sourceError) {
            Text("Não foi possível abrir a fonte. Verifique se há um navegador disponível.",
                fontSize = 12.sp, color = RestrictionMuted)
        }
    }
}

@Composable
private fun RestrictionPage(
    backLabel: String,
    onBack: () -> Unit,
    gap: Int = 12,
    content: @Composable ColumnScope.() -> Unit,
) {
    DsScreen {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(gap.dp)) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Image(painterResource(R.drawable.triagem_back), null, Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(backLabel, fontSize = 13.sp, lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium, color = RestrictionRed)
            }
            content()
        }
    }
}

@Composable
private fun Heading(title: String, introduction: String, menu: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = if (menu) 23.sp else 22.sp,
            lineHeight = if (menu) 35.sp else 33.sp,
            fontWeight = FontWeight.SemiBold, color = RestrictionInk,
            modifier = Modifier.semantics { heading() })
        Text(introduction, fontSize = if (menu) 14.sp else 12.sp,
            lineHeight = if (menu) 21.sp else 18.sp, color = RestrictionMuted)
    }
}
