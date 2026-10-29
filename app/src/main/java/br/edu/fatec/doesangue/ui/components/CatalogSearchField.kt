package br.edu.fatec.doesangue.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.StockLevel
import br.edu.fatec.doesangue.ui.screens.home.HomeStyle
import br.edu.fatec.doesangue.ui.theme.*

@Composable
fun CatalogSearchField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, singleLine = true,
        placeholder = { Text("Nome da unidade ou cidade", style = HomeStyle.Supporting) },
        textStyle = HomeStyle.Supporting, shape = RoundedCornerShape(14.dp),
        leadingIcon = { Image(painterResource(R.drawable.refine_list_search), null, Modifier.size(20.dp)) },
        trailingIcon = if (value.isNotEmpty()) {{ TextButton(onClick = { onValueChange("") }) {
            Text("Limpar", style = HomeStyle.Supporting)
        } }} else null,
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = SurfaceSoft,
            focusedContainerColor = SurfaceSoft, unfocusedBorderColor = Border),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Buscar por nome ou cidade" })
}

@Composable
fun CatalogBloodTypeFilter(selected: BloodType?, onSelect: (BloodType?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text("Tipo sanguíneo: ${selected?.label ?: "Todos"}", style = HomeStyle.Supporting) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos") }, onClick = { expanded = false; onSelect(null) })
            BloodType.knownTypes.forEach { type ->
                DropdownMenuItem(text = { Text(type.label) }, onClick = { expanded = false; onSelect(type) })
            }
        }
    }
}
internal fun StockLevel.foreground() = when (this) {
    StockLevel.CRITICAL -> BloodRedStrong
    StockLevel.ALERT -> HomeStyle.Warning
    StockLevel.STABLE -> HomeStyle.Success
}
internal fun StockLevel.softBackground() = when (this) {
    StockLevel.CRITICAL -> HomeStyle.Soft
    StockLevel.ALERT -> HomeStyle.WarningSoft
    StockLevel.STABLE -> HomeStyle.SuccessSoft
}
