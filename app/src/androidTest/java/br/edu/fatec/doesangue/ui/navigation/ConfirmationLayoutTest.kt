package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.model.RescheduleOrigin
import br.edu.fatec.doesangue.domain.model.RoutineChoice
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingUiState
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleConfirmScreen
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Real screen, locally supplied values and callbacks; no repository or Auth. */
class ConfirmationLayoutTest {
    @get:Rule val compose=createComposeRule()
    private val center=DonationCenter("sintetica-10","Unidade Escola Demo 10 — Bosque Sereno",
        "Cidade fictícia","Endereço fictício","","","America/Sao_Paulo")
    private val initial=SchedulingUiState(selectedCenter=center,selectedDate=LocalDate.of(2026,11,2),
        selectedTime=LocalTime.NOON,selectedTimeSlotId="horario-sintetico")
    private var confirmations=0
    private var scale=1.15f

    private fun open(state:SchedulingUiState=initial,fontScale:Float=1.15f) {
        scale=fontScale
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale)) {
                AndroidView(modifier=Modifier.fillMaxSize(),factory={ context ->
                    ComposeView(context).apply { setContent { Content(state) } }
                },onRelease={ it.disposeComposition() })
            }
        }
        compose.waitForIdle()
    }

    @Composable private fun Content(state:SchedulingUiState) {
        DoeSangueTheme {
            Box(Modifier.fillMaxSize().testTag("confirmation-layout-fixture")) {
                ScheduleConfirmScreen(state,{}, { confirmations++ })
            }
        }
    }

    private fun label(text:String)=compose.onAllNodesWithText(text).onLast()
    private fun layout(node:SemanticsNodeInteraction):TextLayoutResult {
        val results=mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }
    private fun assertWholeLabel(text:String) {
        val node=label(text).performScrollTo().assertIsDisplayed()
        assertEquals(scale,node.fetchSemanticsNode().layoutInfo.density.fontScale,0.01f)
        val result=layout(node)
        for (word in Regex("\\S+").findAll(text)) {
            assertEquals("Label word '${word.value}' must not break across lines",
                result.getLineForOffset(word.range.first),result.getLineForOffset(word.range.last))
        }
    }
    private fun assertValue(text:String) {
        val result=layout(compose.onNodeWithText(text).performScrollTo().assertIsDisplayed())
        assertEquals(text,result.layoutInput.text.text)
        assertFalse(result.didOverflowWidth)
        assertFalse((0 until result.lineCount).any(result::isLineEllipsized))
    }
    private fun capture(name:String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null),"confirmation-layout-$name.png").outputStream().use {
            compose.onNodeWithTag("confirmation-layout-fixture").captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun screenshotUnitNameDoesNotBreakUnitLabelIntoLetters() {
        open();assertWholeLabel("Unidade");assertValue(center.name)
        val labelBounds=label("Unidade").fetchSemanticsNode().boundsInRoot
        val valueBounds=compose.onNodeWithText(center.name).fetchSemanticsNode().boundsInRoot
        assertTrue("Label and value must occupy separate columns",labelBounds.right <= valueBounds.left+0.5f)
        capture("nome-longo")
    }
    @Test fun enlargedFontPreservesLongNameLabelsAndConfirmationCallback() {
        val longName="Unidade Sintética de Demonstração e Coleta — Jardim das Flores do Bosque Sereno"
        open(initial.copy(selectedCenter=center.copy(name=longName)),1.5f)
        listOf("Unidade","Data","Horário","Rotina").forEach(::assertWholeLabel)
        listOf(longName,"02/11/2026","12:00","Sem repetição automática").forEach(::assertValue)
        label("Unidade").performScrollTo()
        capture("fonte-ampliada")
        compose.onNode(hasText("Confirmar agendamento") and hasClickAction()).performScrollTo().performClick()
        assertEquals(1,confirmations)
    }
    @Test fun routineAndRescheduleKeepReadableReviewFields() {
        val routine=RoutineChoice(LocalDate.of(2026,11,6),30,center.timeZoneId,0L)
        open(initial.copy(routine=routine),1.5f)
        listOf("Unidade","Data","Horário","Rotina","Início da rotina").forEach(::assertWholeLabel)
        assertValue("A cada 30 dias");assertValue("06/11/2026")
    }
    @Test fun reschedulingAtDefaultScaleKeepsSourceAndDestinationVisible() {
        val origin=RescheduleOrigin("23","50","Unidade de origem sintética",Instant.parse("2026-11-01T15:00:00Z"),center.timeZoneId)
        open(initial.copy(rescheduleOrigin=origin),1f)
        listOf("Unidade","Data","Horário","Rotina").forEach(::assertWholeLabel)
        assertValue(center.name);assertValue("Preferência atual preservada")
        compose.onNodeWithText(origin.centerName).performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Confirmar remarcação") and hasClickAction()).performScrollTo().assertIsEnabled()
        assertEquals(0,confirmations)
    }
}
