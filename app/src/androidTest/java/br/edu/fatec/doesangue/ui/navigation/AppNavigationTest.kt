package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Testa os grafos reais por cliques; não autentica nem persiste dados. */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var navController: NavHostController
    private lateinit var restoration: StateRestorationTester

    @Before
    fun openApp() {
        restoration = StateRestorationTester(compose)
        restoration.setContent {
            navController = rememberNavController()
            DoeSangueTheme { AppNavHost(navController) }
        }
        // Avança o relógio do Compose para concluir a animação/espera da Splash.
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(10_000) {
            navController.currentDestination?.hasRoute<AppRoute.Welcome>() == true
        }
        compose.waitForIdle()
    }

    @Test
    fun splashIsRemovedFromBackStack() {
        assertRoute<AppRoute.Welcome>()
        assertNoPreviousScreen()
    }

    @Test
    fun enteringMainRemovesAuthenticationHistory() {
        enterMain()
        assertNoPreviousScreen()
    }

    @Test
    fun passwordRecoveryReturnsToLogin() {
        tap("Já tenho uma conta")
        tap("Esqueci minha senha")
        assertRoute<AppRoute.ForgotPassword>()
        tap("‹")
        assertRoute<AppRoute.Login>()
    }

    @Test
    fun registrationCompletionRemovesRegistrationSteps() {
        tap("Começar")
        tap("Continuar")
        assertRoute<AppRoute.Register2>()
        tap("‹")
        assertRoute<AppRoute.Register1>()
        repeat(3) { tap("Continuar") }
        assertRoute<AppRoute.Register4>()
        tap("Criar minha conta")
        assertRoute<AppRoute.EmailVerification>()
        tap("Entendi")
        assertRoute<AppRoute.Login>()
        compose.runOnIdle {
            assertTrue(navController.previousBackStackEntry?.destination
                ?.hasRoute<AppRoute.Welcome>() == true)
        }
    }

    @Test
    fun rootArrowsReturnHomeWithoutAccumulatingTabs() {
        enterMain()
        listOf("Unidades", "Perfil", "Doações", "Agendar").forEach { label ->
            tapTab(label)
            assertNoPreviousScreen()
            tap("‹")
            assertRoute<AppRoute.Home>()
            assertNoPreviousScreen()
        }
        tapTab("Doações")
        tap("Agendamentos")
        assertRoute<AppRoute.DonationsAppointments>()
        tap("‹")
        assertRoute<AppRoute.Home>()
        assertNoPreviousScreen()
    }

    @Test
    fun internalBackKeepsThePreviousScreen() {
        enterMain()
        tapTab("Perfil")
        tap("Configurações")
        tap("Sobre o app")
        assertRoute<AppRoute.About>()
        tap("‹")
        assertRoute<AppRoute.Settings>()
        tap("‹")
        assertRoute<AppRoute.Profile>()
        tap("‹")
        assertRoute<AppRoute.Home>()
    }

    @Test
    fun schedulingCompletionClearsItsSteps() {
        enterMain()
        tapTab("Agendar")
        repeat(3) { tap("Continuar") }
        assertRoute<AppRoute.ScheduleConfirm>()
        // O título também contém este texto; selecionamos somente o botão.
        compose.onNode(hasText("Confirmar agendamento") and hasClickAction())
            .performScrollTo().performClick()
        assertRoute<AppRoute.ScheduleSuccess>()
        tap("Ir para o início")
        assertRoute<AppRoute.HomeScheduled>()
        assertNoPreviousScreen()
    }

    @Test
    fun cancellationReturnsToListWithoutReopeningOldDetail() {
        enterMain()
        tapTab("Doações")
        tap("Agendamentos")
        tap("Ver detalhes")
        tap("Cancelar agendamento")
        tap("Cancelar agendamento")
        assertRoute<AppRoute.AppointmentCancelled>()
        compose.runOnIdle { navController.popBackStack() }
        assertRoute<AppRoute.DonationsAppointments>()
        assertNoPreviousScreen()
    }

    @Test
    fun savedNavigationRestoresDestinationAndReturnPath() {
        enterMain()
        tapTab("Perfil")
        tap("Configurações")
        tap("Sobre o app")
        // Exercita o estado salvo do Compose; não simula morte real do processo.
        restoration.emulateSavedInstanceStateRestore()
        assertRoute<AppRoute.About>()
        tap("‹")
        assertRoute<AppRoute.Settings>()
    }

    private fun enterMain() {
        tap("Já tenho uma conta")
        tap("Entrar")
        assertRoute<AppRoute.Home>()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    private fun tapTab(text: String) {
        // A barra inferior aparece depois do cabeçalho com o mesmo texto.
        compose.onAllNodesWithText(text).onLast().performClick()
        compose.waitForIdle()
    }

    private inline fun <reified T : Any> assertRoute() {
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue("Destino esperado: ${T::class.simpleName}",
                navController.currentDestination?.hasRoute<T>() == true)
        }
    }

    private fun assertNoPreviousScreen() {
        compose.runOnIdle { assertNull(navController.previousBackStackEntry) }
    }
}
