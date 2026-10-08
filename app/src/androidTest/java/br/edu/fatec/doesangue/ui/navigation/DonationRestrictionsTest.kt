package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.runtime.remember
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DonationRestrictionsTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private lateinit var restoration: StateRestorationTester
    private var openedUrl: String? = null
    private var browserUnavailable = false

    @Before
    fun open() {
        restoration = StateRestorationTester(compose)

        restoration.setContent {
            val appContainer = remember {
                AppContainer(
                    centerRepositoryOverride = FakeDonationCenterRepository(),
                    timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
                )
            }
            nav = rememberNavController()

            CompositionLocalProvider(
                LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) {
                        if (browserUnavailable) {
                            throw IllegalArgumentException("Sem navegador")
                        }
                        openedUrl = uri
                    }
                }
            ) {
                DoeSangueTheme {
                    AppNavHost(
                        navController = nav,
                        schedulingViewModelFactory =
                            appContainer.schedulingViewModelFactory,
                    )
                }
            }
        }

        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(10_000) {
            nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true
        }
        tap("Já tenho uma conta")
        tap("Entrar")
    }

    @Test fun categoriesShowListsSourcesAndReturnHome() {
        compose.onNodeWithText("Consultar medicamentos").assertDoesNotExist()
        compose.onNodeWithText("Antes de doar").performScrollTo().assertIsDisplayed()
        screenshot("inicio")
        tap("Consultar restrições")
        screenshot("categorias")
        val cases = listOf(
            Triple("Uso de medicamentos", "PrEP e PEP", "https://bvsms.saude.gov.br/bvs/saudelegis/gm/2026/prt11685_03_07_2026.html"),
            Triple("Idade e peso", "Primeira doação", "https://bvsms.saude.gov.br/bvs/saudelegis/gm/2026/prt11685_03_07_2026.html"),
            Triple("Doenças e sintomas", "Histórico de hepatite", "https://www.hemocentro.unicamp.br/perguntas-frequentes/criterios-para-doacao-de-sangue/"),
            Triple("Vacinas e procedimentos", "Endoscopia e colonoscopia", "https://portal.hemominas.mg.gov.br/hemominas-adota-novos-criterios-para-doacao-de-sangue"),
        )
        cases.forEachIndexed { index, (category, item, source) ->
            tap(category)
            compose.onNodeWithText("Restrições").assertIsDisplayed()
            compose.onNodeWithText(item).assertExists()
            compose.onNodeWithText("CALCULAR PRAZO").assertDoesNotExist()
            screenshot("lista_" + index)
            compose.onNodeWithText("Para mais informações, contate o hemocentro.")
                .performScrollTo().assertIsDisplayed()
            compose.onNode(hasText("Fonte:", substring = true) and hasClickAction())
                .performScrollTo().performClick()
            compose.runOnIdle { assertEquals(source, openedUrl) }
            compose.onNodeWithText("Conteúdo revisado em 06/10/2026")
                .performScrollTo().assertIsDisplayed()
            tap("Restrições")
            compose.onNodeWithText("Consultar restrições").assertExists()
        }
        tap("Início")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true) }
    }

    @Test fun updatedGuidanceKeepsConditionsVisible() {
        tap("Consultar restrições")
        tap("Idade e peso")
        compose.onNodeWithText("após os 70 anos, mediante avaliação médica", substring = true)
            .performScrollTo().assertIsDisplayed()
        tap("Restrições")
        tap("Vacinas e procedimentos")
        compose.onNodeWithText("o prazo não garante aptidão", substring = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("4 meses após a retirada", substring = true)
            .performScrollTo().assertIsDisplayed()
        tap("Restrições")
        tap("Uso de medicamentos")
        compose.onNodeWithText("Não interrompa a prevenção para doar", substring = true)
            .performScrollTo().assertIsDisplayed()
    }

    @Test fun detailRestoresAndSystemBackKeepsHierarchy() {
        tap("Consultar restrições")
        tap("Vacinas e procedimentos")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Piercings").assertExists()
        Espresso.pressBack()
        compose.onNodeWithText("Consultar restrições").assertExists()
        Espresso.pressBack()
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true) }
    }

    @Test fun scheduledHomeAndUnavailableBrowser() {
        compose.runOnIdle { nav.navigate(AppRoute.HomeScheduled) }
        tap("Consultar restrições")
        tap("Uso de medicamentos")
        browserUnavailable = true
        compose.onNode(hasText("Fonte:", substring = true) and hasClickAction())
            .performScrollTo().performClick()
        compose.onNodeWithText("Não foi possível abrir a fonte. Verifique se há um navegador disponível.")
            .performScrollTo().assertIsDisplayed()
        tap("Restrições")
        tap("Início")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.HomeScheduled>() == true) }
    }

    private fun tap(label: String) {
        val node = compose.onNode(hasText(label) and hasClickAction())
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        // Conclui a transição do NavHost antes de capturar os pixels.
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "restricoes_" + name + ".png")
        file.outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
