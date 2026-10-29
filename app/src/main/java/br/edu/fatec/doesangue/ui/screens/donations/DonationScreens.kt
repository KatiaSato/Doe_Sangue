package br.edu.fatec.doesangue.ui.screens.donations

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.presentation.common.*
import br.edu.fatec.doesangue.presentation.appointments.AppointmentsUiState
import br.edu.fatec.doesangue.ui.components.*
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.screens.home.HomeStyle
import br.edu.fatec.doesangue.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class DonationTab(val label: String) { HISTORY("Histórico"), APPOINTMENTS("Agendamentos"), REMINDERS("Lembretes") }

@Composable
fun DonationTabs(selected: DonationTab, onSelect: (DonationTab) -> Unit, enabled: Boolean = true) {
    // Mantém o nome inteiro quando a pessoa amplia a fonte, sem cortar palavras.
    if (LocalDensity.current.fontScale > 1.2f) {
        ScrollableTabRow(selectedTabIndex = selected.ordinal, edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surface, contentColor = BloodRedStrong) {
            DonationTab.entries.forEach { tab ->
                Tab(selected = tab == selected, onClick = { onSelect(tab) }, enabled = enabled,
                    selectedContentColor = BloodRedStrong, unselectedContentColor = InkSecondary,
                    text = { Text(tab.label, style = HomeStyle.Supporting, maxLines = 1) })
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth().selectableGroup()) {
        DonationTab.entries.forEach { tab ->
            val active = selected == tab
            Column(Modifier.weight(1f).selectable(selected = active, enabled = enabled, role = Role.Tab,
                onClick = { onSelect(tab) }), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(tab.label, Modifier.padding(vertical = 12.dp), style = HomeStyle.Supporting,
                    color = if (active) BloodRedStrong else InkSecondary)
                HorizontalDivider(thickness = if (active) 3.dp else 1.dp, color = if (active) BloodRed else Border)
            }
        }
    }
}

@Composable
fun DonationHistoryScreen(state: ReadUiState<List<DonationRecord>>, onRetry: () -> Unit,
    onBack: () -> Unit, onAppointments: () -> Unit, onNavigate: (AppRoute) -> Unit,
    onReminders: () -> Unit = {}) {
    BackHandler { onBack() }
    DsScreen(title = "Minhas doações", onBack = onBack, bottomRoute = AppRoute.DonationsHistory,
        onNavigate = onNavigate, scrollable = false) {
        DonationTabs(DonationTab.HISTORY, { when (it) {
            DonationTab.APPOINTMENTS -> onAppointments(); DonationTab.REMINDERS -> onReminders(); else -> Unit
        } })
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 18.dp)) {
            item { Column { Text("Histórico de demonstração: registros fictícios publicados pela administração.")
                Text("Agendamentos não são contabilizados como doações.", style = MaterialTheme.typography.bodySmall) } }
            when (state) {
                ReadUiState.Loading -> item { Text("Carregando histórico…") }
                is ReadUiState.Error -> item { Column { Text(state.message, color = MaterialTheme.colorScheme.error); PrimaryButton("Tentar novamente", onRetry) } }
                is ReadUiState.Loaded -> {
                    if (state.data.isEmpty()) item { Text("Nenhum registro de doação fictícia encontrado.") }
                    items(state.data, key = { it.id }) { record -> InfoCard {
                        Text(record.occurredAt.atZone(ZoneId.of(record.timeZoneId)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm")), style = MaterialTheme.typography.titleMedium)
                        Text(record.centerName); Text("Doação fictícia • Registro ${record.id}", color = BloodRed)
                        Text("Fonte: ${record.source}", style = MaterialTheme.typography.bodySmall)
                    } }
                    item { InfoCard { Text("Total de registros fictícios", style = MaterialTheme.typography.titleMedium)
                        Text(state.data.size.toString(), style = MaterialTheme.typography.headlineMedium, color = BloodRed) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonationAppointmentsScreen(uiState: AppointmentsUiState, onRetry: () -> Unit,
    onOpenDetails: (String) -> Unit, onBack: () -> Unit, onHistory: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onReminders: () -> Unit = {}, onRetryCities: () -> Unit = {}) {
    BackHandler { onBack() }
    var query by rememberSaveable { mutableStateOf("") }
    var selection by rememberSaveable { mutableStateOf(AppointmentListFilter.ALL) }
    val list = rememberLazyListState()
    LaunchedEffect(query, selection) { list.scrollToItem(0) }
    DsScreen(title = "Minhas doações", onBack = onBack, bottomRoute = AppRoute.DonationsAppointments,
        onNavigate = onNavigate, scrollable = false) {
        DonationTabs(DonationTab.APPOINTMENTS, { when (it) {
            DonationTab.HISTORY -> onHistory(); DonationTab.REMINDERS -> onReminders(); else -> Unit
        } })
        Spacer(Modifier.height(16.dp))
        CatalogSearchField(query) { query = it }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AppointmentListFilter.entries.forEach { option -> FilterChip(selected = selection == option,
                onClick = { selection = option }, label = { Text(option.label, style = HomeStyle.Supporting) },
                shape = RoundedCornerShape(18.dp),
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = HomeStyle.Soft,
                    selectedLabelColor = BloodRedStrong)) }
        }
        LazyColumn(Modifier.weight(1f), state = list, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            when (uiState) {
                AppointmentsUiState.Loading -> item { Text("Carregando agendamentos…") }
                is AppointmentsUiState.Error -> item { Column { Text(uiState.message, color = MaterialTheme.colorScheme.error); PrimaryButton("Tentar novamente", onRetry) } }
                is AppointmentsUiState.Loaded -> {
                    if (uiState.centersLoading) item { Text("Carregando cidades para a busca…", style = HomeStyle.Supporting) }
                    uiState.centersError?.let { message -> item { Column { Text(message, style = HomeStyle.Supporting)
                        TextButton(onClick = onRetryCities) { Text("Atualizar cidades") } } } }
                    val now = Instant.now()
                    val result = filterAppointments(uiState.appointments, uiState.centers, query, selection, now)
                    val cities = uiState.centers.associate { it.id to it.city }
                    val recordsById = uiState.appointments.associateBy { it.appointment.id }
                    if (result.isEmpty()) item { Text(if (uiState.appointments.isEmpty()) "Você ainda não possui agendamentos."
                        else "Nenhum agendamento encontrado para esta busca e situação.") }
                    val upcoming = result.filter { it.isUpcoming(now) }
                    val previous = result.filterNot { it.isUpcoming(now) }
                    if (upcoming.isNotEmpty()) item { Text("Próximos • ${upcoming.size}", style = HomeStyle.Label) }
                    items(upcoming, key = { it.appointment.id }) { summary -> AppointmentListCard(summary, cities[summary.centerId], onOpenDetails,
                        recordsById[summary.rescheduledToId ?: summary.rescheduledFromId]) }
                    if (previous.isNotEmpty()) item { Text(if (selection == AppointmentListFilter.ALL) "Anteriores • ${previous.size}"
                        else "${selection.label} • ${previous.size}", style = HomeStyle.Label) }
                    items(previous, key = { it.appointment.id }) { summary -> AppointmentListCard(summary, cities[summary.centerId], onOpenDetails,
                        recordsById[summary.rescheduledToId ?: summary.rescheduledFromId]) }
                }
            }
        }
    }
}

@Composable
private fun AppointmentListCard(summary: AppointmentSummary, city: String?, onOpen: (String) -> Unit, related: AppointmentSummary?) {
    val local = summary.startsAt.atZone(ZoneId.of(summary.timeZoneId))
    val locale = Locale.forLanguageTag("pt-BR")
    val relatedDate = related?.let { it.startsAt.atZone(ZoneId.of(it.timeZoneId))
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm")) }
    val (status, foreground, soft) = when {
        summary.appointment.status == AppointmentStatus.SCHEDULED -> Triple("Agendado", HomeStyle.Success, HomeStyle.SuccessSoft)
        summary.rescheduledToId != null -> Triple("Remarcado", HomeStyle.Warning, HomeStyle.WarningSoft)
        else -> Triple("Cancelado", BloodRedStrong, HomeStyle.Soft)
    }
    Card(Modifier.fillMaxWidth().clickable(role = Role.Button,
        onClickLabel = "Ver detalhes do agendamento ${summary.appointment.id}", onClick = { onOpen(summary.appointment.id) }),
        shape = HomeStyle.CardShape, border = BorderStroke(1.dp, Border), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.widthIn(min = 48.dp).background(SurfaceSoft, HomeStyle.TileShape).padding(8.dp)
                .semantics { contentDescription = local.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(local.format(DateTimeFormatter.ofPattern("dd")), style = HomeStyle.Greeting)
                Text(local.format(DateTimeFormatter.ofPattern("MMM", locale)).removeSuffix(".").uppercase(locale), style = HomeStyle.Caption, color = InkSecondary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(status, Modifier.background(soft, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 2.dp), style = HomeStyle.Caption, color = foreground)
                Text(local.format(DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm")), style = HomeStyle.Supporting)
                Text(summary.centerName, style = HomeStyle.Label, color = Ink)
                city?.let { Text(it, style = HomeStyle.Supporting, color = InkSecondary) }
                Text("Agendamento ${summary.appointment.id}", style = HomeStyle.Caption, color = InkSecondary)
                if (summary.rescheduledFromId != null) Text(relatedDate?.let { "Remarcado de $it" }
                    ?: "Remarcado do agendamento ${summary.rescheduledFromId}", style = HomeStyle.Caption, color = InkSecondary)
                if (summary.rescheduledToId != null) Text(relatedDate?.let { "Nova data: $it" }
                    ?: "Nova reserva: ${summary.rescheduledToId}", style = HomeStyle.Caption, color = InkSecondary)
            }
            Image(painterResource(R.drawable.refine_list_chevron), null, Modifier.size(16.dp))
        }
    }
}
