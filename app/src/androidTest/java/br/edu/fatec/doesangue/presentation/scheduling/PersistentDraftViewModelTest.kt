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

/** Recria VM e store do arquivo, sem sessão/rede/reserva reais. */
class PersistentDraftViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val directory = File(instrumentation.targetContext.cacheDir, "t02-vm-${UUID.randomUUID()}")
    private val stores = mutableListOf<ViewModelStore>()
    private val a = AuthenticatedUser("test-user-1")
    private val presence = MutableStateFlow<SessionPresence>(SessionPresence.Authenticated(a))
    private val clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC)
    private val date = LocalDate.of(2026,11,20)
    private val center = DonationCenter("test-center-1", "Unidade Sintética T02", "Cidade", "Endereço", "", "", "America/Sao_Paulo")
    private var centers = listOf(center)
    private var available = true
    private val keys = mutableListOf<String>()
    private var answer: suspend (String, String) -> Result<Appointment> = { slot, _ ->
        Result.success(Appointment("synthetic-1", a.id, slot, AppointmentStatus.SCHEDULED, clock.instant())) }
    private var centerReads = 0
    private var slotReads = 0
    private var failWrite = false
    private var failRead = false
    private var failClear = false
    private fun main(block: () -> Unit) { instrumentation.runOnMainSync(block); instrumentation.waitForIdleSync() }
    private fun await(predicate: () -> Boolean) {
        val end = android.os.SystemClock.uptimeMillis() + 8_000
        while (!predicate() && android.os.SystemClock.uptimeMillis() < end) { Thread.sleep(15); instrumentation.waitForIdleSync() }
        assertTrue("Estado esperado não foi alcançado", predicate())
    }
    private fun storage(): SchedulingDraftStore {
        val file = FileSchedulingDraftStore(directory)
        return object : SchedulingDraftStore by file {
            override suspend fun save(draft: SchedulingDraft): Result<Unit> = if (failWrite) Result.failure(java.io.IOException()) else file.save(draft)
            override suspend fun read(ownerId: String): Result<SchedulingDraft?> = if (failRead) Result.failure(java.io.IOException()) else file.read(ownerId)
            override suspend fun clear(ownerId: String): Result<Unit> = if (failClear) Result.failure(java.io.IOException()) else file.clear(ownerId)
        }
    }
    private fun model(): SchedulingViewModel {
        val store = ViewModelStore().also { stores += it }
        val auth = object : AuthRepository by FakeAuthRepository() { override fun observeSession() = presence }
        val centerRepo = object : DonationCenterRepository { override suspend fun getCenters(): Result<List<DonationCenter>> { centerReads++; return Result.success(centers) } }
        val slots = object : DonationTimeSlotRepository by FakeDonationTimeSlotRepository() {
            override suspend fun getTimeSlots(centerId: String, fromInclusive: Instant, untilExclusive: Instant): Result<List<DonationTimeSlot>> {
                slotReads++
                return if (!available) Result.success(emptyList()) else FakeDonationTimeSlotRepository().getTimeSlots(centerId, fromInclusive, untilExclusive)
            }
        }
        val appointments = object : AppointmentRepository by FakeAppointmentRepository() {
            override suspend fun createAppointmentForOwner(ownerId: String, timeSlotId: String, idempotencyKey: String): Result<Appointment> {
                if (ownerId != a.id) return Result.failure(DataFailure(DataError.SESSION_UNAVAILABLE))
                return createAppointment(timeSlotId, idempotencyKey)
            }
            override suspend fun createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment> {
                keys += idempotencyKey; return answer(timeSlotId, idempotencyKey)
            }
        }
        lateinit var model: SchedulingViewModel
        main { model = ViewModelProvider(store, viewModelFactory { initializer {
            SchedulingViewModel(centerRepo, GetDonationTimeSlotsUseCase(slots, clock), appointments,
                GetAvailableDonationDatesUseCase(slots, clock), clock, storage(), auth)
        } })[SchedulingViewModel::class.java] }
        await { model.draftRecovery.value.phase != DraftRecoveryPhase.LOADING }
        return model
    }
    private fun closeModels() = main { stores.forEach { it.clear() }; stores.clear() }
    @After fun close() { closeModels(); directory.deleteRecursively() }
    private fun fill(model: SchedulingViewModel) {
        main { model.selectCenter(center); model.selectDate(date) }
        await { model.uiState.value.timeSlots.isNotEmpty() }
        main { model.selectTimeSlot(model.uiState.value.timeSlots.first().id); model.onStepVisible(SchedulingStep.CONFIRM) }
        await { runBlocking { FileSchedulingDraftStore(directory).read(a.id).getOrNull()?.slotId != null } }
    }
    private fun prepared() = SchedulingDraft(a.id, center.id, center.name, center.timeZoneId, date.toString(),
        "test-slot-2026-11-20T11:00:00Z", "2026-11-20T11:00:00Z", SchedulingStep.CONFIRM,
        "00000000-0000-4000-8000-000000000099")
    @Test fun unsentChoicesSurviveNewVmAndAreRevalidatedWithoutSending() {
        val first = model(); fill(first); closeModels()
        val restored = model(); assertEquals(DraftRecoveryPhase.OFFER, restored.draftRecovery.value.phase)
        val reads = slotReads; main { restored.resumeDraft() }
        await { restored.draftRecovery.value.phase == DraftRecoveryPhase.READY }
        assertTrue(slotReads > reads); assertEquals(date, restored.uiState.value.selectedDate)
        assertNotNull(restored.uiState.value.selectedTimeSlotId); assertTrue(keys.isEmpty())
    }
    @Test fun changedAvailabilityDropsOnlyTheInvalidSlotAndExplainsIt() {
        val first = model(); fill(first); closeModels(); available = false
        val restored = model(); main { restored.resumeDraft() }
        await { restored.draftRecovery.value.phase == DraftRecoveryPhase.READY }
        assertEquals(date, restored.uiState.value.selectedDate); assertNull(restored.uiState.value.selectedTimeSlotId)
        assertTrue(restored.uiState.value.draftNotice!!.contains("não está mais disponível")); assertTrue(keys.isEmpty())
    }
    @Test fun missingUnitAndPastDateReturnToTheAppropriateChoice() = runBlocking<Unit> {
        FileSchedulingDraftStore(directory).save(prepared().copy(requestKey = null, dateIso = "2026-11-01", slotId = null, startsAtIso = null)).getOrThrow()
        val restored = model(); main { restored.resumeDraft() }
        await { restored.draftRecovery.value.phase == DraftRecoveryPhase.READY }
        assertNull(restored.uiState.value.selectedDate); assertTrue(restored.uiState.value.draftNotice!!.contains("já passou"))
        closeModels(); centers = emptyList()
        val missing = model(); main { missing.resumeDraft() }
        await { missing.draftRecovery.value.phase == DraftRecoveryPhase.READY }
        assertNull(missing.uiState.value.selectedCenter); assertEquals(SchedulingStep.CENTER, missing.draftRecovery.value.resumeStep)
    }
    @Test fun diskFailurePreventsRpcAndRetryUsesSamePreparedIdentity() {
        val first = model(); fill(first); failWrite = true
        main { first.confirmAppointment() }
        await { !first.uiState.value.appointmentCreation.isLoading }
        assertTrue(keys.isEmpty()); assertTrue(first.uiState.value.appointmentCreation.errorMessage!!.contains("neste aparelho"))
        failWrite = false; main { first.confirmAppointment() }
        await { first.uiState.value.appointmentCreation.appointment != null }
        val saved = runBlocking { storage().read(a.id).getOrThrow()!! }
        assertEquals(keys.single(), saved.requestKey)
    }
    @Test fun lostResponseThenNewVmReusesSameKeyEvenWhenOfferIsGone() {
        answer = { _, _ -> Result.failure(DataFailure(DataError.NETWORK)) }
        val first = model(); fill(first); main { first.confirmAppointment() }
        await { first.uiState.value.appointmentCreation.errorMessage != null }
        val original = keys.single(); closeModels(); centers = emptyList(); available = false
        val restored = model(); val reads = slotReads
        main { restored.resumeDraft(); restored.consumeDraftResumeStep() }
        await { restored.draftRecovery.value.phase == DraftRecoveryPhase.READY }
        assertEquals(reads, slotReads); assertEquals(1, keys.size)
        answer = { slot, _ -> Result.success(Appointment("original-result", a.id, slot, AppointmentStatus.SCHEDULED, clock.instant())) }
        main { restored.selectDate(date.plusDays(1)); restored.loadTimeSlots(); restored.confirmAppointment() }
        await { restored.uiState.value.appointmentCreation.appointment != null }
        assertEquals(listOf(original, original), keys)
    }
    @Test fun preparedPastRequestReturnsCancelledWithoutReactivationAndClearsOnlyOnFinish() = runBlocking<Unit> {
        val draft = prepared().copy(dateIso = "2026-11-01", startsAtIso = "2026-11-01T11:00:00Z")
        storage().save(draft).getOrThrow()
        val restored = model(); main { restored.resumeDraft() }
        await { restored.draftRecovery.value.phase == DraftRecoveryPhase.READY }
        answer = { slot, _ -> Result.success(Appointment("original-cancelled", a.id, slot, AppointmentStatus.CANCELLED, clock.instant())) }
        main { restored.confirmAppointment() }
        await { restored.uiState.value.appointmentCreation.appointment != null }
        assertEquals(AppointmentStatus.CANCELLED, restored.uiState.value.appointmentCreation.appointment!!.status)
        assertNotNull(storage().read(a.id).getOrThrow())
        main { restored.finishDraft() }; await { restored.uiState.value.draftFinished }
        assertNull(storage().read(a.id).getOrThrow())
    }
    @Test fun otherAccountNeverReceivesPreviousChoicesOrKey() = runBlocking<Unit> {
        storage().save(prepared()).getOrThrow()
        presence.value = SessionPresence.Authenticated(AuthenticatedUser("synthetic-b"))
        val other = model(); assertEquals(DraftRecoveryPhase.READY, other.draftRecovery.value.phase)
        assertNull(other.uiState.value.selectedCenter); assertNotNull(storage().read(a.id).getOrThrow()); assertTrue(keys.isEmpty())
    }
    @Test fun readFailureWaitsForExplicitRetryAndNeverSends() {
        failRead = true; val first = model()
        assertEquals(DraftRecoveryPhase.ERROR, first.draftRecovery.value.phase)
        failRead = false; main { first.retryDraftRecovery() }
        await { first.draftRecovery.value.phase == DraftRecoveryPhase.READY }; assertTrue(keys.isEmpty())
    }
    @Test fun preparedIdentityIsAlreadyDurableWhileRpcIsPendingAndLateReplyIsIgnored() {
        val pending = CompletableDeferred<Result<Appointment>>()
        answer = { _, _ -> withContext(NonCancellable) { pending.await() } }
        try {
            val first = model(); fill(first); main { first.confirmAppointment() }
            await { keys.isNotEmpty() }
            assertEquals(keys.single(), runBlocking { storage().read(a.id).getOrThrow()!!.requestKey })
            closeModels()
            main { pending.complete(Result.success(Appointment("late", a.id, "test-slot-2026-11-20T11:00:00Z", AppointmentStatus.SCHEDULED, clock.instant()))) }
            assertNull(first.uiState.value.appointmentCreation.appointment)
            val restored = model(); assertNotNull(restored.draftRecovery.value.draft?.requestKey)
        } finally { pending.complete(Result.failure(DataFailure(DataError.UNKNOWN))) }
    }
    @Test fun onlyDefiniteRejectionAllowsExplicitReviewAndANewRequestIdentity() = runBlocking<Unit> {
        answer = { _, _ -> Result.failure(DataFailure(DataError.SLOT_FULL)) }
        val first = model(); fill(first); main { first.confirmAppointment() }
        await { first.uiState.value.canReviewRejectedRequest }
        val original = keys.single()
        assertEquals(original, storage().read(a.id).getOrThrow()!!.requestKey)
        main { first.reviewRejectedRequest() }
        await { first.draftRecovery.value.phase == DraftRecoveryPhase.OFFER }
        assertNull(storage().read(a.id).getOrThrow()!!.requestKey)
        main { first.resumeDraft() }; await { first.draftRecovery.value.phase == DraftRecoveryPhase.READY }
        answer = { slot, _ -> Result.success(Appointment("new-intent", a.id, slot, AppointmentStatus.SCHEDULED, clock.instant())) }
        main { first.confirmAppointment() }; await { first.uiState.value.appointmentCreation.appointment != null }
        assertEquals(2, keys.size); assertNotEquals(original, keys.last())
    }
    @Test fun cleanupFailureRetriesOnlyLocalCleanupAndTemporaryRefreshPreservesOwner() = runBlocking<Unit> {
        val first = model(); fill(first)
        main { presence.value = SessionPresence.RefreshPending }
        assertEquals(date, first.uiState.value.selectedDate)
        assertEquals(DraftRecoveryPhase.READY, first.draftRecovery.value.phase)
        main { presence.value = SessionPresence.Authenticated(a); first.confirmAppointment() }
        await { first.uiState.value.appointmentCreation.appointment != null }
        failClear = true; main { first.finishDraft() }
        await { first.uiState.value.draftNotice?.contains("limpeza local") == true }
        assertFalse(first.uiState.value.draftFinished); assertNotNull(storage().read(a.id).getOrThrow())
        failClear = false; main { first.finishDraft() }; await { first.uiState.value.draftFinished }
        assertEquals(1, keys.size); assertNull(storage().read(a.id).getOrThrow())
        main { first.finishDraft() }; assertEquals(1, keys.size)
    }
}
