package br.edu.fatec.doesangue.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.domain.model.BloodType

@Composable
fun BloodTypePicker(selected: BloodType?, onSelect: (BloodType) -> Unit, enabled: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BloodType.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { type ->
                    FilterChip(
                        selected = selected == type, onClick = { onSelect(type) }, enabled = enabled,
                        label = { Text(type.label) }, modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
