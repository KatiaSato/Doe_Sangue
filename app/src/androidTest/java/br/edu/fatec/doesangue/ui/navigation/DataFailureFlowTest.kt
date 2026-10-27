package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.presentation.common.toDataMessage
import br.edu.fatec.doesangue.presentation.profile.toProfileMessage
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.*

/** Grafo/VM/telas reais, fontes fictícias; nenhuma conexão ou reserva real. */
@RunWith(AndroidJUnit4::class)
class DataFailureFlowTest {
    @get:Rule val compose=createComposeRule()
    private lateinit var nav:NavHostController
    private lateinit var model:SchedulingViewModel
    private val keys=mutableListOf<String>()
    private val cancellationIds=mutableListOf<String>()
    private var creationError:Throwable?=DataFailure(DataError.SLOT_FULL)
    private var cancellationError:Throwable?=DataFailure(DataError.CANCELLATION_CLOSED)
    private var centerError:Throwable?=null
    private var calendarError:Throwable?=null
    private val center=DonationCenter("test-center-1","Unidade Sintética F19","Cidade fictícia","Endereço fictício","","","America/Sao_Paulo")
    private val date=LocalDate.now(ZoneId.of(center.timeZoneId)).plusDays(3)
    private val summary=AppointmentSummary(Appointment("f19-synthetic","test-user-1","test-slot",AppointmentStatus.SCHEDULED,Instant.parse("2026-10-26T12:00:00Z")),
        center.name,date.atTime(8,0).atZone(ZoneId.of(center.timeZoneId)).toInstant(),center.timeZoneId)

    private fun open(profiles:DonorProfileRepository=FakeDonorProfileRepository()) {
        val auth=object:AuthRepository by FakeAuthRepository() {
            override suspend fun restoreSession()=Result.success(AuthenticatedUser("test-user-1"))
        }
        val centers=object:DonationCenterRepository {
            override suspend fun getCenters():Result<List<DonationCenter>> = centerError?.let { Result.failure(it) }?:Result.success(listOf(center))
        }
        val slots=object:DonationTimeSlotRepository by FakeDonationTimeSlotRepository() {
            override suspend fun getAvailableDates(centerId:String,month:YearMonth):Result<Set<LocalDate>> =
                calendarError?.let { Result.failure(it) }?:Result.success(setOf(date))
        }
        val appointments=object:AppointmentRepository {
            override suspend fun getMyAppointments()=Result.success(listOf(summary))
            override suspend fun createAppointment(timeSlotId:String,idempotencyKey:String):Result<Appointment> {
                keys+=idempotencyKey
                return creationError?.let { Result.failure(it) }?:Result.success(summary.appointment.copy(timeSlotId=timeSlotId))
            }
            override suspend fun cancelAppointment(appointmentId:String):Result<Appointment> {
                cancellationIds+=appointmentId
                return cancellationError?.let { Result.failure(it) }?:Result.success(summary.appointment.copy(status=AppointmentStatus.CANCELLED))
            }
        }
        val container=AppContainer(routineRepositoryOverride = br.edu.fatec.doesangue.ui.navigation.FakeReminderRoutineRepository(), centerRepositoryOverride=centers,timeSlotRepositoryOverride=slots,
            appointmentRepositoryOverride=appointments,authRepositoryOverride=auth,donorProfileRepositoryOverride=profiles,
            campaignRepositoryOverride=FakeCampaignRepository(),bloodStockRepositoryOverride=FakeBloodStockRepository(),
            donationHistoryRepositoryOverride=FakeDonationHistoryRepository())
        compose.setContent { nav=rememberNavController(); DoeSangueTheme { FeatureTestHost(container,nav) } }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Home>()==true }
    }
    private fun openDate() {
        tap("Agendar")
        compose.runOnIdle { model=ViewModelProvider(nav.getBackStackEntry<AppGraph.Scheduling>())[SchedulingViewModel::class.java] }
        compose.runOnIdle { model.selectCenter(center); nav.navigate(AppRoute.ScheduleDate) }
        compose.waitForIdle()
    }

    @Test fun creationShowsCauseKeepsDraftAndKeyThroughNetworkUnknownAndSuccess() {
        open(); openDate()
        compose.runOnIdle { model.selectDate(date) }
        compose.waitForIdle()
        compose.runOnIdle { model.selectTimeSlot(model.uiState.value.timeSlots.first().id); nav.navigate(AppRoute.ScheduleConfirm) }
        fun confirm() { compose.onNode(hasText("Confirmar agendamento") and hasClickAction()).performScrollTo().performClick(); compose.waitForIdle() }
        confirm()
        compose.onNodeWithText(DataFailure(DataError.SLOT_FULL).toDataMessage("fallback")).assertIsDisplayed()
        screenshot("sem-vagas")
        compose.runOnIdle {
            assertEquals(date,model.uiState.value.selectedDate)
            assertNotNull(model.uiState.value.selectedTimeSlotId)
            creationError=DataFailure(DataError.NETWORK)
        }
        confirm()
        compose.onNodeWithText(DataFailure(DataError.NETWORK).toDataMessage("fallback")).assertIsDisplayed()
        compose.runOnIdle { creationError=IllegalStateException("Synthetic private SQL/token") }
        confirm()
        compose.onNodeWithText("Não foi possível confirmar o agendamento. Tente novamente.").assertIsDisplayed()
        compose.onNodeWithText("Synthetic private SQL/token").assertDoesNotExist()
        compose.runOnIdle { creationError=null }
        confirm()
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.ScheduleSuccess>()==true }
        assertEquals(4,keys.size); assertEquals(1,keys.toSet().size)
    }

    @Test fun cancellationKeepsOwnRecordAndAllowsRetryWithoutClaimingSuccess() {
        open()
        compose.runOnIdle { nav.navigate(AppRoute.AppointmentDetailById(summary.appointment.id)) }
        compose.waitForIdle()
        fun cancel() { tap("Cancelar agendamento"); tap("Sim, cancelar") }
        cancel()
        compose.onNodeWithText(DataFailure(DataError.CANCELLATION_CLOSED).toDataMessage("fallback")).assertIsDisplayed()
        compose.onNodeWithText("Agendado").assertIsDisplayed()
        compose.onNodeWithText("Cancelado").assertDoesNotExist()
        screenshot("prazo-cancelamento")
        compose.runOnIdle { cancellationError=DataFailure(DataError.NETWORK) }
        cancel()
        compose.onNodeWithText(DataFailure(DataError.NETWORK).toDataMessage("fallback")).assertIsDisplayed()
        compose.runOnIdle { cancellationError=null }
        cancel()
        compose.onNodeWithText("Cancelado").assertIsDisplayed()
        assertEquals(listOf(summary.appointment.id,summary.appointment.id,summary.appointment.id),cancellationIds)
    }

    @Test fun calendarNetworkErrorHasWorkingRetryAndDoesNotEnableUnloadedDates() {
        calendarError=DataFailure(DataError.NETWORK)
        open(); openDate(); tap("Selecionar data")
        compose.onNodeWithText(DataFailure(DataError.NETWORK).toDataMessage("fallback")).assertIsDisplayed()
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        compose.runOnIdle { calendarError=null }
        tap("Tentar novamente")
        compose.onNodeWithText("Selecione um dia com vagas disponíveis.").assertIsDisplayed()
        compose.onNodeWithText("Confirmar data").assertIsNotEnabled()
        assertTrue(keys.isEmpty())
    }

    @Test fun unitServiceFailureHasRetryAndRestoresTheCatalog() {
        centerError=DataFailure(DataError.SERVICE_UNAVAILABLE)
        open(); tap("Unidades")
        compose.onNodeWithText(DataFailure(DataError.SERVICE_UNAVAILABLE).toDataMessage("fallback")).assertIsDisplayed()
        compose.runOnIdle { centerError=null }
        tap("Tentar novamente")
        compose.onNodeWithText(center.name).assertIsDisplayed()
    }

    @Test fun profileBloodTypeFailureKeepsEditingAndOriginalProfileThenRetries() {
        var reject=true
        val original=DonorProfile("test-user-1","Doador Sintético",bloodType=BloodType.UNKNOWN)
        val profiles=object:DonorProfileRepository by FakeDonorProfileRepository(Result.success(original)) {
            override suspend fun updateCurrentBloodType(bloodType:BloodType):Result<DonorProfile> =
                if(reject)Result.failure(ProfileFailure(ProfileError.ACCESS_DENIED)) else Result.success(original.copy(bloodType=bloodType))
        }
        open(profiles); tap("Perfil"); tap("Alterar tipo sanguíneo"); tap("A+"); tap("Salvar tipo sanguíneo")
        compose.onNodeWithText(ProfileFailure(ProfileError.ACCESS_DENIED).toProfileMessage("fallback")).assertIsDisplayed()
        compose.onNodeWithText("A+").assertIsSelected()
        compose.runOnIdle { reject=false }
        tap("Salvar tipo sanguíneo")
        compose.onNodeWithText("Tipo sanguíneo informado: A+").assertIsDisplayed()
    }

    @Test fun profileSessionErrorStaysOnProfileAndRetriesWithoutAutomaticLogout() {
        var reject=true
        val profiles=object:DonorProfileRepository by FakeDonorProfileRepository() {
            override suspend fun getCurrentProfile():Result<DonorProfile?> =
                if(reject)Result.failure(ProfileFailure(ProfileError.SESSION_UNAVAILABLE)) else Result.success(DonorProfile("test-user-1","Doador Sintético"))
        }
        open(profiles); tap("Perfil")
        compose.onNodeWithText(ProfileFailure(ProfileError.SESSION_UNAVAILABLE).toProfileMessage("fallback")).assertIsDisplayed()
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.Profile>()==true); reject=false }
        tap("Tentar novamente")
        compose.onNodeWithText("Doador Sintético").assertIsDisplayed()
    }

    private fun tap(text:String) {
        val node=compose.onNodeWithText(text)
        if(runCatching { node.assertIsDisplayed() }.isFailure) node.performScrollTo()
        node.performClick(); compose.waitForIdle()
    }
    private fun screenshot(name:String) {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val path=File(instrumentation.targetContext.getExternalFilesDir(null),"f19-$name.png")
        path.outputStream().use { instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
}
