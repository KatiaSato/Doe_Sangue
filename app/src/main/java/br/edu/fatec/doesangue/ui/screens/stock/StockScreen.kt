package br.edu.fatec.doesangue.ui.screens.stock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.presentation.common.*
import br.edu.fatec.doesangue.ui.components.*
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.screens.home.HomeStyle
import br.edu.fatec.doesangue.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BloodStockScreen(state: ReadUiState<List<BloodStock>>, onRetry: () -> Unit,
    onBack: () -> Unit, onNavigate: (AppRoute) -> Unit,
    initialLevel: StockLevel? = null,
    centersState: ReadUiState<List<DonationCenter>> = ReadUiState.Loaded(emptyList()),
    onRetryCities: () -> Unit = {}) {
    var query by rememberSaveable { mutableStateOf("") }
    var level by rememberSaveable(initialLevel) { mutableStateOf(initialLevel) }
    var bloodType by rememberSaveable { mutableStateOf<BloodType?>(null) }
    val list = rememberLazyListState()
    LaunchedEffect(query, level, bloodType) { list.scrollToItem(0) }
    DsScreen(title = "Estoque de sangue", onBack = onBack, bottomRoute = AppRoute.Home,
        onNavigate = onNavigate, scrollable = false) {
        Text("Encontre as unidades pelo nível de estoque.", style = HomeStyle.Supporting, color = InkSecondary)
        Spacer(Modifier.height(12.dp))
        CatalogSearchField(query) { query = it }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<StockLevel?>(null) + listOf(StockLevel.CRITICAL, StockLevel.ALERT, StockLevel.STABLE)).forEach { option ->
                FilterChip(selected = level == option, onClick = { level = option },
                    label = { Text(option?.label ?: "Todos", style = HomeStyle.Supporting) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = option?.softBackground() ?: HomeStyle.Soft,
                        selectedLabelColor = option?.foreground() ?: BloodRedStrong))
            }
        }
        CatalogBloodTypeFilter(bloodType) { bloodType = it }
        LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            item { Text("Demonstração: níveis fictícios por unidade e tipo sanguíneo.", style = HomeStyle.Caption, color = InkSecondary) }
            when (centersState) {
                ReadUiState.Loading -> item { Text("Carregando cidades para a busca…", style = HomeStyle.Supporting) }
                is ReadUiState.Error -> item { Column { Text(centersState.message, style = HomeStyle.Supporting)
                    TextButton(onClick = onRetryCities) { Text("Atualizar cidades") } } }
                else -> Unit
            }
            when (state) {
                ReadUiState.Loading -> item { Text("Carregando estoque…") }
                is ReadUiState.Error -> item { Column { Text(state.message, color = MaterialTheme.colorScheme.error); PrimaryButton("Tentar novamente", onRetry) } }
                is ReadUiState.Loaded -> {
                    val centers = (centersState as? ReadUiState.Loaded)?.data.orEmpty()
                    val groups = filterStockUnits(state.data, centers, query, level, bloodType)
                    if (groups.isEmpty()) item { Text(if (state.data.isEmpty()) "Nenhum nível de estoque publicado."
                        else "Nenhuma unidade encontrada para esta busca, nível e tipo sanguíneo.") }
                    else item { Text("${groups.size} " + if (groups.size == 1) "unidade encontrada" else "unidades encontradas", style = HomeStyle.Label) }
                    items(groups, key = { it.centerId }) { group ->
                        StockUnitCard(group) { onNavigate(AppRoute.CenterDetailById(group.centerId)) }
                    }
                }
            }
            item { Text("Dados fictícios. Uma unidade pode aparecer em mais de um nível, conforme o tipo sanguíneo. Não representam disponibilidade real.",
                style = HomeStyle.Caption, color = InkSecondary) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StockUnitCard(group: StockUnitGroup, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Ver unidade e horários", onClick = onOpen),
        border = BorderStroke(1.dp, Border), shape = HomeStyle.CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(group.centerName, Modifier.weight(1f), style = HomeStyle.Label)
                Image(painterResource(R.drawable.refine_list_chevron), null, Modifier.size(16.dp))
            }
            group.city?.let { Text(it, style = HomeStyle.Supporting, color = InkSecondary) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.records.forEach { stock ->
                    Surface(shape = HomeStyle.TileShape, color = stock.level.softBackground()) {
                        Text("${stock.bloodType.label} • ${stock.level.label}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = HomeStyle.Supporting, color = stock.level.foreground())
                    }
                }
            }
            // Preserva fontes/instantes diferentes; nunca atribui a um tipo a data de outro.
            group.records.groupBy { Triple(it.source, it.updatedAt, it.timeZoneId) }.values.forEach { records ->
                val first = records.first()
                val date = first.updatedAt.atZone(ZoneId.of(first.timeZoneId)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm"))
                val types = records.joinToString { it.bloodType.label }
                Column {
                    Text("$types · atualizado em $date", style = HomeStyle.Caption, color = InkSecondary)
                    Text("Fonte: ${first.source}", style = HomeStyle.Caption, color = InkSecondary)
                }
            }
            Text("Ver unidade e horários →", style = HomeStyle.Supporting, color = BloodRedStrong)
        }
    }
}
