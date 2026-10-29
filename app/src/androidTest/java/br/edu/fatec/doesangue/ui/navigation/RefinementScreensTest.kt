package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.presentation.appointments.AppointmentsUiState
import br.edu.fatec.doesangue.presentation.common.ReadUiState
import br.edu.fatec.doesangue.presentation.routines.ReminderRoutineUiState
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingUiState
import br.edu.fatec.doesangue.ui.screens.donations.*
import br.edu.fatec.doesangue.ui.screens.stock.BloodStockScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleCenterScreen
import br.edu.fatec.doesangue.ui.screens.routines.ReminderOverviewScreen
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate

/** Telas e filtros reais; dados somente em memória, nenhuma rede/Auth/SQL. */
class RefinementScreensTest {
    @get:Rule val compose = createComposeRule()
    private val a = DonationCenter("1","Unidade Sintética Alfa","Paulínia","Endereço fictício A","","","America/Sao_Paulo")
    private val b = a.copy(id="2",name="Unidade Sintética Beta",city="Cidade Azul")
    private val instant = Instant.parse("2026-10-29T12:00:00Z")
    private fun summary(id:String, status:AppointmentStatus=AppointmentStatus.SCHEDULED) =
        AppointmentSummary(Appointment(id,"sintetico","slot",status,instant),a.name,Instant.parse("2030-12-01T11:00:00Z"),a.timeZoneId,a.id)
    private fun stocks() = listOf(
        BloodStock(a.id,a.name,BloodType.O_NEGATIVE,StockLevel.CRITICAL,"Fonte A",instant,a.timeZoneId),
        BloodStock(a.id,a.name,BloodType.A_POSITIVE,StockLevel.ALERT,"Fonte A",instant,a.timeZoneId),
        BloodStock(b.id,b.name,BloodType.O_NEGATIVE,StockLevel.CRITICAL,"Fonte B",instant,b.timeZoneId))
    private fun search(text:String) {
        compose.onNode(hasSetTextAction()).performTextReplacement(text)
        closeSoftKeyboard()
        compose.waitForIdle()
    }
    private fun appScreen(state:AppointmentsUiState,onOpen:(String)->Unit={},onReminders:()->Unit={}) {
        compose.setContent { DoeSangueTheme { DonationAppointmentsScreen(state,{},onOpen,{},{},{},
            onReminders=onReminders) } }
    }
    @Test fun appointmentsCombineCityAndStateWithoutMixingRemarcadoWithCancelled() {
        val old = summary("old",AppointmentStatus.CANCELLED).copy(rescheduledToId="new")
        appScreen(AppointmentsUiState.Loaded(listOf(summary("new").copy(rescheduledFromId="old"),
            summary("cancel",AppointmentStatus.CANCELLED),old), listOf(a)))
        compose.onNodeWithText("Cancelados").performClick()
        search(" PAULINIA ")
        compose.onNodeWithText("Agendamento cancel").assertExists()
        compose.onNodeWithText("Agendamento old").assertDoesNotExist()
        compose.onNodeWithText("Agendamento new").assertDoesNotExist()
        compose.onNodeWithText("Remarcados").performClick()
        compose.onNodeWithText("Agendamento old").assertExists()
        compose.onNodeWithText("Nova data: 01/12/2030 • 08:00").assertExists()
        compose.onNodeWithText("Agendamento cancel").assertDoesNotExist()
        capture("remarcados")
        compose.onNodeWithText("Agendados").performClick()
        compose.onNodeWithText("Agendamento new").assertExists()
    }
    @Test fun missingCityCataloguePreservesRecordAndCardOpensExactId() {
        var opened:String?=null
        appScreen(AppointmentsUiState.Loaded(listOf(summary("kept")),centersError="Busca por cidade indisponível."),
            onOpen={opened=it})
        compose.onNodeWithText("Agendamento kept").assertExists()
        search("alfa")
        compose.onNode(hasText("Agendamento kept") and hasClickAction()).performClick()
        assertEquals("kept",opened)
    }
    @Test fun reminderTabRemainsVisibleWhenAppointmentListIsLongOrLoading() {
        var switched=false
        appScreen(AppointmentsUiState.Loaded((1..30).map {summary(it.toString())}),onReminders={switched=true})
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
        compose.onNodeWithText("Lembretes").assertIsDisplayed().performClick()
        assertTrue(switched)
        capture("lista-rolada")
    }
    @Test fun stockCombinesLevelTypeAndCityThenClearRestoresTheOtherUnit() {
        compose.setContent { DoeSangueTheme { BloodStockScreen(ReadUiState.Loaded(stocks()),{},{},{},
            initialLevel=StockLevel.CRITICAL,centersState=ReadUiState.Loaded(listOf(a,b))) } }
        compose.onNodeWithText("Tipo sanguíneo: Todos").performClick()
        compose.onNodeWithText("O-").performClick()
        search("paulinia")
        compose.onNodeWithText(a.name).assertExists()
        compose.onNodeWithText(b.name).assertDoesNotExist()
        compose.onNodeWithText("O- • Crítico").assertExists()
        compose.onNodeWithText("A+ • Alerta").assertDoesNotExist()
        capture("estoque-filtrado")
        compose.onNodeWithText("Limpar").performClick()
        compose.onNodeWithText(b.name).assertExists()
        compose.onNodeWithText("Alerta").performClick()
        compose.onNodeWithText("Nenhuma unidade encontrada para esta busca, nível e tipo sanguíneo.").assertExists()
        compose.onNodeWithText("Tipo sanguíneo: O-").performClick()
        compose.onAllNodesWithText("Todos").onLast().performClick()
        compose.onNodeWithText("A+ • Alerta").assertExists()
    }
    @Test fun stockGroupingPreservesDistinctSourceAndUpdateAndCenterLink() {
        var route:AppRoute?=null
        val records=stocks().take(2).mapIndexed { index, value ->
            if(index==0)value else value.copy(source="Outra fonte",updatedAt=instant.minusSeconds(3600)) }
        compose.setContent { DoeSangueTheme { BloodStockScreen(ReadUiState.Loaded(records),{},{},{route=it},
            centersState=ReadUiState.Loaded(listOf(a))) } }
        compose.onNodeWithText("Fonte: Fonte A").assertExists()
        compose.onNodeWithText("Fonte: Outra fonte").assertExists()
        compose.onNodeWithText("O- · atualizado em 29/10/2026 • 09:00").assertExists()
        compose.onNodeWithText("A+ · atualizado em 29/10/2026 • 08:00").assertExists()
        compose.onNode(hasText(a.name) and hasClickAction()).performClick()
        assertEquals(AppRoute.CenterDetailById(a.id),route)
    }
    @Test fun schedulingSearchKeepsASelectedUnitEvenWhenFilteredOutAndBackRestoresIt() {
        var state by mutableStateOf(SchedulingUiState(centers=listOf(a,b),selectedCenter=a))
        var continued:String?=null
        compose.setContent { DoeSangueTheme { ScheduleCenterScreen(state,{state=state.copy(selectedCenter=it)},{},{},
            {continued=state.selectedCenter?.id},{}) } }
        search("cidade azul")
        compose.onNodeWithText(b.name).assertExists()
        compose.onNodeWithText("Unidade selecionada: ${a.name}. Limpe a busca para vê-la na lista.").assertExists()
        compose.onNodeWithText("Continuar").performScrollTo().performClick()
        assertEquals(a.id,continued)
        compose.onNodeWithText("Limpar").performScrollTo().performClick()
        compose.onNodeWithText(a.name).assertExists()
    }
    @Test fun filtersAndRoutineRemainReadableWithLargeFont() {
        val routine=ReminderRoutine("routine","sintetico",LocalDate.of(2030,12,1),30,a.timeZoneId,true,1)
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.5f)) {
                DoeSangueTheme { ReminderOverviewScreen(ReminderRoutineUiState(loading=false,loaded=true,routine=routine),
                    {},{},{},{},{},{},{}) }
            }
        }
        compose.onNodeWithText("Lembretes").assertIsDisplayed()
        compose.onNodeWithText("Minha rotina").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Editar rotina").performScrollTo().assertIsDisplayed()
        capture("lembretes-fonte-ampliada")
    }
    @Test fun routinePauseAndDeleteAreExplicitAndSavingBlocksNavigation() {
        val routine=ReminderRoutine("routine","sintetico",LocalDate.of(2030,12,1),30,a.timeZoneId,true,1)
        var state by mutableStateOf(ReminderRoutineUiState(loading=false,loaded=true,routine=routine))
        var paused=false;var deleted=false;var tabChanged=false
        compose.setContent { DoeSangueTheme { ReminderOverviewScreen(state,{},{},{paused=!it},{deleted=true},{},
            {tabChanged=true},{}) } }
        compose.onNodeWithText("Pausar rotina").performScrollTo().performClick()
        assertTrue(paused)
        compose.onNodeWithText("Excluir").performScrollTo().performClick()
        assertFalse(deleted)
        compose.onNodeWithText("Manter rotina").performClick()
        assertFalse(deleted)
        compose.onNodeWithText("Excluir").performClick()
        compose.onNodeWithText("Excluir rotina").performClick()
        assertTrue(deleted)
        compose.runOnIdle {state=state.copy(saving=true)}
        compose.onNodeWithText("Agendamentos").assertIsNotEnabled().performClick()
        assertFalse(tabChanged)
    }
    @Test fun enlargedAppointmentFiltersWrapWithoutHidingTheLastSituation() {
        compose.setContent {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.5f)) {
                DoeSangueTheme { DonationAppointmentsScreen(AppointmentsUiState.Loaded(listOf(summary("large")),listOf(a)),{},{},{},{},{}) }
            }
        }
        compose.onNodeWithText("Remarcados").assertIsDisplayed().performClick()
        compose.onNodeWithText("Nenhum agendamento encontrado para esta busca e situação.").assertExists()
        capture("agendamentos-fonte-ampliada")
    }
    private fun capture(name:String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null),"refinements_$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
}
