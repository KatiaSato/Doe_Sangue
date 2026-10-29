package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.presentation.home.HomeViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.io.File

/** Fluxos reais com repositories sintéticos, sem rede ou gravação remota. */
@RunWith(AndroidJUnit4::class)
class HomeReadFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private lateinit var home: HomeViewModel
    private val clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneOffset.UTC)
    private var stocksAnswer: suspend () -> Result<List<BloodStock>> = { Result.success(stocks()) }
    private var historyAnswer: suspend () -> Result<List<DonationRecord>> = { Result.success(records()) }
    private var stockCalls = 0
    private var historyCalls = 0
    private var historyCancellations = 0
    private var owner = "a"
    private var hasFutureAppointment = true
    private var previewFontScale by mutableFloatStateOf(1f)
    private val appointments = object : AppointmentRepository {
        override suspend fun getMyAppointments() = Result.success(if (!hasFutureAppointment) emptyList() else listOf(
            summary("past", "2026-10-01T11:00:00Z"),
            summary("cancelled", "2026-10-16T11:00:00Z", AppointmentStatus.CANCELLED),
            summary("future-2", "2026-10-22T11:00:00Z"),
            summary("future-1", "2026-10-19T11:00:00Z"),
        ))
        override suspend fun createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment> =
            Result.failure(UnsupportedOperationException())
        override suspend fun cancelAppointment(appointmentId: String): Result<Appointment> =
            Result.failure(UnsupportedOperationException())
    }

    private fun open() {
        val stockRepository = object : BloodStockRepository {
            override suspend fun getBloodStocks(): Result<List<BloodStock>> { stockCalls++; return stocksAnswer() }
        }
        val historyRepository = object : DonationHistoryRepository {
            override suspend fun getMyDonations(): Result<List<DonationRecord>> {
                historyCalls++
                return try { historyAnswer() } catch (cancelled: CancellationException) {
                    historyCancellations++
                    throw cancelled
                }
            }
        }
        val container = AppContainer(routineRepositoryOverride = br.edu.fatec.doesangue.ui.navigation.FakeReminderRoutineRepository(),
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            authRepositoryOverride = object : AuthRepository by FakeAuthRepository() {
                override suspend fun signIn(email: String, password: String): Result<AuthenticatedUser> {
                    owner = if (email.startsWith("c@")) "c" else "a"
                    return Result.success(AuthenticatedUser("test-user-$owner"))
                }
            },
            donorProfileRepositoryOverride = FakeDonorProfileRepository(),
            appointmentRepositoryOverride = appointments,
            campaignRepositoryOverride = FakeCampaignRepository(),
            bloodStockRepositoryOverride = stockRepository,
            donationHistoryRepositoryOverride = historyRepository,
        )
        val homeFactory = viewModelFactory {
            initializer {
                HomeViewModel(container.donorProfileRepository, appointments, container.campaignRepository,
                    stockRepository, clock).also { home = it }
            }
        }
        compose.setContent {
            nav = rememberNavController()
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, previewFontScale)) {
                DoeSangueTheme { FeatureTestHost(container, nav, homeFactory = homeFactory) }
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true }
        tap("Já tenho uma conta")
        compose.onNodeWithText("E-mail").performTextInput("teste@example.com")
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        tap("Entrar")
    }

    @Test fun homeUsesNameNearestFutureActiveBookingAndIdDetail() {
        open()
        compose.onNodeWithText("Olá, Doador de teste").assertExists()
        compose.onNodeWithContentDescription("19/10/2026").assertExists()
        compose.onAllNodesWithText("Agendar doação").assertCountEquals(1)
        compose.onNodeWithText("Meus agendamentos").assertDoesNotExist()
        compose.onNodeWithText("Agendamento past").assertDoesNotExist()
        compose.onNodeWithText("Agendamento cancelled").assertDoesNotExist()
        screenshot("home")
        compose.onNode(hasText("Doe sangue, doe vida!") and hasClickAction()).performScrollTo()
        screenshot("home_conteudo")
        compose.runOnIdle { previewFontScale = 1.5f }
        compose.onNodeWithText("Olá, Doador de teste").performScrollTo().assertIsDisplayed()
        screenshot("home_fonte_ampliada")
        compose.onNode(hasText("Doe sangue, doe vida!") and hasClickAction()).performScrollTo().assertIsDisplayed()
        screenshot("home_conteudo_fonte_ampliada")
        compose.runOnIdle { previewFontScale = 1f }
        tap("08:00 · Ver detalhes")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.AppointmentDetailById>() == true) }
        compose.onNodeWithText("Agendamento future-1").assertExists()
    }

    @Test fun campaignPreviewOpensPersistedDetail() {
        open()
        tap("Doe sangue, doe vida!")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.CampaignDetailById>() == true) }
        compose.onNodeWithText("Doe sangue, doe vida!").assertExists()
    }

    @Test fun stockFailureKeepsBookingAndRetryDoesNotDuplicatePendingRequests() {
        stocksAnswer = { Result.failure(IllegalStateException()) }
        open()
        compose.onNodeWithText("Próximo agendamento").assertExists()
        compose.onNodeWithText("Não foi possível carregar o estoque demonstrativo.").assertExists()
        val pending = CompletableDeferred<Result<List<BloodStock>>>()
        stocksAnswer = { pending.await() }
        tap("Tentar novamente")
        compose.runOnIdle { repeat(3) { home.load() }; assertEquals(2, stockCalls) }
        compose.runOnIdle { pending.complete(Result.success(stocks())) }
        compose.waitForIdle()
        compose.onNode(hasText("Crítico") and hasClickAction()).assertExists()
    }

    @Test fun publishedStockExposesSourceAndLocalUpdate() {
        open()
        tap("Ver todos")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.BloodStock>() == true) }
        compose.onNodeWithText("O- • Crítico").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("A+, A-, O+, O- · atualizado em 15/10/2026 • 09:00").assertExists()
        compose.onAllNodesWithText("Fonte: Fonte sintética").assertCountEquals(1)
        screenshot("estoque")
    }

    @Test fun historyHasOwnRecordsAndCalculatedTotalNotBookings() {
        open()
        compose.onAllNodesWithText("Doações").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Doação fictícia • Registro h-1").assertExists()
        compose.onNodeWithText("Total de registros fictícios").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("2").assertExists()
        screenshot("historico")
        compose.onNodeWithText("Agendamento future-1").assertDoesNotExist()
        tap("Agendamentos")
        compose.onNodeWithText("Agendamento future-1").assertExists()
    }

    @Test fun historyErrorRetryEmptyAndTotalZero() {
        historyAnswer = { Result.failure(IllegalStateException()) }
        open()
        compose.onAllNodesWithText("Doações").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Não foi possível carregar seu histórico demonstrativo. Tente novamente.").assertExists()
        historyAnswer = { Result.success(emptyList()) }
        tap("Tentar novamente")
        compose.onNodeWithText("Nenhum registro de doação fictícia encontrado.").assertExists()
        compose.onNodeWithText("0").assertExists()
        compose.runOnIdle { assertEquals(2, historyCalls) }
    }

    @Test fun profileShortcutUsesActualBookingListAndPrivacyIsReachable() {
        open()
        compose.onAllNodesWithText("Perfil").onLast().performClick()
        compose.waitForIdle()
        tap("Meu agendamento")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.DonationsAppointments>() == true) }
        compose.onAllNodesWithText("Perfil").onLast().performClick()
        compose.waitForIdle()
        tap("Configurações"); tap("Sobre o app"); tap("Privacidade e termos")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.PrivacyTerms>() == true) }
        compose.onNodeWithText("Uso de demonstração").assertExists()
        screenshot("privacidade")
        tap("‹")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.About>() == true) }
    }

    @Test fun emptyStockIsExplicitOnHomeAndFullList() {
        stocksAnswer = { Result.success(emptyList()) }
        open()
        compose.onNodeWithText("Nenhum nível de estoque publicado.").assertExists()
        tap("Ver todos")
        compose.onNodeWithText("Nenhum nível de estoque publicado.").assertExists()
    }

    @Test fun stockListErrorCanRetryWithoutAffectingBooking() {
        open()
        stocksAnswer = { Result.failure(IllegalStateException()) }
        tap("Ver todos")
        compose.onNodeWithText("Não foi possível carregar o estoque demonstrativo. Tente novamente.").assertExists()
        stocksAnswer = { Result.success(stocks()) }
        tap("Tentar novamente")
        compose.onNodeWithText("O- • Crítico").assertExists()
        compose.runOnIdle { assertEquals(3, stockCalls) }
    }

    @Test fun noFutureBookingOffersScheduling() {
        hasFutureAppointment = false
        open()
        compose.onNodeWithText("Você não possui agendamentos futuros ativos.").assertExists()
        tap("Agendar doação")
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.ScheduleCenter>() == true) }
    }

    @Test fun leavingPendingHistoryCancelsReadAndReentryLoadsAgain() {
        val pending = CompletableDeferred<Result<List<DonationRecord>>>()
        historyAnswer = { pending.await() }
        open()
        compose.onAllNodesWithText("Doações").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Carregando histórico…").assertExists()
        compose.onAllNodesWithText("Início").onLast().performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, historyCancellations) }
        historyAnswer = { Result.success(records()) }
        compose.onAllNodesWithText("Doações").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Doação fictícia • Registro h-1").assertExists()
        compose.runOnIdle { assertEquals(2, historyCalls) }
    }

    @Test fun newLoginCannotShowPreviousAccountHistory() {
        historyAnswer = { Result.success(if (owner == "a") records() else records().take(1).map { it.copy(id = "c-1") }) }
        open()
        compose.onAllNodesWithText("Doações").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Doação fictícia • Registro h-1").assertExists()
        compose.onAllNodesWithText("Perfil").onLast().performClick()
        compose.waitForIdle()
        tap("Sair da conta"); tap("Já tenho uma conta")
        compose.onNodeWithText("E-mail").performTextInput("c@example.com")
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        tap("Entrar")
        compose.onAllNodesWithText("Doações").onLast().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Doação fictícia • Registro c-1").assertExists()
        compose.onNodeWithText("Doação fictícia • Registro h-1").assertDoesNotExist()
        compose.onNodeWithText("Doação fictícia • Registro h-2").assertDoesNotExist()
    }

    private fun tap(text: String) {
        val node = compose.onNode(hasText(text) and hasClickAction())
        if (!node.isDisplayed()) node.performScrollTo()
        node.performClick()
        compose.waitForIdle()
    }
    private fun stocks() = listOf(
        BloodType.O_POSITIVE to StockLevel.STABLE, BloodType.O_NEGATIVE to StockLevel.CRITICAL,
        BloodType.A_POSITIVE to StockLevel.ALERT, BloodType.A_NEGATIVE to StockLevel.CRITICAL,
    ).map { (type, level) ->
        BloodStock("1", "Unidade sintética", type, level, "Fonte sintética", clock.instant(), "America/Sao_Paulo")
    }
    private fun records() = listOf("h-1", "h-2").map {
        DonationRecord(it, "Unidade sintética", Instant.parse("2026-05-10T13:00:00Z"), "America/Sao_Paulo", "Fonte sintética")
    }
    private fun summary(id: String, start: String, status: AppointmentStatus = AppointmentStatus.SCHEDULED) =
        AppointmentSummary(Appointment(id, "test-user-1", "slot", status, clock.instant()),
            "Unidade sintética", Instant.parse(start), "America/Sao_Paulo")

    private fun screenshot(name: String) {
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "home_read_$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
