package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.presentation.scheduling.*
import br.edu.fatec.doesangue.ui.components.DonationDatePickerDialog
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleDateScreen
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.*

/** Calendário real do Material3 e tela reais, sem rede nem reserva. */
@RunWith(AndroidJUnit4::class)
class AvailabilityCalendarTest {
    @get:Rule val compose = createComposeRule()
    private val month = YearMonth.of(2026,11)
    private val today = month.atDay(1)
    private fun day(number: String): SemanticsNodeInteraction {
        val pattern=Regex("(?<!\\d)$number(?!\\d)")
        val matcher=SemanticsMatcher("dia $number no calendário") { node ->
            val texts=node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty() +
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            texts.any(pattern::containsMatchIn)
        }
        val candidates=compose.onAllNodes(matcher and hasClickAction() and hasAnyAncestor(isDialog()))
        // O pager mantém meses adjacentes compostos fora da área visível.
        val displayed=candidates.fetchSemanticsNodes().indices.filter {
            runCatching { candidates[it].assertIsDisplayed() }.isSuccess
        }
        check(displayed.size==1) { "Esperado um dia $number visível, encontrados ${displayed.size}" }
        return candidates[displayed.single()]
    }

    @Test fun asynchronousAvailabilityEnablesOnlyReturnedDaysAndProtectsConfirmation() {
        val state = mutableStateOf(CalendarAvailabilityUiState(month=month,isLoading=true))
        val months = mutableListOf<YearMonth>()
        compose.setContent { DoeSangueTheme {
            DonationDatePickerDialog(month.atDay(20),today,state.value,{ months+=it },{}, {},{})
        } }
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        day("20").assertIsNotEnabled()
        compose.runOnIdle { state.value=CalendarAvailabilityUiState(month=month,dates=setOf(month.atDay(20))) }
        day("20").assertIsEnabled()
        day("21").assertIsNotEnabled()
        compose.onNodeWithText("Confirmar data").assertIsEnabled()
        compose.runOnIdle { assertEquals(listOf(month),months) }
        screenshot("calendario-disponivel")
    }

    @Test fun availableDayReturnsLocalDateAndDismissesWithoutInventingOtherDates() {
        var confirmed: LocalDate? = null
        var dismissed=0
        compose.setContent { DoeSangueTheme {
            DonationDatePickerDialog(null,today,CalendarAvailabilityUiState(month=month,dates=setOf(month.atDay(20))),
                {},{}, { confirmed=it },{ dismissed++ })
        } }
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        day("21").assertIsNotEnabled()
        day("20").performClick()
        compose.onNodeWithText("Confirmar data").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(month.atDay(20),confirmed); assertEquals(1,dismissed) }
    }

    @Test fun changingMonthDisablesStaleSelectionUntilNewMonthIsLoadedAndChosen() {
        val state=mutableStateOf(CalendarAvailabilityUiState(month=month,dates=setOf(month.atDay(20))))
        val months=mutableListOf<YearMonth>()
        compose.setContent { DoeSangueTheme {
            DonationDatePickerDialog(month.atDay(20),today,state.value,{ m ->
                months+=m
                if(m!=month) state.value=CalendarAvailabilityUiState(month=m,isLoading=true)
            },{}, {},{})
        } }
        compose.onNodeWithText("Confirmar data").assertIsEnabled()
        compose.onNode(hasContentDescription("next month",substring=true,ignoreCase=true) or
            hasContentDescription("próximo mês",substring=true,ignoreCase=true)).performClick()
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(month.plusMonths(1),months.last())
            state.value=CalendarAvailabilityUiState(month=month.plusMonths(1),dates=setOf(month.plusMonths(1).atDay(20)))
        }
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        day("20").performClick()
        compose.onNodeWithText("Confirmar data").assertIsEnabled()
    }

    @Test fun errorAndEmptyCalendarKeepConfirmDisabledAndExposeWorkingRetry() {
        val state=mutableStateOf(CalendarAvailabilityUiState(month=month,errorMessage="Falha sintética na consulta"))
        var retries=0
        compose.setContent { DoeSangueTheme {
            DonationDatePickerDialog(null,today,state.value,{}, { retries++ },{}, {})
        } }
        compose.onNodeWithText("Falha sintética na consulta").assertExists()
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.runOnIdle { assertEquals(1,retries); state.value=CalendarAvailabilityUiState(month=month) }
        compose.onNodeWithText("Nenhum dia com vagas neste mês.").assertExists()
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        compose.onNodeWithText("Atualizar dias").performClick()
        compose.runOnIdle { assertEquals(2,retries) }
    }

    @Test fun slotLabelsAndExplicitRefreshUseCurrentAvailabilityWithoutStartingCalendar() {
        val center=DonationCenter("1","Unidade Sintética","Cidade fictícia","Endereço fictício","","","America/Sao_Paulo")
        val slots=listOf(DonationTimeSlot("s1","1",Instant.parse("2026-11-20T11:00:00Z"),3,1),
            DonationTimeSlot("s2","1",Instant.parse("2026-11-20T12:00:00Z"),3,2))
        var selected: String?=null
        var refreshes=0
        var calendarReads=0
        compose.setContent { DoeSangueTheme {
            ScheduleDateScreen(SchedulingUiState(selectedCenter=center,selectedDate=month.atDay(20),timeSlots=slots),
                {},{ selected=it },{ refreshes++ },{ calendarReads++ },{}, {})
        } }
        compose.onNodeWithText("1 vaga").assertExists()
        compose.onNodeWithText("2 vagas").assertExists()
        compose.onNodeWithText("08:00").performScrollTo().performClick()
        compose.onNodeWithText("Atualizar horários").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("s1",selected); assertEquals(1,refreshes); assertEquals(0,calendarReads) }
        compose.onNodeWithText("Continuar").assertIsNotEnabled()
        screenshot("horarios-vagas")
    }

    private fun screenshot(name: String) {
        val file=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"t03-$name.png")
        val root=if(name.startsWith("calendario")) compose.onNode(isDialog()) else compose.onRoot()
        file.outputStream().use { root.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
