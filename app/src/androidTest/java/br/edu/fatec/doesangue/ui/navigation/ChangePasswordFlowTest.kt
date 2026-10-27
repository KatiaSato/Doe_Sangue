package br.edu.fatec.doesangue.ui.navigation

import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.presentation.auth.ChangePasswordUiState
import br.edu.fatec.doesangue.ui.screens.profile.SecurityScreen
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tela, ViewModel e navegação reais; apenas fontes fictícias, sem rede ou credenciais reais. */
@RunWith(AndroidJUnit4::class)
class ChangePasswordFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private val user = AuthenticatedUser("test-user-1")
    private var session: AuthenticatedUser? = user
    private var signOuts = 0
    private val requests = mutableListOf<Pair<String, String>>()
    private var answer: suspend () -> Result<Unit> = { Result.success(Unit) }
    private val oldPassword = "SyntheticOld1!"
    private val newPassword = "SyntheticNew2!"
    private val labels = listOf("Senha atual", "Nova senha", "Confirmar nova senha")

    private fun openSecurity() {
        val auth = object : AuthRepository by FakeAuthRepository() {
            override suspend fun restoreSession() = Result.success(session)
            override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
                requests += currentPassword to newPassword
                return answer()
            }
            override suspend fun signOut(): Result<Unit> {
                signOuts++
                session = null
                return Result.success(Unit)
            }
        }
        val container = AppContainer(routineRepositoryOverride = br.edu.fatec.doesangue.ui.navigation.FakeReminderRoutineRepository(),
            authRepositoryOverride = auth,
            donorProfileRepositoryOverride = FakeDonorProfileRepository(),
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            appointmentRepositoryOverride = FakeAppointmentRepository(),
            campaignRepositoryOverride = FakeCampaignRepository(),
            bloodStockRepositoryOverride = FakeBloodStockRepository(),
            donationHistoryRepositoryOverride = FakeDonationHistoryRepository(),
        )
        compose.setContent {
            nav = rememberNavController()
            DoeSangueTheme { FeatureTestHost(container, nav) }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Home>() == true }
        compose.onAllNodesWithText("Perfil").onLast().performClick()
        tap("Configurações")
        tap("Segurança")
        assertSecurity()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun replace(label: String, value: String) {
        compose.onNodeWithText(label).performScrollTo().performTextReplacement(value)
        compose.waitForIdle()
    }

    private fun fill() {
        replace(labels[0], oldPassword)
        replace(labels[1], newPassword)
        replace(labels[2], newPassword)
    }

    private fun assertSecurity() = compose.runOnIdle {
        assertTrue(nav.currentDestination?.hasRoute<AppRoute.Security>() == true)
    }

    private fun assertBlankFields() {
        labels.forEach { compose.onNodeWithText(it).assertTextContains("") }
        labels.forEach { assertRendered(it, "", false) }
        compose.onAllNodesWithText("Mostrar").assertCountEquals(3)
        compose.onAllNodesWithText("Ocultar").assertCountEquals(0)
    }

    private fun assertRendered(label: String, value: String, visible: Boolean) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        assertEquals(if (visible) value else "\u2022".repeat(value.length),
            layouts.single().layoutInput.text.text)
    }

    @Suppress("DEPRECATION")
    private fun saveAndObserveToast() {
        val event = InstrumentationRegistry.getInstrumentation().uiAutomation.executeAndWaitForEvent(
            { tap("Alterar senha"); compose.waitForIdle() },
            { it.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
                it.text.any { text -> text.toString() == "Senha salva com sucesso." } },
            5_000,
        )
        event.recycle()
    }

    @Test fun invalidFieldsStayOnSecurityAndNeverCallRepository() {
        openSecurity()
        tap("Alterar senha")
        compose.onNodeWithText("Informe sua senha atual.").assertExists()
        replace(labels[0], oldPassword)
        replace(labels[1], "curta")
        replace(labels[2], "curta")
        tap("Alterar senha")
        // A orientação também existe como texto fixo; a segunda ocorrência é o erro.
        compose.onAllNodesWithText("Use ao menos 8 caracteres com maiúscula, minúscula, número e símbolo.")
            .assertCountEquals(2)
        replace(labels[1], newPassword)
        tap("Alterar senha")
        compose.onNodeWithText("As senhas devem ser iguais.").assertExists()
        replace(labels[1], oldPassword)
        replace(labels[2], oldPassword)
        tap("Alterar senha")
        compose.onNodeWithText("Escolha uma senha diferente da atual.").assertExists()
        assertTrue(requests.isEmpty())
        assertSecurity()
    }

    @Test fun visibilityIsIndependentAndNetworkFailurePreservesValues() {
        openSecurity()
        fill()
        assertRendered(labels[0], oldPassword, false)
        assertRendered(labels[1], newPassword, false)
        assertRendered(labels[2], newPassword, false)
        compose.onAllNodesWithText("Mostrar").onFirst().performScrollTo().performClick()
        assertRendered(labels[0], oldPassword, true)
        assertRendered(labels[1], newPassword, false)
        compose.onAllNodesWithText("Mostrar").onFirst().performScrollTo().performClick()
        assertRendered(labels[1], newPassword, true)
        assertRendered(labels[2], newPassword, false)
        compose.onAllNodesWithText("Mostrar").onFirst().performScrollTo().performClick()
        assertRendered(labels[2], newPassword, true)
        compose.onAllNodesWithText("Ocultar").onFirst().performScrollTo().performClick()
        assertRendered(labels[0], oldPassword, false)
        assertRendered(labels[1], newPassword, true)
        answer = { Result.failure(AuthFailure(AuthError.NETWORK)) }
        tap("Alterar senha")
        compose.onNodeWithText("Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.")
            .assertExists()
        assertEquals(listOf(oldPassword to newPassword), requests)
        assertRendered(labels[0], oldPassword, false)
        assertRendered(labels[1], newPassword, true)
        assertRendered(labels[2], newPassword, true)
        assertSecurity()
    }

    @Test fun pendingSaveBlocksExitAndRetryConfirmsSuccessWithoutSigningOut() {
        openSecurity()
        fill()
        val pending = CompletableDeferred<Result<Unit>>()
        answer = { pending.await() }
        try {
            tap("Alterar senha")
            labels.forEach { compose.onNodeWithText(it).assertIsNotEnabled() }
            compose.onAllNodesWithText("Mostrar").assertCountEquals(3)
            compose.onAllNodesWithText("Mostrar").fetchSemanticsNodes().indices.forEach {
                compose.onAllNodesWithText("Mostrar")[it].assertIsNotEnabled()
            }
            compose.onNodeWithText("Salvando…").assertIsNotEnabled()
            compose.onNodeWithText("‹").assertDoesNotExist()
            Espresso.closeSoftKeyboard()
            Espresso.pressBack()
            compose.waitForIdle()
            assertSecurity()
            assertEquals(1, requests.size)
            compose.runOnIdle { pending.complete(Result.failure(AuthFailure(AuthError.CURRENT_PASSWORD_INCORRECT))) }
            compose.waitForIdle()
            compose.onNodeWithText("Senha atual incorreta.").assertExists()
            labels.forEach { compose.onNodeWithText(it).assertIsEnabled() }
            compose.onNodeWithText("‹").assertExists()
            answer = { Result.success(Unit) }
            saveAndObserveToast()
            compose.runOnIdle {
                assertTrue(nav.currentDestination?.hasRoute<AppRoute.Settings>() == true)
                assertEquals(user, session)
                assertEquals(0, signOuts)
                assertEquals(2, requests.size)
            }
            tap("Segurança")
            assertBlankFields()
            compose.onNodeWithText("Alterar senha").assertIsEnabled()
            assertEquals(2, requests.size)
        } finally {
            pending.complete(Result.success(Unit))
        }
    }

    @Test fun unavailableSessionAndUnknownFailureShowGuidanceWithoutFalseSuccess() {
        openSecurity()
        fill()
        answer = { Result.failure(AuthFailure(AuthError.SESSION_UNAVAILABLE)) }
        tap("Alterar senha")
        compose.onNodeWithText("Sua sessão não está disponível. Entre novamente para continuar.").assertExists()
        assertSecurity()
        answer = { Result.failure(IllegalStateException("synthetic-private-detail")) }
        tap("Alterar senha")
        compose.onNodeWithText("Não foi possível alterar sua senha. Tente novamente.").assertExists()
        compose.onNodeWithText("synthetic-private-detail").assertDoesNotExist()
        assertEquals(2, requests.size)
        assertEquals(0, signOuts)
        assertSecurity()
    }

    @Test fun returningBeforeSaveDiscardsPasswordsAndVisibility() {
        openSecurity()
        fill()
        compose.onAllNodesWithText("Mostrar").onFirst().performScrollTo().performClick()
        tap("‹")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.Settings>() == true) }
        tap("Segurança")
        assertBlankFields()
        assertTrue(requests.isEmpty())
        assertEquals(user, session)
    }

    @Test fun savedStateRestorationDoesNotRestorePasswordsOrVisibility() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            DoeSangueTheme {
                SecurityScreen(ChangePasswordUiState(), { _, _ , _ -> fail("Unexpected save") }, {})
            }
        }
        fill()
        compose.onAllNodesWithText("Mostrar").onFirst().performScrollTo().performClick()
        assertRendered(labels[0], oldPassword, true)
        restoration.emulateSavedInstanceStateRestore()
        assertBlankFields()
    }
}
