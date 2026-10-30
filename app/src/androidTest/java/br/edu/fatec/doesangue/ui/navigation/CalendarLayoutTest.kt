package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.presentation.scheduling.CalendarAvailabilityUiState
import br.edu.fatec.doesangue.ui.components.DonationDatePickerDialog
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Calendar geometry and interaction; in-memory dates, no API/Auth/reservation. */
class CalendarLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val november = YearMonth.of(2026,11)
    private val locale get() = Locale.getDefault()
    private val queries = mutableListOf<YearMonth>()
    private var confirmed:LocalDate? = null
    private var expectedFontScale = 1f

    private fun loaded(month:YearMonth) = CalendarAvailabilityUiState(month=month,
        dates=(1..month.lengthOfMonth()).map { month.atDay(it) }.toSet())

    private fun open(month:YearMonth, scale:Float, selected:LocalDate?=null) {
        expectedFontScale=scale
        val availability=mutableStateOf(loaded(month))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(scale)) {
                // A Dialog uses its host View's context; keep the override in that context too.
                AndroidView(modifier=Modifier.fillMaxSize(), factory={ configuredContext ->
                    ComposeView(configuredContext).apply {
                        setContent {
                            DoeSangueTheme {
                                DonationDatePickerDialog(selected,month.atDay(1),availability.value,{ current ->
                                    queries += current
                                    if (current != availability.value.month) availability.value=loaded(current)
                                },{}, { confirmed=it },{})
                            }
                        }
                    }
                },onRelease={ it.disposeComposition() })
            }
        }
        compose.waitForIdle()
    }

    private fun dateNode(date:LocalDate):SemanticsNodeInteraction {
        val monthName=date.month.getDisplayName(TextStyle.FULL,locale)
        val day=Regex("(?<!\\d)${date.dayOfMonth}(?!\\d)")
        val year=Regex("(?<!\\d)${date.year}(?!\\d)")
        val matcher=SemanticsMatcher("date $date") { node ->
            val labels=node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty()+
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            labels.any { it.contains(monthName,ignoreCase=true) && day.containsMatchIn(it) && year.containsMatchIn(it) }
        }
        return compose.onNode(matcher and hasClickAction() and hasAnyAncestor(isDialog()))
    }

    private fun assertWeekdaySeparation(month:YearMonth) {
        val first=dateNode(month.atDay(1))
        if (!first.isDisplayed()) first.performScrollTo()
        compose.waitForIdle()
        first.assertIsDisplayed()
        assertEquals("Font scale must also reach the dialog window",expectedFontScale,
            first.fetchSemanticsNode().layoutInfo.density.fontScale,0.01f)
        val weekdayNames=DateFormatSymbols(locale).weekdays.filter { it.isNotBlank() }.map { it.lowercase(locale) }
        val weekdayMatcher=SemanticsMatcher("weekday header") { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
                .any { it.lowercase(locale) in weekdayNames }
        }
        val headers=compose.onAllNodes(weekdayMatcher and hasAnyAncestor(isDialog()))
        headers.assertCountEquals(7)
        // requiredHeight may draw outside clipped semantics bounds; inspect actual placement.
        val headerBottom=headers.fetchSemanticsNodes().maxOf { it.positionInRoot.y + it.size.height }
        val firstTop=first.fetchSemanticsNode().positionInRoot.y
        assertTrue("Weekday header overlaps first date: headerBottom=$headerBottom, firstDateTop=$firstTop",
            firstTop >= headerBottom - 0.5f)
        compose.onNodeWithText("Cancelar").assertIsDisplayed()
        compose.onNodeWithText("Confirmar data").assertIsDisplayed()
    }

    private fun capture(name:String) {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null),"calendar-layout-$name.png").outputStream().use {
            compose.onNode(isDialog()).captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }

    private fun revealDate(date:LocalDate):SemanticsNodeInteraction {
        val target=dateNode(date)
        val body=compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
            and hasAnyAncestor(isDialog()))
        repeat(5) {
            if (target.isDisplayed()) return target
            // A date's closest scroll ancestor is the horizontal month pager.
            // Use a real vertical gesture on the dialog body to reach its last row.
            body.performTouchInput { swipeUp() }
            compose.waitForIdle()
        }
        return target.assertIsDisplayed()
    }

    @Test fun novemberWithoutSelectionAtPhoneFontScaleHasNoWeekdayOverlap() {
        open(november,1.15f);assertWeekdaySeparation(november);capture("novembro-sem-selecao")
    }
    @Test fun novemberWithSelectedDateHasNoWeekdayOverlapAndConfirmsSameDate() {
        val selected=november.atDay(25)
        open(november,1.15f,selected);assertWeekdaySeparation(november)
        compose.onNodeWithText("Confirmar data").assertIsEnabled().performClick()
        assertEquals(selected,confirmed)
    }
    @Test fun fourWeekMonthKeepsWeekdayHeaderSeparate() {
        val month=YearMonth.of(2026,2)
        open(month,1.15f);assertWeekdaySeparation(month)
    }
    @Test fun sixWeekMonthKeepsFirstAndLastRowsReachable() {
        val month=YearMonth.of(2026,8)
        open(month,1.15f);assertWeekdaySeparation(month)
        revealDate(month.atEndOfMonth()).assertIsDisplayed().assertIsEnabled().performClick()
        compose.onNodeWithText("Confirmar data").assertIsDisplayed().performClick()
        assertEquals(month.atEndOfMonth(),confirmed)
    }
    @Test fun defaultFontScaleHasNoWeekdayOverlap() {
        open(november,1f);assertWeekdaySeparation(november)
    }
    @Test fun enlargedFontKeepsGridAndActionsAccessible() {
        open(november,1.5f);assertWeekdaySeparation(november)
        revealDate(november.atEndOfMonth()).performClick()
        compose.onNodeWithText("Confirmar data").assertIsDisplayed().performClick()
        assertEquals(november.atEndOfMonth(),confirmed);capture("fonte-ampliada")
    }
    @Test fun monthNavigationKeepsWeekdaysSeparateAndRequestsNewMonth() {
        open(november,1.15f)
        compose.onNode(hasContentDescription("next month",substring=true,ignoreCase=true) or
            hasContentDescription("próximo mês",substring=true,ignoreCase=true)).performClick()
        val next=november.plusMonths(1)
        compose.waitUntil(10_000) { queries.lastOrNull()==next }
        assertWeekdaySeparation(next)
    }
    @Test fun yearPickerWorksInsideScrollableContent() {
        open(november,1.15f)
        val monthName=november.month.getDisplayName(TextStyle.FULL,locale)
        val heading=SemanticsMatcher("month and year heading") { node ->
            node.config.getOrNull(SemanticsProperties.Text).orEmpty().any { label ->
                label.text.contains(monthName,ignoreCase=true) &&
                    Regex("\\d+").findAll(label.text).map { it.value }.toList()==listOf("2026")
            }
        }
        val control=compose.onNode(heading and hasClickAction())
        if (!control.isDisplayed()) control.performScrollTo()
        control.performClick()
        compose.waitForIdle()
        // Material3 exposes an accessibility phrase ("Navigate to year 2027"), not only digits.
        compose.onNode(hasText("2027",substring=true) and hasClickAction() and hasAnyAncestor(isDialog()))
            .performScrollTo().assertIsDisplayed().performClick()
        val next=november.plusYears(1)
        compose.waitUntil(10_000) { queries.lastOrNull()==next }
        assertWeekdaySeparation(next)
    }
}
