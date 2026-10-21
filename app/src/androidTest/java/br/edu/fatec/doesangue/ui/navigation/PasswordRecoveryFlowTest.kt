package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.AuthError
import br.edu.fatec.doesangue.domain.model.AuthFailure
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.presentation.auth.PasswordRecoveryViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tela, factory e grafo reais com Auth fictício: não usa rede nem envia e-mail. */
@RunWith(AndroidJUnit4::class)
class PasswordRecoveryFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private val requests = mutableListOf<String>()
    private var answer: suspend () -> Result<Unit> = { Result.success(Unit) }
    private val acceptedMessage = "Solicitação recebida. Se este e-mail estiver cadastrado, " +
        "confira sua caixa de entrada e o spam para as instruções."

    private fun openRecovery() {
        val repository = object : AuthRepository by FakeAuthRepository() {
            override suspend fun requestPasswordReset(email: String): Result<Unit> {
                requests += email
                return answer()
            }
        }
        val container = AppContainer(
            authRepositoryOverride = repository,
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            donorProfileRepositoryOverride = FakeDonorProfileRepository(),
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
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true }
        compose.onNodeWithText("Já tenho uma conta").performClick()
        compose.onNodeWithText("Esqueci minha senha").performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(requests.isEmpty())
    }

    private fun send() {
        compose.onNodeWithText("Enviar link").performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test fun localValidationShowsSpecificMessagesWithoutCallingAuth() {
        openRecovery()
        send()
        compose.onNodeWithText("Informe seu e-mail.").assertExists()
        compose.onNodeWithText("E-mail").performTextInput("formato-invalido")
        send()
        compose.onNodeWithText("Informe um e-mail válido.").assertExists()
        compose.onNodeWithText(acceptedMessage).assertDoesNotExist()
        compose.onNodeWithText("Enviar link").assertIsEnabled()
        compose.runOnIdle {
            assertTrue(requests.isEmpty())
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.ForgotPassword>() == true)
        }
        capture("formato-invalido")
    }

    @Test fun acceptedRequestIsNeutralBlocksRepeatAndEditingAllowsAnotherRequest() {
        openRecovery()
        compose.onNodeWithText("E-mail").performTextInput("  doador.teste@example.com  ")
        send()
        compose.onNodeWithText(acceptedMessage).assertExists()
        compose.onNodeWithText("Enviar link").assertIsNotEnabled()
        compose.onNodeWithText("E-mail").assertIsEnabled()
        compose.runOnIdle {
            assertEquals(listOf("doador.teste@example.com"), requests)
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.ForgotPassword>() == true)
        }
        capture("solicitacao-aceita")
        compose.onNodeWithText("E-mail").performTextReplacement("outro.teste@example.com")
        compose.onNodeWithText(acceptedMessage).assertDoesNotExist()
        compose.onNodeWithText("Enviar link").assertIsEnabled()
        send()
        compose.runOnIdle {
            assertEquals(listOf("doador.teste@example.com", "outro.teste@example.com"), requests)
        }
    }

    @Test fun sendingDisablesFieldAndButtonAndNetworkFailureAllowsRetry() {
        val pending = CompletableDeferred<Result<Unit>>()
        answer = { pending.await() }
        openRecovery()
        compose.onNodeWithText("E-mail").performTextInput("doador.teste@example.com")
        send()
        compose.onNodeWithText("Enviando…").assertIsNotEnabled()
        compose.onNodeWithText("E-mail").assertIsNotEnabled()
        compose.onNodeWithText(acceptedMessage).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, requests.size)
            pending.complete(Result.failure(AuthFailure(AuthError.NETWORK)))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Não foi possível conectar ao serviço. Verifique sua conexão e tente novamente.")
            .assertExists()
        compose.onNodeWithText("E-mail").assertIsEnabled()
        compose.onNodeWithText("Enviar link").assertIsEnabled()
        compose.runOnIdle { answer = { Result.success(Unit) } }
        send()
        compose.onNodeWithText(acceptedMessage).assertExists()
        compose.runOnIdle { assertEquals(2, requests.size) }
    }

    @Test fun structuredErrorsAndUnknownFailureAreVisibleWithoutInternalDetails() {
        openRecovery()
        compose.onNodeWithText("E-mail").performTextInput("doador.teste@example.com")
        val cases = listOf(
            AuthFailure(AuthError.RATE_LIMITED) to "Muitas tentativas. Aguarde um pouco e tente novamente.",
            AuthFailure(AuthError.EMAIL_NOT_AUTHORIZED) to
                "O envio de recuperação não está permitido para este endereço. Tente outro e-mail.",
            IllegalStateException("detalhe-interno-ficticio") to
                "Não foi possível solicitar a recuperação. Tente novamente.",
        )
        for ((failure, message) in cases) {
            compose.runOnIdle { answer = { Result.failure(failure) } }
            send()
            compose.onNodeWithText(message).assertExists()
            compose.onNodeWithText("detalhe-interno-ficticio").assertDoesNotExist()
            compose.onNodeWithText(acceptedMessage).assertDoesNotExist()
        }
        compose.runOnIdle { assertEquals(3, requests.size) }
    }

    @Test fun returningToLoginDuringRequestDoesNotNavigateOnLateReplyAndNewEntryStartsFresh() {
        val pending = CompletableDeferred<Result<Unit>>()
        answer = { withContext(NonCancellable) { pending.await() } }
        openRecovery()
        compose.onNodeWithText("E-mail").performTextInput("doador.teste@example.com")
        send()
        lateinit var oldModel: PasswordRecoveryViewModel
        compose.runOnIdle {
            oldModel = ViewModelProvider(nav.currentBackStackEntry!!)[PasswordRecoveryViewModel::class.java]
        }
        compose.onNodeWithText("Voltar para o login").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Login>() == true)
            pending.complete(Result.success(Unit))
        }
        compose.waitForIdle()
        compose.onNodeWithText(acceptedMessage).assertDoesNotExist()
        compose.onNodeWithText("Esqueci minha senha").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            val newModel = ViewModelProvider(nav.currentBackStackEntry!!)[PasswordRecoveryViewModel::class.java]
            assertNotSame(oldModel, newModel)
            assertEquals("", newModel.uiState.value.email)
            assertFalse(newModel.uiState.value.isSending)
            assertFalse(newModel.uiState.value.requestAccepted)
            assertEquals(1, requests.size)
        }
        compose.onNodeWithText("Enviar link").assertIsEnabled()
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "recovery-request-test").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
