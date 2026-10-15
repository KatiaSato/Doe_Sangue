package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.Campaign
import br.edu.fatec.doesangue.domain.repository.CampaignRepository
import br.edu.fatec.doesangue.presentation.campaigns.CampaignsViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Consulta e navegação reais com respostas públicas fictícias; nenhuma rede. */
@RunWith(AndroidJUnit4::class)
class CampaignFilterFlowTest {
    @get:Rule val compose = createComposeRule()
    private val repository = ControlledCampaignRepository()
    private lateinit var nav: NavHostController
    private lateinit var model: CampaignsViewModel

    private fun openCampaigns() {
        val container = AppContainer(
            authRepositoryOverride = FakeAuthRepository(),
            donorProfileRepositoryOverride = FakeDonorProfileRepository(),
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            appointmentRepositoryOverride = FakeAppointmentRepository(),
            campaignRepositoryOverride = repository,
        )
        val factory = viewModelFactory { initializer { CampaignsViewModel(repository).also { model = it } } }
        compose.setContent {
            nav = rememberNavController()
            DoeSangueTheme { FeatureTestHost(container, nav, campaignsFactory = factory) }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Welcome>() == true }
        compose.onNodeWithText("Já tenho uma conta").performClick()
        compose.onNodeWithText("E-mail").performTextInput("teste@example.com")
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle { nav.navigate(AppRoute.Campaigns) }
        compose.waitForIdle()
    }

    private fun filter(label: String) {
        compose.onNodeWithText("Tipo sanguíneo:", substring = true).performScrollTo().performClick()
        val option = compose.onNodeWithText(label, substring = false)
        runCatching { option.assertIsDisplayed() }.onFailure { option.performScrollTo() }
        option.performClick()
        compose.waitForIdle()
    }

    @Test fun exactFilterKeepsGeneralCampaignsAndCanReturnToAll() {
        openCampaigns()
        compose.onNodeWithText("Campanha A").assertExists()
        filter("O-")
        compose.onNodeWithText("Campanha geral sintética").assertExists()
        compose.onNodeWithText("Campanha O").assertExists()
        compose.onNodeWithText("Campanha A").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(null, BloodType.O_NEGATIVE), repository.queries) }
        filter("Todos")
        compose.onNodeWithText("Campanha A").assertExists()
        compose.runOnIdle { assertNull(repository.queries.last()) }
    }

    @Test fun failureRetryAndEmptyKeepSelectedFilter() {
        openCampaigns()
        compose.runOnIdle { repository.answer = { Result.failure(IllegalStateException("erro-interno")) } }
        filter("B+")
        compose.onNodeWithText("Não foi possível carregar as campanhas. Tente novamente.").assertExists()
        compose.onNodeWithText("erro-interno").assertDoesNotExist()
        compose.runOnIdle { repository.answer = { Result.success(emptyList()) } }
        compose.onNodeWithText("Tentar novamente").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Nenhuma campanha encontrada para este filtro.").assertExists()
        compose.onNodeWithText("Tipo sanguíneo: B+").assertExists()
        compose.runOnIdle { assertEquals(listOf(null, BloodType.B_POSITIVE, BloodType.B_POSITIVE), repository.queries) }
    }

    @Test fun changingFilterDiscardsLateOldResponseAndBlocksDuplicateRead() {
        val old = CompletableDeferred<Result<List<Campaign>>>()
        repository.answer = { type -> if (type == null) withContext(NonCancellable) { old.await() }
            else Result.success(repository.rows.filter { type in it.bloodTypes }) }
        openCampaigns()
        compose.onNodeWithText("Carregando campanhas…").assertExists()
        compose.runOnIdle { model.loadCampaigns(); assertEquals(1, repository.queries.size) }
        filter("O-")
        compose.onNodeWithText("Campanha O").assertExists()
        compose.runOnIdle { old.complete(Result.success(repository.rows)) }
        compose.waitForIdle()
        compose.onNodeWithText("Campanha A").assertDoesNotExist()
        compose.onNodeWithText("Tipo sanguíneo: O-").assertExists()
    }

    @Test fun detailUsesSelectedIdAndUnitZoneAndUnavailableIdHasNoData() {
        openCampaigns(); filter("O-")
        compose.onAllNodesWithText("Ver campanha")[1].performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Descrição O").assertExists()
        compose.onNodeWithText("Fonte: Fonte sintética").assertExists()
        compose.onNodeWithText("15/10/2026 08:00 até 16/10/2026 08:00").assertExists()
        compose.onNodeWithText("‹").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Tipo sanguíneo: O-").assertExists()
        compose.runOnIdle { nav.navigate(AppRoute.CampaignDetailById("inexistente")) }
        compose.waitForIdle()
        compose.onNodeWithText("Campanha não encontrada ou indisponível.").assertExists()
        compose.onNodeWithText("Descrição O").assertDoesNotExist()
    }

    private class ControlledCampaignRepository : CampaignRepository {
        val rows = listOf(row("geral", "Campanha geral sintética", emptyList()),
            row("o", "Campanha O", listOf(BloodType.O_NEGATIVE)),
            row("a", "Campanha A", listOf(BloodType.A_POSITIVE)))
        val queries = mutableListOf<BloodType?>()
        var answer: suspend (BloodType?) -> Result<List<Campaign>> = { type ->
            Result.success(rows.filter { type == null || it.bloodTypes.isEmpty() || type in it.bloodTypes })
        }
        override suspend fun getCampaigns(bloodType: BloodType?): Result<List<Campaign>> {
            queries += bloodType
            return answer(bloodType)
        }
        private fun row(id: String, title: String, types: List<BloodType>) = Campaign(
            id, "unit-1", title, if (id == "o") "Descrição O" else "Descrição sintética",
            Instant.parse("2026-10-15T11:00:00Z"), Instant.parse("2026-10-16T11:00:00Z"),
            "Fonte sintética", "Unidade sintética", "America/Sao_Paulo", types,
        )
    }
}
