package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.data.local.FileSchedulingDraftStore
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.presentation.auth.SessionMonitorViewModel
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

class RescheduleRoutineFlowTest {
    @get:Rule val compose=createComposeRule()
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val directory=File(instrumentation.targetContext.cacheDir,"f13-f14-ui-${UUID.randomUUID()}")
    private val store=FileSchedulingDraftStore(directory)
    private val user=AuthenticatedUser("test-user-1")
    private val presence=MutableStateFlow<SessionPresence>(SessionPresence.Authenticated(user))
    private val zone=ZoneId.of("America/Sao_Paulo")
    private val date=LocalDate.now(zone).plusDays(5)
    private val center=DonationCenter("test-center-1","Unidade Sintética F13","Cidade","Endereço fictício","","","America/Sao_Paulo")
    private val original=AppointmentSummary(Appointment("23",user.id,"4",AppointmentStatus.SCHEDULED,Instant.now()),
        center.name,date.minusDays(1).atTime(8,0).atZone(zone).toInstant(),center.timeZoneId,center.id)
    private var summaries=listOf(original)
    private val routines=FakeReminderRoutineRepository()
    private val calls=mutableListOf<Pair<String,RoutineChoice?>>()
    private var reject:DataError?=null
    private var routineAlreadyProcessed=false
    private lateinit var container:AppContainer
    private lateinit var nav:NavHostController
    @After fun close() {directory.deleteRecursively()}
    private fun open() {
        val auth=object:AuthRepository by FakeAuthRepository() {
            override fun observeSession()=presence
            override suspend fun restoreSession()=Result.success(user)
        }
        val centers=object:DonationCenterRepository {override suspend fun getCenters()=Result.success(listOf(center))}
        val appointments=object:AppointmentRepository by FakeAppointmentRepository() {
            override suspend fun getMyAppointments()=Result.success(summaries)
            override suspend fun rescheduleForOwner(ownerId:String,appointmentId:String,timeSlotId:String,idempotencyKey:String):Result<Appointment> {
                assertEquals(user.id,ownerId);assertEquals("23",appointmentId);calls+="reschedule" to null
                reject?.let {return Result.failure(DataFailure(it))}
                val row=Appointment("91",ownerId,timeSlotId,AppointmentStatus.SCHEDULED,Instant.now())
                summaries=listOf(original.copy(appointment=original.appointment.copy(status=AppointmentStatus.CANCELLED),rescheduledToId="91"),
                    AppointmentSummary(row,center.name,date.atTime(9,30).atZone(zone).toInstant(),center.timeZoneId,center.id,rescheduledFromId="23"))
                return Result.success(row)
            }
            override suspend fun createWithRoutineForOwner(ownerId:String,timeSlotId:String,idempotencyKey:String,routine:RoutineChoice):Result<Appointment> {
                calls+="routine" to routine;if(!routineAlreadyProcessed) routines.save(ownerId,routine).getOrThrow()
                return Result.success(Appointment("92",ownerId,timeSlotId,AppointmentStatus.SCHEDULED,Instant.now()))
            }
            override suspend fun createAppointmentForOwner(ownerId:String,timeSlotId:String,idempotencyKey:String):Result<Appointment> {
                calls+="create" to null;return Result.success(Appointment("93",ownerId,timeSlotId,AppointmentStatus.SCHEDULED,Instant.now()))
            }
        }
        container=AppContainer(routineRepositoryOverride=routines,authRepositoryOverride=auth,centerRepositoryOverride=centers,
            timeSlotRepositoryOverride=FakeDonationTimeSlotRepository(),appointmentRepositoryOverride=appointments,
            donorProfileRepositoryOverride=FakeDonorProfileRepository(),campaignRepositoryOverride=FakeCampaignRepository(),
            bloodStockRepositoryOverride=FakeBloodStockRepository(),donationHistoryRepositoryOverride=FakeDonationHistoryRepository(),schedulingDraftStore=store)
        compose.setContent {
            nav=rememberNavController()
            val monitor:SessionMonitorViewModel=viewModel(factory=container.sessionMonitorViewModelFactory)
            DoeSangueTheme {FeatureTestHost(container,nav,sessionState=monitor.uiState)}
        }
        compose.mainClock.advanceTimeBy(2_000);route<AppRoute.Home>()
    }
    private inline fun <reified T:Any> route()=compose.waitUntil(10_000) {nav.currentDestination?.hasRoute<T>()==true}
    private fun tap(text:String) {
        val node=compose.onNode(hasText(text) and hasClickAction())
        if(runCatching {node.assertIsDisplayed()}.isFailure) node.performScrollTo()
        node.performClick();compose.waitForIdle()
    }
    private fun model()=ViewModelProvider(nav.getBackStackEntry<AppGraph.Scheduling>(),container.schedulingViewModelFactory)[SchedulingViewModel::class.java]
    private fun fillSlot() {
        tap(center.name);tap("Continuar");route<AppRoute.ScheduleDate>()
        compose.runOnIdle {model().selectDate(date)};compose.waitForIdle();tap("09:30");tap("Continuar")
    }
    private fun capture(name:String) {
        instrumentation.uiAutomation.waitForIdle(300,5_000)
        File(instrumentation.targetContext.getExternalFilesDir(null),"f13-f14-$name.png").outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun rescheduleShowsBothChoicesSkipsRoutineAndLinksOldRecordToReplacement() {
        open();compose.runOnIdle {nav.navigate(AppRoute.AppointmentDetailById("23"))};tap("Remarcar agendamento")
        route<AppRoute.ScheduleCenter>();fillSlot();route<AppRoute.ScheduleConfirm>()
        compose.onNodeWithText("Agendamento atual 23").assertExists();capture("revisao-remarcacao")
        assertTrue(calls.isEmpty());tap("Confirmar remarcação");route<AppRoute.ScheduleSuccess>()
        compose.onNodeWithText("Agendamento remarcado!").assertExists();assertEquals(listOf("reschedule"),calls.map {it.first})
        tap("Ir para o início");route<AppRoute.Home>()
        compose.runOnIdle {nav.navigate(AppRoute.AppointmentDetailById("23"))}
        compose.onNodeWithText("Remarcado").assertExists();tap("Ver novo agendamento 91")
        compose.onNodeWithText("Remarcado a partir do agendamento 23.").assertExists()
        compose.runOnIdle {nav.popBackStack()};compose.waitForIdle()
        compose.onNodeWithText("Ver novo agendamento 91").assertExists()
    }
    @Test fun rejectedRescheduleKeepsOriginalAndSpecificMessageUntilExplicitReview() {
        reject=DataError.SLOT_FULL;open();compose.runOnIdle {nav.navigate(AppRoute.ScheduleReschedule("23"))}
        route<AppRoute.ScheduleCenter>();fillSlot();route<AppRoute.ScheduleConfirm>();tap("Confirmar remarcação")
        compose.onNodeWithText("Este horário ficou sem vagas. Volte e escolha outro horário.").assertExists()
        assertEquals(AppointmentStatus.SCHEDULED,summaries.single().appointment.status)
        tap("Rever escolhas");tap("Retomar");route<AppRoute.ScheduleDate>()
        compose.runOnIdle {assertEquals("23",model().uiState.value.rescheduleOrigin!!.appointmentId)}
    }
    @Test fun routineEditorMasksNumericDateAndSupportsSavePauseEditResumeAndDelete() {
        open();compose.runOnIdle {nav.navigate(AppRoute.EditRoutine)};route<AppRoute.EditRoutine>()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Data inicial").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Data inicial").performTextInput(date.format(DateTimeFormatter.ofPattern("ddMMyyyy")))
        compose.onNodeWithText("Intervalo em dias").performTextInput("21")
        tap("Salvar rotina");compose.onNodeWithText("Rotina salva.").assertExists();assertEquals(date,routines.routines.getValue(user.id).startsOn)
        capture("rotina-salva");tap("Pausar rotina");compose.onNodeWithText("Rotina pausada.").assertExists()
        compose.onNodeWithText("Intervalo em dias").performTextReplacement("22");tap("Salvar rotina")
        assertFalse(routines.routines.getValue(user.id).active);tap("Retomar rotina")
        compose.onNodeWithText("Rotina retomada.").assertExists();tap("Excluir rotina")
        tap("Manter rotina");assertNotNull(routines.routines[user.id]);tap("Excluir rotina")
        compose.onAllNodes(hasText("Excluir rotina") and hasClickAction()).onLast().performClick()
        compose.onNodeWithText("Rotina excluída.").assertExists();assertFalse(routines.routines.containsKey(user.id));assertTrue(calls.isEmpty())
    }
    @Test fun optionalRoutineIsSentTogetherWithBookingAndNoOtherBookingIsCreated() {
        open();tap("Agendar");route<AppRoute.ScheduleCenter>();fillSlot();route<AppRoute.ScheduleRecurrence>()
        tap("Configurar rotina")
        compose.onNodeWithText("Data inicial").performTextInput(date.format(DateTimeFormatter.ofPattern("ddMMyyyy")))
        compose.onNodeWithText("Intervalo em dias").performTextInput("17");tap("Continuar");route<AppRoute.ScheduleConfirm>()
        assertTrue(calls.isEmpty());tap("Confirmar agendamento");route<AppRoute.ScheduleSuccess>()
        assertEquals(listOf("routine"),calls.map {it.first});assertEquals(17,calls.single().second!!.intervalDays)
        assertNotNull(routines.routines[user.id]);capture("criacao-com-rotina")
    }
    @Test fun noRoutineChoiceDoesNotDeleteExistingPreferenceAndSummaryOpensEditor() {
        routines.routines[user.id]=ReminderRoutine("1",user.id,date,13,center.timeZoneId,true,8)
        open();tap("Agendar");route<AppRoute.ScheduleCenter>();fillSlot();route<AppRoute.ScheduleRecurrence>()
        tap("Continuar");tap("Confirmar agendamento");route<AppRoute.ScheduleSuccess>()
        assertEquals(listOf("create"),calls.map {it.first});assertEquals(8L,routines.routines.getValue(user.id).version)
        tap("Ir para o início");compose.runOnIdle {nav.navigate(AppRoute.DonationsAppointments)}
        tap("Editar rotina");route<AppRoute.EditRoutine>()
    }
    @Test fun resumingMissingUnitPreservesSavedSourceInsteadOfStartingIncomingReschedule()=runBlocking<Unit> {
        store.save(SchedulingDraft(user.id,"missing-center","Unidade removida",center.timeZoneId,
            rescheduleOrigin=RescheduleOrigin.from(original))).getOrThrow()
        open();compose.runOnIdle {nav.navigate(AppRoute.ScheduleReschedule("999"))};tap("Retomar")
        route<AppRoute.ScheduleCenter>()
        compose.runOnIdle {assertEquals("23",model().uiState.value.rescheduleOrigin!!.appointmentId)}
        assertTrue(calls.isEmpty());compose.onNodeWithText("A unidade do rascunho não está mais disponível. Escolha outra unidade.").assertExists()
    }
    @Test fun replayOfBookingWithDeletedRoutineShowsRequestedPreferenceWithoutClaimingCurrentRoutine()=runBlocking<Unit> {
        routineAlreadyProcessed=true
        val choice=RoutineChoice(date,17,center.timeZoneId,0)
        store.save(SchedulingDraft(user.id,center.id,center.name,center.timeZoneId,date.toString(),
            "test-slot-${date.atTime(9,30).atZone(zone).toInstant()}",date.atTime(9,30).atZone(zone).toInstant().toString(),
            SchedulingStep.CONFIRM,"00000000-0000-4000-8000-000000000099",routine=choice)).getOrThrow()
        open();tap("Agendar");tap("Retomar");route<AppRoute.ScheduleConfirm>();tap("Confirmar agendamento");route<AppRoute.ScheduleSuccess>()
        assertTrue(routines.routines.isEmpty())
        compose.onNodeWithText("Preferência deste pedido: a cada 17 dias. Consulte sua rotina atual em Minhas doações.").assertExists()
        assertEquals(1,calls.size)
    }
    @Test fun preparedRescheduleCanBeRecoveredWithoutSourceStillBeingActive()=runBlocking<Unit> {
        val origin=RescheduleOrigin.from(original)
        store.save(SchedulingDraft(user.id,center.id,center.name,center.timeZoneId,date.toString(),
            "test-slot-${date.atTime(9,30).atZone(zone).toInstant()}",date.atTime(9,30).atZone(zone).toInstant().toString(),
            SchedulingStep.CONFIRM,"00000000-0000-4000-8000-000000000099",origin)).getOrThrow()
        summaries=listOf(original.copy(appointment=original.appointment.copy(status=AppointmentStatus.CANCELLED)))
        open();tap("Agendar");tap("Retomar");route<AppRoute.ScheduleConfirm>()
        assertTrue(calls.isEmpty());tap("Confirmar remarcação");route<AppRoute.ScheduleSuccess>()
        compose.runOnIdle {assertFalse(nav.previousBackStackEntry?.destination?.hasRoute<AppRoute.ScheduleRecurrence>()==true)}
    }
}
