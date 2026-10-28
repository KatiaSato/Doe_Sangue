package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.edu.fatec.doesangue.ui.components.RoutineDatePickerDialog
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Calendário local real, sem contas, rede ou dados persistidos. */
class RoutineDatePickerTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026,11,10)
    private var confirmed: LocalDate? = null
    private var dismissed = 0
    private fun open(selected: LocalDate? = null, retained: LocalDate? = null) {
        compose.setContent { DoeSangueTheme {
            RoutineDatePickerDialog(selected,today,retained,{confirmed=it},{dismissed++})
        } }
    }
    private fun day(number: Int): SemanticsNodeInteraction {
        val pattern = Regex("(?<!\\d)$number(?!\\d)")
        val matcher = SemanticsMatcher("dia $number visível") { node ->
            val texts = node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty() +
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            texts.any(pattern::containsMatchIn)
        }
        val candidates = compose.onAllNodes(matcher and hasClickAction() and hasAnyAncestor(isDialog()))
        val visible = candidates.fetchSemanticsNodes().indices.filter {
            runCatching { candidates[it].assertIsDisplayed() }.isSuccess
        }
        check(visible.size == 1)
        return candidates[visible.single()]
    }
    @Test fun todayAndFutureAreSelectableAndConfirmationReturnsExactCivilDate() {
        open()
        compose.onNodeWithText("Usar data").assertIsNotEnabled()
        day(9).assertIsNotEnabled();day(10).assertIsEnabled()
        day(20).assertIsEnabled().performClick()
        compose.onNodeWithText("Usar data").assertIsEnabled().performClick()
        compose.runOnIdle {assertEquals(LocalDate.of(2026,11,20),confirmed);assertEquals(1,dismissed)}
    }
    @Test fun invalidPastInputIsNotSelectedAndCannotBeConfirmed() {
        open(today.minusDays(1))
        compose.onNodeWithText("Usar data").assertIsNotEnabled()
        day(9).assertIsNotEnabled()
        compose.runOnIdle {assertNull(confirmed)}
    }
    @Test fun originalPastDateMayBeKeptButOtherPastDatesStayDisabled() {
        val original=today.minusDays(5)
        open(original,original)
        day(5).assertIsEnabled();day(6).assertIsNotEnabled()
        compose.onNodeWithText("Usar data").assertIsEnabled().performClick()
        compose.runOnIdle {assertEquals(original,confirmed);assertEquals(1,dismissed)}
    }
    @Test fun cancelDiscardsTemporarySelectionWithoutConfirming() {
        open(today)
        day(20).performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle {assertNull(confirmed);assertEquals(1,dismissed)}
    }
}
