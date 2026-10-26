package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.BuildConfig
import br.edu.fatec.doesangue.data.repository.SupabaseDonationCenterRepository
import br.edu.fatec.doesangue.data.repository.SupabaseDonationTimeSlotRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseDonationCenterDataSource
import br.edu.fatec.doesangue.data.supabase.SupabaseDonationTimeSlotDataSource
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/** Grafo real com consulta pública real; Auth/perfil/reservas fictícios e nenhuma escrita remota. */
@RunWith(AndroidJUnit4::class)
class SupabaseAvailabilityFlowTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var nav: NavHostController

    @Test fun publishedUnitCalendarAndSlotsRenderPublicSeedWithoutCreatingAppointment() {
        val arguments=InstrumentationRegistry.getArguments()
        assumeTrue(arguments.getString("enableSupabaseReadTests")=="true" &&
            arguments.getString("expectAvailabilitySeed")=="true")
        assumeTrue("Fixture datada deve continuar futura.",
            LocalDate.now(ZoneId.of("America/Sao_Paulo")) < LocalDate.of(2026,10,28))
        // Não instala Auth e não acessa a sessão persistida no aparelho.
        val client=createSupabaseClient(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }
        try {
            val auth=object: AuthRepository by FakeAuthRepository() {
                override suspend fun restoreSession()=Result.success(AuthenticatedUser("usuario-sintetico"))
            }
            val container=AppContainer(
                centerRepositoryOverride=SupabaseDonationCenterRepository(SupabaseDonationCenterDataSource(client)),
                timeSlotRepositoryOverride=SupabaseDonationTimeSlotRepository(SupabaseDonationTimeSlotDataSource(client)),
                authRepositoryOverride=auth, donorProfileRepositoryOverride=FakeDonorProfileRepository(),
                appointmentRepositoryOverride=FakeAppointmentRepository(), campaignRepositoryOverride=FakeCampaignRepository(),
                bloodStockRepositoryOverride=FakeBloodStockRepository(), donationHistoryRepositoryOverride=FakeDonationHistoryRepository())
            compose.setContent { nav=rememberNavController(); DoeSangueTheme { FeatureTestHost(container,nav) } }
            compose.mainClock.advanceTimeBy(2_000)
            compose.waitUntil(15_000) { nav.currentDestination?.hasRoute<AppRoute.Home>()==true }
            compose.onNodeWithText("Unidades").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Unidade Demo A").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Unidade Demo A").performScrollTo().performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Agendar doação").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Agendar doação").performScrollTo().performClick()
            compose.waitUntil(15_000) { nav.currentDestination?.hasRoute<AppRoute.ScheduleDate>()==true }
            compose.onNodeWithText("Selecionar data").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("Selecione um dia com vagas disponíveis.").fetchSemanticsNodes().isNotEmpty() }
            day("27").assertIsNotEnabled()
            day("28").assertIsEnabled()
            day("29").assertIsEnabled()
            day("30").assertIsNotEnabled()
            screenshot("calendario-supabase")
            day("28").performClick()
            compose.onNodeWithText("Confirmar data").performClick()
            compose.waitUntil(15_000) { compose.onAllNodesWithText("2 vagas").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("28/10/2026").assertExists()
            compose.onNodeWithText("08:00").assertExists()
            compose.onNodeWithText("10:00").assertExists()
            compose.onNodeWithText("2 vagas").assertExists()
            compose.onNodeWithText("3 vagas").assertExists()
            compose.onNodeWithText("08:00").performScrollTo().performClick()
            compose.onNodeWithText("Continuar").assertIsEnabled()
            screenshot("horarios-supabase")
            // Encerra antes de recorrência/confirmação; nenhuma reserva foi solicitada.
        } finally { runBlocking { client.close() } }
    }

    private fun day(number:String): SemanticsNodeInteraction {
        val pattern=Regex("(?<!\\d)$number(?!\\d)")
        val matcher=SemanticsMatcher("dia $number") { node ->
            val texts=node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }.orEmpty()+
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            texts.any(pattern::containsMatchIn)
        }
        val candidates=compose.onAllNodes(matcher and hasClickAction() and hasAnyAncestor(isDialog()))
        val displayed=candidates.fetchSemanticsNodes().indices.filter {
            runCatching { candidates[it].assertIsDisplayed() }.isSuccess
        }
        check(displayed.size==1)
        return candidates[displayed.single()]
    }

    private fun screenshot(name:String) {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val file=File(instrumentation.targetContext.getExternalFilesDir(null),"t03-$name.png")
        file.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
