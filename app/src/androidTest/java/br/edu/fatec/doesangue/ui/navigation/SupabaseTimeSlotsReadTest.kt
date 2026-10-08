package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleDateScreen
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Leitura real, sem escrita, do seed acadêmico de 19/10/2026.
 * Executar explicitamente com enableSupabaseReadTests=true no ambiente de desenvolvimento.
 * A suíte normal continua independente da rede e ignora este teste.
 */
@RunWith(AndroidJUnit4::class)
class SupabaseTimeSlotsReadTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun onlySeedDateOffersTimeAndChangingDateClearsSelection() {
        assumeTrue(
            "Consulta remota exige execução explícita no ambiente de desenvolvimento.",
            InstrumentationRegistry.getArguments()
                .getString("enableSupabaseReadTests") == "true",
        )

        val container = AppContainer()
        val center = runBlocking {
            container.donationCenterRepository.getCenters().getOrThrow()
                .single { it.name == "Unidade Demo A" && it.city == "Cidade fictícia" }
        }

        compose.setContent {
            val model: SchedulingViewModel = viewModel(
                factory = container.schedulingViewModelFactory,
            )
            val state by model.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                model.selectCenter(center)
                model.selectDate(LocalDate.of(2026, 10, 18))
            }
            DoeSangueTheme {
                ScheduleDateScreen(
                    uiState = state,
                    onSelectDate = model::selectDate,
                    onSelectTime = model::selectTime,
                    onRetry = model::loadTimeSlots,
                    onBack = {},
                    onContinue = {},
                )
            }
        }

        assertEmptyDate()
        chooseDay("19")
        waitForText("08:00")
        compose.onNodeWithText("09:30").assertDoesNotExist()
        compose.onNodeWithText("Continuar").assertIsNotEnabled()
        compose.onNodeWithText("08:00").performClick().assertIsSelected()
        compose.onNodeWithText("Continuar").assertIsEnabled()

        chooseDay("20")
        assertEmptyDate()
        chooseDay("18")
        assertEmptyDate()

        // Voltar ao dia disponível não restaura a seleção antiga.
        chooseDay("19")
        waitForText("08:00")
        compose.onNodeWithText("08:00").assertIsNotSelected()
        compose.onNodeWithText("Continuar").assertIsNotEnabled()
    }

    private fun assertEmptyDate() {
        waitForText("Nenhum horário disponível para esta data.")
        compose.onNodeWithText("08:00").assertDoesNotExist()
        compose.onNodeWithText("Continuar").assertIsNotEnabled()
        compose.onNodeWithText("Não foi possível carregar os horários.").assertDoesNotExist()
    }

    private fun waitForText(text: String) {
        compose.waitUntil(30_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun chooseDay(day: String) {
        compose.onNode(hasText("/", substring = true) and hasClickAction()).performClick()
        val number = Regex("(?<!\\d)$day(?!\\d)")
        val matcher = SemanticsMatcher("dia $day no calendário") { node ->
            node.config.getOrNull(SemanticsProperties.Text)
                ?.any { number.containsMatchIn(it.text) } == true
        }
        compose.onNode(matcher and hasClickAction() and hasAnyAncestor(isDialog()))
            .performClick()
        compose.onNodeWithText("Confirmar data").performClick()
        compose.waitForIdle()
    }
}
