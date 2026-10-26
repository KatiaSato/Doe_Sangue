package br.edu.fatec.doesangue.ui.navigation

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.model.GeoCoordinates
import br.edu.fatec.doesangue.domain.repository.DonationCenterRepository
import br.edu.fatec.doesangue.presentation.centers.CentersViewModel
import br.edu.fatec.doesangue.presentation.centers.CenterDetailViewModel
import br.edu.fatec.doesangue.presentation.centers.CenterDetailUiState
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Grafo, factory, ViewModel e tela reais; todas as fontes são fictícias e sem rede. */
@RunWith(AndroidJUnit4::class)
class CentersReadFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private lateinit var model: CentersViewModel
    private var calls = 0
    private var answer: suspend () -> Result<List<DonationCenter>> = { Result.success(rows) }
    private val rows = listOf(
        center("a", "Unidade Sintética A", "Cidade Alfa", "Endereço fictício A"),
        center("c", "Unidade Sintética C", "Cidade Gama", "Endereço fictício C"),
    )

    private fun openUnits() {
        val repository = object : DonationCenterRepository {
            override suspend fun getCenters(): Result<List<DonationCenter>> {
                calls++
                return answer()
            }
        }
        val container = AppContainer(
            centerRepositoryOverride = repository,
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            authRepositoryOverride = FakeAuthRepository(),
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
        compose.onNodeWithText("E-mail").performTextInput("teste@example.com")
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Unidades").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            model = ViewModelProvider(nav.currentBackStackEntry!!)[CentersViewModel::class.java]
            assertEquals(1, calls)
        }
    }

    @Test fun showsReturnedNamesCitiesAndAddressesWithoutPrototypeDistances() {
        openUnits()
        rows.forEach { row ->
            compose.onNodeWithText(row.name).assertExists()
            compose.onNodeWithText(row.city).assertExists()
            compose.onNodeWithText(row.address).assertExists()
        }
        compose.onNodeWithText("Hemocentro Campinas").assertDoesNotExist()
        compose.onNodeWithText("4,8 km").assertDoesNotExist()
        compose.onNodeWithText("Ver no mapa").assertDoesNotExist()
        compose.onNodeWithText("Carregando unidades…").assertDoesNotExist()
        compose.onNodeWithText("Nenhuma unidade disponível no momento.").assertDoesNotExist()
    }

    @Test fun emptyResponseShowsEmptyStateWithoutPrototypeCards() {
        answer = { Result.success(emptyList()) }
        openUnits()
        compose.onNodeWithText("Nenhuma unidade disponível no momento.").assertIsDisplayed()
        compose.onNodeWithText("Hemocentro Campinas").assertDoesNotExist()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
        compose.runOnIdle { assertTrue(model.uiState.value.centers.isEmpty()) }
    }

    @Test fun failureCanRetryWithLoadingAndNoConcurrentRequests() {
        answer = { Result.failure(IllegalStateException("detalhe-interno-ficticio")) }
        openUnits()
        compose.onNodeWithText("Não foi possível carregar as unidades. Tente novamente.")
            .assertIsDisplayed()
        compose.onNodeWithText("detalhe-interno-ficticio").assertDoesNotExist()
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        compose.runOnIdle { answer = { pending.await() } }
        compose.onNodeWithText("Tentar novamente").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Carregando unidades…").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
        compose.onNodeWithText(rows.first().name).assertDoesNotExist()
        compose.runOnIdle {
            model.loadCenters()
            model.loadCenters()
            assertEquals(2, calls)
            pending.complete(Result.success(rows))
        }
        compose.waitForIdle()
        compose.onNodeWithText(rows.first().name).assertIsDisplayed()
        compose.onNodeWithText("Carregando unidades…").assertDoesNotExist()
        compose.runOnIdle {
            assertNull(model.uiState.value.errorMessage)
            assertFalse(model.uiState.value.isLoading)
            assertEquals(2, calls)
        }
    }

    @Test fun leavingRootIgnoresLateResponseAfterViewModelIsCleared() {
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        // Simula uma fonte que entrega uma resposta mesmo depois do cancelamento.
        answer = { withContext(NonCancellable) { pending.await() } }
        openUnits()
        val initial = model.uiState.value
        compose.onNodeWithText("Perfil").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Profile>() == true)
            pending.complete(Result.success(rows))
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(initial, model.uiState.value) }
        compose.onNodeWithText(rows.first().name).assertDoesNotExist()
    }

    @Test fun selectedIdLoadsFreshDataReturnsToListAndCanOpenAnotherCenter() {
        openUnits()
        val updated = rows[1].copy(
            name = "Unidade Sintética C atualizada",
            address = "Endereço atualizado C",
            phone = "Contato fictício C",
            openingHours = "Atendimento fictício C",
        )
        compose.runOnIdle { answer = { Result.success(listOf(updated, rows[0])) } }
        compose.onNodeWithText(rows[1].name).performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("c", nav.currentBackStackEntry?.toRoute<AppRoute.CenterDetailById>()?.centerId)
            assertEquals(2, calls)
        }
        compose.onNodeWithText(updated.name).assertExists()
        compose.onNodeWithText(updated.address).assertExists()
        compose.onNodeWithText(updated.city).assertExists()
        compose.onNodeWithText(updated.phone).assertExists()
        compose.onNodeWithText(updated.openingHours).assertExists()
        compose.onNodeWithText(rows[0].name).assertDoesNotExist()
        compose.onNodeWithText("Agendar nesta unidade").assertDoesNotExist()
        compose.onNodeWithText("‹").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Centers>() == true)
            assertEquals(2, calls)
        }
        compose.onNodeWithText(rows[0].name).performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("a", nav.currentBackStackEntry?.toRoute<AppRoute.CenterDetailById>()?.centerId)
            assertEquals(3, calls)
        }
        compose.onNodeWithText(rows[0].name).assertExists()
        compose.onNodeWithText(updated.name).assertDoesNotExist()
        compose.onNodeWithText("Agendar doação").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.ScheduleDate>() == true)
            val scheduling = ViewModelProvider(nav.getBackStackEntry<AppGraph.Scheduling>())[SchedulingViewModel::class.java]
            assertEquals(rows[0].id, scheduling.uiState.value.selectedCenter?.id)
        }
    }

    @Test fun absentSelectedIdAndUnknownIdShowUnavailableWithoutScheduling() {
        openUnits()
        // A unidade some do catálogo público entre a leitura da lista e a do detalhe.
        compose.runOnIdle { answer = { Result.success(listOf(rows[0])) } }
        compose.onNodeWithText(rows[1].name).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Unidade não encontrada ou indisponível.").assertIsDisplayed()
        compose.onNodeWithText(rows[0].name).assertDoesNotExist()
        compose.onNodeWithText(rows[1].name).assertDoesNotExist()
        compose.onNodeWithText("Agendar doação").assertDoesNotExist()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
        compose.onNodeWithText("‹").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            answer = { Result.success(emptyList()) }
            nav.navigate(AppRoute.CenterDetailById("id-inexistente"))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Unidade não encontrada ou indisponível.").assertIsDisplayed()
        compose.onNodeWithText("Hemocentro Campinas").assertDoesNotExist()
        compose.runOnIdle { assertEquals(3, calls) }
    }

    @Test fun missingOptionalFieldsShowFallbacksAndNoInventedServicesOrDistances() {
        openUnits()
        compose.onNodeWithText(rows[0].name).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Não informado.").assertExists()
        compose.onNodeWithText("Horários não informados.").assertExists()
        compose.onNodeWithText("4,8 km").assertDoesNotExist()
        compose.onNodeWithText("Tipos de atendimento").assertDoesNotExist()
        compose.onNodeWithText("Doação de sangue total").assertDoesNotExist()
    }

    @Test fun detailFailureRetryLoadingAndConcurrentGuard() {
        openUnits()
        compose.runOnIdle { answer = { Result.failure(IllegalStateException("falha-interna-ficticia")) } }
        compose.onNodeWithText(rows[0].name).performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Não foi possível carregar a unidade. Tente novamente.").assertExists()
        compose.onNodeWithText("falha-interna-ficticia").assertDoesNotExist()
        compose.onNodeWithText("Unidade não encontrada ou indisponível.").assertDoesNotExist()
        compose.onNodeWithText("Agendar doação").assertDoesNotExist()
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        compose.runOnIdle { answer = { pending.await() } }
        compose.onNodeWithText("Tentar novamente").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Carregando unidade…").assertIsDisplayed()
        compose.runOnIdle {
            val detail = ViewModelProvider(nav.currentBackStackEntry!!)[CenterDetailViewModel::class.java]
            detail.loadCenter()
            detail.loadCenter()
            assertEquals(3, calls)
            pending.complete(Result.success(rows))
        }
        compose.waitForIdle()
        compose.onNodeWithText(rows[0].name).assertExists()
        compose.onNodeWithText("Tentar novamente").assertDoesNotExist()
        compose.onNodeWithText("Carregando unidade…").assertDoesNotExist()
    }

    @Test fun returningFromDetailIgnoresResponseAfterItsViewModelIsCleared() {
        openUnits()
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        compose.runOnIdle { answer = { withContext(NonCancellable) { pending.await() } } }
        compose.onNodeWithText(rows[0].name).performScrollTo().performClick()
        compose.waitForIdle()
        lateinit var detail: CenterDetailViewModel
        compose.runOnIdle {
            detail = ViewModelProvider(nav.currentBackStackEntry!!)[CenterDetailViewModel::class.java]
        }
        compose.onNodeWithText("Carregando unidade…").assertIsDisplayed()
        compose.onNodeWithText("‹").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.Centers>() == true)
            pending.complete(Result.success(rows))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(CenterDetailUiState.Loading, detail.uiState.value)
            assertEquals(2, calls)
        }
        compose.onNodeWithText(rows[0].name).assertExists()
        compose.onNodeWithText(rows[1].name).assertExists()
    }

    @Test fun searchMatchesNameOrCityIgnoringCaseAccentsAndOuterSpacesWithoutNewReads() {
        val accentedCity = rows[1].copy(city = "São Tomé")
        answer = { Result.success(listOf(rows[0], accentedCity)) }
        openUnits()
        search("SINTETICA A")
        compose.onNodeWithText(rows[0].name).assertExists()
        compose.onNodeWithText(rows[1].name).assertDoesNotExist()
        search("  SAO TOME  ")
        compose.onNodeWithText(rows[1].name).assertExists()
        compose.onNodeWithText(rows[0].name).assertDoesNotExist()
        search("Endereço fictício") // Endereço não faz parte dos critérios aprovados.
        compose.onNodeWithText("Nenhuma unidade encontrada para esta busca.").assertExists()
        compose.onNodeWithText("Nenhuma unidade disponível no momento.").assertDoesNotExist()
        compose.onNodeWithText("Buscar por nome ou cidade").assertExists()
        search("   ")
        compose.onNodeWithText(rows[0].name).assertExists()
        compose.onNodeWithText(rows[1].name).assertExists()
        compose.onNodeWithText("Buscar por nome ou cidade").performTextClearance()
        compose.waitForIdle()
        compose.onNodeWithText(rows[0].name).assertExists()
        compose.onNodeWithText(rows[1].name).assertExists()
        compose.runOnIdle {
            assertEquals("", model.uiState.value.searchQuery)
            assertEquals(1, calls)
        }
    }

    @Test fun returningFromDetailPreservesSearchAndFilteredListWithoutReloadingList() {
        openUnits()
        search("gama")
        compose.onNodeWithText(rows[0].name).assertDoesNotExist()
        compose.onNodeWithText(rows[1].name).performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals("c", nav.currentBackStackEntry?.toRoute<AppRoute.CenterDetailById>()?.centerId)
        }
        compose.onNodeWithText("‹").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Buscar por nome ou cidade").assertTextContains("gama")
        compose.onNodeWithText(rows[0].name).assertDoesNotExist()
        compose.onNodeWithText(rows[1].name).assertExists()
        compose.runOnIdle {
            assertEquals("gama", model.uiState.value.searchQuery)
            assertEquals(2, calls) // Uma leitura da lista e outra do detalhe.
        }
    }

    @Test fun queryTypedWhileInitialReadIsPendingIsAppliedWhenItCompletes() {
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        answer = { pending.await() }
        openUnits()
        search("ALFA")
        compose.onNodeWithText("Carregando unidades…").assertExists()
        compose.onNodeWithText("Nenhuma unidade encontrada para esta busca.").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, calls)
            assertTrue(model.uiState.value.isLoading)
            pending.complete(Result.success(rows))
        }
        compose.waitForIdle()
        compose.onNodeWithText(rows[0].name).assertExists()
        compose.onNodeWithText(rows[1].name).assertDoesNotExist()
        compose.onNodeWithText("Buscar por nome ou cidade").assertTextContains("ALFA")
    }

    @Test fun retryKeepsQueryAndUsesLatestTextAndLatestReturnedCatalog() {
        openUnits()
        search("alfa")
        compose.runOnIdle {
            answer = { Result.failure(IllegalStateException("falha-ficticia")) }
            model.loadCenters()
        }
        compose.waitForIdle()
        search("gama")
        compose.onNodeWithText("Não foi possível carregar as unidades. Tente novamente.").assertExists()
        compose.onNodeWithText("Nenhuma unidade encontrada para esta busca.").assertDoesNotExist()
        compose.onNodeWithText(rows[1].name).assertDoesNotExist()
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        compose.runOnIdle { answer = { pending.await() } }
        compose.onNodeWithText("Tentar novamente").performScrollTo().performClick()
        compose.waitForIdle()
        search("alfa") // Texto muda durante a nova consulta.
        compose.onNodeWithText("Carregando unidades…").assertExists()
        val fresh = rows[0].copy(name = "Unidade Sintética A atualizada")
        compose.runOnIdle {
            assertEquals(3, calls)
            pending.complete(Result.success(listOf(fresh, rows[1])))
        }
        compose.waitForIdle()
        compose.onNodeWithText(fresh.name).assertExists()
        compose.onNodeWithText(rows[1].name).assertDoesNotExist()
        compose.onNodeWithText("Buscar por nome ou cidade").assertTextContains("alfa")
        search("")
        compose.onNodeWithText(fresh.name).assertExists()
        compose.onNodeWithText(rows[1].name).assertExists()
        compose.runOnIdle { assertEquals(3, calls) }
    }

    @Test @SdkSuppress(minSdkVersion = 26)
    fun detailMapUsesSelectedPositionAndKeepsRouteSearchAndScheduling() {
        val selected = rows[1].copy(coordinates = GeoCoordinates(-22.91, -47.06))
        answer = { Result.success(listOf(rows[0], selected)) }
        openUnits()
        search("gama")
        compose.onNodeWithText(selected.name).performScrollTo().performClick()
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var captured: Intent? = null
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.data?.scheme != "geo") return null
                captured = Intent(intent)
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            compose.onNodeWithText("Abrir no mapa").performScrollTo().performClick()
            compose.runOnIdle {
                assertEquals(Intent.ACTION_VIEW, captured?.action)
                assertTrue(captured?.dataString?.startsWith("geo:-22.91,-47.06?q=") == true)
                assertTrue(android.net.Uri.decode(captured?.dataString)?.contains(selected.name) == true)
                assertEquals("c", nav.currentBackStackEntry?.toRoute<AppRoute.CenterDetailById>()?.centerId)
                assertEquals(2, calls)
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
        compose.onNodeWithText("‹").performClick()
        compose.onNodeWithText("Buscar por nome ou cidade").assertTextContains("gama")
        compose.onNodeWithText(rows[0].name).assertDoesNotExist()
        compose.onNodeWithText(selected.name).performScrollTo().performClick()
        compose.onNodeWithText("Agendar doação").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(nav.currentDestination?.hasRoute<AppRoute.ScheduleDate>() == true)
            val scheduling = ViewModelProvider(nav.getBackStackEntry<AppGraph.Scheduling>())[SchedulingViewModel::class.java]
            assertEquals(selected.id, scheduling.uiState.value.selectedCenter?.id)
        }
    }

    private fun search(query: String) {
        compose.onNodeWithText("Buscar por nome ou cidade").performTextReplacement(query)
        compose.waitForIdle()
    }

    private fun center(id: String, name: String, city: String, address: String) = DonationCenter(
        id = id, name = name, city = city, address = address,
        openingHours = "", phone = "", timeZoneId = "America/Sao_Paulo",
    )
}
