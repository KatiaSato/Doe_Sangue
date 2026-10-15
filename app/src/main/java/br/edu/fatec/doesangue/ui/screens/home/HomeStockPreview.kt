package br.edu.fatec.doesangue.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.BloodStock
import br.edu.fatec.doesangue.domain.model.StockLevel
import br.edu.fatec.doesangue.ui.theme.BloodRedStrong
import br.edu.fatec.doesangue.ui.theme.InkSecondary
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun HomeStockPreview(stocks: List<BloodStock>) {
    // A prévia mantém o limite de quatro registros e identifica cada unidade.
    // Nunca combina níveis de unidades diferentes num único indicador.
    stocks.take(4).groupBy { it.centerId }.values.forEach { records ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${records.first().centerName} · dados fictícios", style = HomeStyle.Caption, color = InkSecondary)
            Row(Modifier.fillMaxWidth().heightIn(min = 82.dp).height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                records.forEach { record ->
                    val (color, background) = when (record.level) {
                        StockLevel.STABLE -> HomeStyle.Success to HomeStyle.SuccessSoft
                        StockLevel.ALERT -> HomeStyle.Warning to HomeStyle.WarningSoft
                        StockLevel.CRITICAL -> BloodRedStrong to HomeStyle.Soft
                    }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().background(background, HomeStyle.TileShape)
                            .padding(horizontal = 8.dp, vertical = 12.dp).semantics(mergeDescendants = true) {},
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(record.bloodType.label, style = HomeStyle.BloodType, color = color, textAlign = TextAlign.Center)
                        Text(record.level.label, style = HomeStyle.Caption, color = color, textAlign = TextAlign.Center)
                    }
                }
                repeat(4 - records.size) { Spacer(Modifier.weight(1f)) }
            }
            val metadata = records.groupBy { it.source to it.updatedAt }
            metadata.values.forEach { entries ->
                val record = entries.first()
                val updated = record.updatedAt.atZone(ZoneId.of(record.timeZoneId))
                val types = entries.joinToString { it.bloodType.label }
                val prefix = if (metadata.size > 1) "$types · " else ""
                Text(
                    prefix + "Publicação manual · atualizado em " + updated.format(DateTimeFormatter.ofPattern("dd MMM", HomeLocale)),
                    modifier = Modifier.semantics {
                        contentDescription = "$types. Fonte: ${record.source}. Atualizado em " +
                            updated.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", HomeLocale))
                    },
                    style = HomeStyle.Caption, color = InkSecondary,
                )
            }
        }
    }
}
