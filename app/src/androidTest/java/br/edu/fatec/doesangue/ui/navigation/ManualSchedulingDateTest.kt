package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.presentation.scheduling.CalendarAvailabilityUiState
import br.edu.fatec.doesangue.ui.components.DonationDatePickerDialog
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/** Apenas o diálogo e respostas locais: nenhuma rede, conta ou reserva. */
class ManualSchedulingDateTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026,11,1)
    private val initialMonth = YearMonth.from(today)
    private val target = LocalDate.of(2027,8,12)
    private val availability = mutableStateOf(CalendarAvailabilityUiState(month=initialMonth))
    private val months = mutableListOf<YearMonth>()
    private var confirmed: LocalDate? = null
    private var dismissed = 0
    private var retries = 0
    private fun open(selected: LocalDate? = null) {
        compose.setContent { DoeSangueTheme {
            DonationDatePickerDialog(selected,today,availability.value,{ month ->
                months += month
                if (month != availability.value.month) availability.value = CalendarAvailabilityUiState(month=month,isLoading=true)
            },{ retries++ },{ confirmed=it },{ dismissed++ })
        } }
        compose.onNodeWithText("Digitar data").performClick()
        compose.waitForIdle()
    }
    private fun type(value: String) {
        compose.onNodeWithText("Data do agendamento").performTextReplacement(value)
        compose.waitForIdle()
    }
    @Test fun unpaddedPaddedAndDigitsRecognizeSameDateAndWaitForCorrectMonth() {
        open();type("12/8/2027")
        compose.onNodeWithText("Data reconhecida: 12/08/2027").assertExists()
        compose.onNodeWithText("Consultando dias disponíveis…").assertExists()
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        assertEquals(YearMonth.of(2027,8),months.last())
        compose.runOnIdle {availability.value=CalendarAvailabilityUiState(month=YearMonth.from(target),dates=setOf(target))}
        for (input in listOf("12/08/2027","12082027","12/8/2027")) {
            type(input)
            compose.onNodeWithText("Data reconhecida: 12/08/2027").assertExists()
            compose.onNodeWithText("Confirmar data").assertIsEnabled()
        }
        capture("data-reconhecida")
        compose.onNodeWithText("Confirmar data").performClick()
        assertEquals(target,confirmed);assertEquals(1,dismissed)
    }
    @Test fun invalidPastAndOutOfRangeInputsExplainWhyWithoutQueryingOrConfirming() {
        availability.value=CalendarAvailabilityUiState(month=initialMonth,dates=setOf(today.plusDays(1)))
        open(today.plusDays(1));val count=months.size
        type("");compose.onNodeWithText("Informe o dia, o mês e o ano.").assertExists()
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        for (input in listOf("31/2/2027","12/8/27","12/8/0000","data")) {
            type(input)
            compose.onNodeWithText("Informe uma data válida, como 12/8/2027 ou 12082027.").assertExists()
            compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        }
        type("31/10/2026")
        compose.onNodeWithText("Esta data já passou. Escolha uma data a partir de hoje.").assertExists()
        type("12/8/9999")
        compose.onNodeWithText("Escolha uma data até 31/12/2100.").assertExists()
        assertEquals(count,months.size);assertNull(confirmed);assertEquals(0,dismissed)
    }
    @Test fun validDateWithoutAvailabilityAndNetworkFailureHaveDifferentFeedbackAndRetry() {
        open();type("12/8/2027")
        compose.runOnIdle {availability.value=CalendarAvailabilityUiState(month=YearMonth.from(target),errorMessage="Falha sintética ao consultar")}
        compose.onNodeWithText("Falha sintética ao consultar").assertExists()
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        compose.onNodeWithText("Tentar novamente").performClick();assertEquals(1,retries)
        compose.runOnIdle {availability.value=CalendarAvailabilityUiState(month=YearMonth.from(target))}
        compose.onNodeWithText("Não há horários com vagas disponíveis nesta data.").assertExists()
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled();capture("data-sem-vagas")
        val count=months.size;compose.onNodeWithText("Atualizar dias").performClick()
        assertEquals(count+1,months.size);assertEquals(YearMonth.from(target),months.last())
        assertNull(confirmed)
    }
    @Test fun switchingModesKeepsRecognizedDateAndCancelDoesNotChangeParentChoice() {
        val selected=today.plusDays(1)
        availability.value=CalendarAvailabilityUiState(month=initialMonth,dates=setOf(selected))
        open(selected);type("12/8/2027")
        compose.runOnIdle {availability.value=CalendarAvailabilityUiState(month=YearMonth.from(target),dates=setOf(target))}
        compose.onNodeWithText("Ver calendário").performClick()
        compose.onNodeWithText("Digitar data").performClick()
        compose.onNodeWithText("Data do agendamento").assertTextContains("12/08/2027")
        compose.onNodeWithText("Confirmar data").assertIsEnabled()
        compose.onNodeWithText("Cancelar").performClick()
        assertNull(confirmed);assertEquals(1,dismissed)
    }
    @Test fun changingTypedMonthCannotConfirmStaleAvailabilityFromPreviousMonth() {
        open();type("12/8/2027");type("12/9/2027")
        compose.runOnIdle {availability.value=CalendarAvailabilityUiState(month=YearMonth.from(target),dates=setOf(target))}
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        compose.onNodeWithText("Consultando dias disponíveis…").assertExists()
        val september=target.plusMonths(1)
        compose.runOnIdle {availability.value=CalendarAvailabilityUiState(month=YearMonth.from(september),dates=setOf(september))}
        compose.onNodeWithText("Confirmar data").assertIsEnabled().performClick()
        assertEquals(september,confirmed)
    }
    private fun capture(name: String) {
        val file=File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"calendar-$name.png")
        compose.onNode(isDialog()).captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
}
