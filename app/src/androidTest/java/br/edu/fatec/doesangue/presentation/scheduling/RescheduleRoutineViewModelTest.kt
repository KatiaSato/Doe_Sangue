package br.edu.fatec.doesangue.presentation.scheduling

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.data.local.FileSchedulingDraftStore
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.domain.usecase.*
import br.edu.fatec.doesangue.ui.navigation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.*
import java.util.UUID

class RescheduleRoutineViewModelTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val directory=File(instrumentation.targetContext.cacheDir,"f13-f14-vm-${UUID.randomUUID()}")
    private val stores=mutableListOf<ViewModelStore>()
    private val user=AuthenticatedUser("test-user-1")
    private val presence=MutableStateFlow<SessionPresence>(SessionPresence.Authenticated(user))
    private val clock=Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"),ZoneOffset.UTC)
    private val center=DonationCenter("test-center-1","Unidade Sintética F13","Cidade","Endereço","","","America/Sao_Paulo")
    private val original=AppointmentSummary(Appointment("23",user.id,"4",AppointmentStatus.SCHEDULED,clock.instant()),
        center.name,Instant.parse("2026-11-19T11:00:00Z"),center.timeZoneId,center.id)
    private var summaries=listOf(original)
    private var readCount=0
    private var available=true
    private var failSave=false
    private val calls=mutableListOf<Triple<String,String,RoutineChoice?>>()
    private var answer: suspend (String)->Result<Appointment> = {slot -> Result.success(Appointment("91",user.id,slot,AppointmentStatus.SCHEDULED,clock.instant()))}
    private var sourceAnswer: (suspend () -> Result<List<AppointmentSummary>>)?=null
    private fun main(block: ()->Unit) {instrumentation.runOnMainSync(block);instrumentation.waitForIdleSync()}
    private fun await(test: ()->Boolean) {
        val deadline=android.os.SystemClock.uptimeMillis()+8_000
        while(!test() && android.os.SystemClock.uptimeMillis()<deadline) {Thread.sleep(15);instrumentation.waitForIdleSync()}
        assertTrue("Estado esperado não alcançado",test())
    }
    private fun storage(): SchedulingDraftStore {
        val file=FileSchedulingDraftStore(directory)
        return object:SchedulingDraftStore by file {
            override suspend fun save(draft: SchedulingDraft)=if(failSave)Result.failure(java.io.IOException()) else file.save(draft)
        }
    }
    private fun model(): SchedulingViewModel {
        val store=ViewModelStore().also {stores+=it}
        val auth=object:AuthRepository by FakeAuthRepository() {override fun observeSession()=presence}
        val centers=object:DonationCenterRepository {override suspend fun getCenters()=Result.success(if(available)listOf(center) else emptyList())}
        val slots=object:DonationTimeSlotRepository by FakeDonationTimeSlotRepository() {
            override suspend fun getTimeSlots(centerId:String,fromInclusive:Instant,untilExclusive:Instant)=
                if(available)FakeDonationTimeSlotRepository().getTimeSlots(centerId,fromInclusive,untilExclusive) else Result.success(emptyList())
        }
        val appointments=object:AppointmentRepository by FakeAppointmentRepository() {
            override suspend fun getMyAppointments(): Result<List<AppointmentSummary>> {readCount++;return sourceAnswer?.invoke() ?: Result.success(summaries)}
            override suspend fun rescheduleForOwner(ownerId:String,appointmentId:String,timeSlotId:String,idempotencyKey:String):Result<Appointment> {
                assertEquals(user.id,ownerId);assertEquals("23",appointmentId)
                calls+=Triple("reschedule",idempotencyKey,null);return answer(timeSlotId)
            }
            override suspend fun createWithRoutineForOwner(ownerId:String,timeSlotId:String,idempotencyKey:String,routine:RoutineChoice):Result<Appointment> {
                assertEquals(user.id,ownerId);calls+=Triple("routine",idempotencyKey,routine);return answer(timeSlotId)
            }
        }
        lateinit var model:SchedulingViewModel
        main {model=ViewModelProvider(store,viewModelFactory {initializer {
            SchedulingViewModel(centers,GetDonationTimeSlotsUseCase(slots,clock),appointments,GetAvailableDonationDatesUseCase(slots,clock),clock,storage(),auth)
        }})[SchedulingViewModel::class.java]}
        await {model.draftRecovery.value.phase!=DraftRecoveryPhase.LOADING}
        return model
    }
    private fun closeModels()=main {stores.forEach {it.clear()};stores.clear()}
    @After fun close() {closeModels();directory.deleteRecursively()}
    private fun fill(model:SchedulingViewModel,reschedule:Boolean) {
        if(reschedule) {main {model.prepareReschedule("23")};await {model.uiState.value.rescheduleOrigin!=null}}
        main {model.selectCenter(center);model.selectDate(LocalDate.of(2026,11,20))}
        await {model.uiState.value.timeSlots.isNotEmpty()}
        main {model.selectTimeSlot(model.uiState.value.timeSlots.first().id);model.onStepVisible(SchedulingStep.CONFIRM)}
    }
    @Test fun preparationRejectsForeignClosedAndPastOriginalsBeforeAnyWrite() {
        val model=model()
        for(summary in listOf(original.copy(appointment=original.appointment.copy(donorId="other")),
            original.copy(appointment=original.appointment.copy(status=AppointmentStatus.CANCELLED)),
            original.copy(startsAt=clock.instant().minusSeconds(1)),original.copy(centerName=""))) {
            summaries=listOf(summary);main {model.prepareReschedule("23")};await {model.uiState.value.rescheduleError!=null}
            assertNull(model.uiState.value.rescheduleOrigin);assertTrue(calls.isEmpty())
        }
    }
    @Test fun lostRescheduleReplyReplaysSameSourceAndKeyAfterNewVmWithoutReadingOriginalOrOffer() {
        answer={Result.failure(DataFailure(DataError.NETWORK))}
        val first=model();fill(first,true);main {first.confirmAppointment()};await {calls.size==1 && !first.uiState.value.appointmentCreation.isLoading}
        val snapshot=runBlocking {storage().read(user.id).getOrThrow()!!}
        assertEquals("23",snapshot.rescheduleOrigin!!.appointmentId);val key=calls.single().second
        closeModels();available=false;summaries=emptyList()
        val reads=readCount;val restored=model();main {restored.resumeDraft()};await {restored.draftRecovery.value.phase==DraftRecoveryPhase.READY}
        assertEquals(reads,readCount);assertEquals(1,calls.size)
        answer={slot -> Result.success(Appointment("91",user.id,slot,AppointmentStatus.CANCELLED,clock.instant()))}
        main {restored.selectRoutine(RoutineChoice(LocalDate.of(2026,11,21),9,"UTC",0));restored.confirmAppointment()}
        await {restored.uiState.value.appointmentCreation.appointment!=null}
        assertEquals(listOf(key,key),calls.map {it.second});assertEquals(AppointmentStatus.CANCELLED,restored.uiState.value.appointmentCreation.appointment!!.status)
    }
    @Test fun unsentRescheduleRevalidatesOriginalAndCanBeDiscardedIfClosed() {
        val first=model();fill(first,true)
        await {runBlocking {storage().read(user.id).getOrNull()?.rescheduleOrigin!=null}}
        closeModels();summaries=listOf(original.copy(appointment=original.appointment.copy(status=AppointmentStatus.CANCELLED)))
        val restored=model();main {restored.resumeDraft()};await {restored.draftRecovery.value.phase==DraftRecoveryPhase.ERROR}
        assertTrue(restored.draftRecovery.value.canDiscard);assertTrue(calls.isEmpty())
        main {restored.discardDraft()};await {restored.draftRecovery.value.phase==DraftRecoveryPhase.READY}
        assertNull(runBlocking {storage().read(user.id).getOrThrow()})
    }
    @Test fun selectedRoutineFingerprintIsDurableAndFrozenAcrossUncertainReplyAndRecreation() {
        val routine=RoutineChoice(LocalDate.of(2026,11,20),13,"America/Sao_Paulo",8)
        val first=model();fill(first,false);main {first.selectRoutine(routine)}
        answer={Result.failure(DataFailure(DataError.NETWORK))};main {first.confirmAppointment()}
        await {calls.size==1 && !first.uiState.value.appointmentCreation.isLoading}
        assertEquals(routine,runBlocking {storage().read(user.id).getOrThrow()!!.routine})
        main {first.selectRoutine(null)};assertEquals(routine,first.uiState.value.routine)
        closeModels();available=false
        val restored=model();main {restored.resumeDraft()};await {restored.draftRecovery.value.phase==DraftRecoveryPhase.READY}
        answer={slot -> Result.success(Appointment("91",user.id,slot,AppointmentStatus.SCHEDULED,clock.instant()))}
        main {restored.confirmAppointment()};await {restored.uiState.value.appointmentCreation.appointment!=null}
        assertEquals(calls[0],calls[1]);assertEquals(routine,calls[1].third)
    }
    @Test fun diskFailurePreventsReschedulingAndDefiniteRejectionPreservesOriginalForReview() {
        val first=model();fill(first,true);failSave=true;main {first.confirmAppointment()}
        await {!first.uiState.value.appointmentCreation.isLoading};assertTrue(calls.isEmpty())
        failSave=false;answer={Result.failure(DataFailure(DataError.SLOT_FULL))};main {first.confirmAppointment()}
        await {first.uiState.value.canReviewRejectedRequest};assertEquals("23",first.uiState.value.rescheduleOrigin!!.appointmentId)
        main {first.reviewRejectedRequest()};await {first.draftRecovery.value.phase==DraftRecoveryPhase.OFFER}
        assertNull(first.draftRecovery.value.draft!!.requestKey);assertEquals("23",first.draftRecovery.value.draft!!.rescheduleOrigin!!.appointmentId)
    }
    @Test fun accountSwitchCancelsSourceReadAndLatePrivateResultCannotPopulateNewAccount() {
        val pending=CompletableDeferred<Result<List<AppointmentSummary>>>()
        sourceAnswer={withContext(NonCancellable) {pending.await()}}
        try {
            val first=model();main {first.prepareReschedule("23")};await {readCount==1}
            main {presence.value=SessionPresence.Authenticated(AuthenticatedUser("other"))};await {first.draftRecovery.value.phase==DraftRecoveryPhase.READY}
            main {pending.complete(Result.success(listOf(original)))}
            assertNull(first.uiState.value.rescheduleOrigin);assertTrue(calls.isEmpty())
        } finally {pending.complete(Result.success(emptyList()))}
    }
}
