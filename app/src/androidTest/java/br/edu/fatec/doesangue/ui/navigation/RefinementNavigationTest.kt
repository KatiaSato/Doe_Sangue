package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.espresso.Espresso.pressBack
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Navegação de produção com repositories fictícios, sem ler nem renovar sessão real. */
class RefinementNavigationTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var nav:NavHostController
    private val user=AuthenticatedUser("test-user-1")
    private val center=DonationCenter("test-center-1","Unidade Sintética Alfa","Paulínia","Endereço fictício","","","America/Sao_Paulo")
    private val routines=FakeReminderRoutineRepository()
    private val stocks=listOf(
        BloodStock(center.id,center.name,BloodType.O_NEGATIVE,StockLevel.CRITICAL,"Fonte fictícia",Instant.EPOCH,center.timeZoneId),
        BloodStock(center.id,center.name,BloodType.A_POSITIVE,StockLevel.ALERT,"Fonte fictícia",Instant.EPOCH,center.timeZoneId))
    private fun open() {
        val presence=MutableStateFlow<SessionPresence>(SessionPresence.Authenticated(user))
        val auth=object:AuthRepository by FakeAuthRepository(){
            override suspend fun restoreSession():Result<AuthenticatedUser?> = Result.success(user)
            override fun observeSession()=presence
        }
        val container=AppContainer(authRepositoryOverride=auth,donorProfileRepositoryOverride=FakeDonorProfileRepository(),
            appointmentRepositoryOverride=FakeAppointmentRepository(),campaignRepositoryOverride=FakeCampaignRepository(),
            routineRepositoryOverride=routines,centerRepositoryOverride=object:DonationCenterRepository{
                override suspend fun getCenters()=Result.success(listOf(center))
            },timeSlotRepositoryOverride=FakeDonationTimeSlotRepository(),
            bloodStockRepositoryOverride=object:BloodStockRepository{override suspend fun getBloodStocks()=Result.success(stocks)},
            donationHistoryRepositoryOverride=FakeDonationHistoryRepository())
        compose.setContent {nav=rememberNavController();DoeSangueTheme {FeatureTestHost(container,nav,homeFactory=container.homeViewModelFactory)}}
        compose.mainClock.advanceTimeBy(2000)
        compose.waitUntil(10_000){nav.currentDestination?.hasRoute<AppRoute.Home>()==true}
        compose.waitForIdle()
    }
    private fun tap(label:String) {
        val n=compose.onNode(hasText(label) and hasClickAction())
        if(!n.isDisplayed())n.performScrollTo()
        n.performClick();compose.waitForIdle()
    }
    @Test fun homeLevelOpensSelectedStockAndUnitBackPreservesTheSearch() {
        open();tap("Crítico")
        assertTrue(nav.currentDestination?.hasRoute<AppRoute.BloodStockFiltered>()==true)
        assertEquals("critico",nav.currentBackStackEntry!!.toRoute<AppRoute.BloodStockFiltered>().level)
        compose.onNodeWithText("O- • Crítico").assertExists()
        compose.onNodeWithText("A+ • Alerta").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextInput("paulinia")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        compose.onNode(hasText(center.name) and hasClickAction()).performClick()
        compose.waitForIdle()
        assertTrue(nav.currentDestination?.hasRoute<AppRoute.CenterDetailById>()==true)
        pressBack();compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertTextEquals("paulinia")
        compose.onNodeWithText("O- • Crítico").assertExists()
        pressBack();compose.waitForIdle()
        assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>()==true)
    }
    @Test fun thirdTabKeepsDonationSectionSelectedAndReturnsFromEditorToTheRoutine() {
        routines.routines[user.id]=ReminderRoutine("1",user.id,LocalDate.of(2030,12,1),30,center.timeZoneId,true,1)
        open()
        compose.onAllNodesWithText("Doações").onLast().performClick();compose.waitForIdle()
        tap("Agendamentos");tap("Lembretes")
        assertTrue(nav.currentDestination?.hasRoute<AppRoute.DonationsReminders>()==true)
        compose.onNode(hasText("Doações") and isSelected()).assertExists()
        tap("Editar rotina")
        assertTrue(nav.currentDestination?.hasRoute<AppRoute.EditRoutine>()==true)
        pressBack();compose.waitForIdle()
        assertTrue(nav.currentDestination?.hasRoute<AppRoute.DonationsReminders>()==true)
        compose.onNodeWithText("Minha rotina").assertExists()
        tap("Histórico")
        assertTrue(nav.currentDestination?.hasRoute<AppRoute.DonationsHistory>()==true)
        pressBack();compose.waitForIdle()
        assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>()==true)
        assertNotNull(routines.routines[user.id])
    }
}
