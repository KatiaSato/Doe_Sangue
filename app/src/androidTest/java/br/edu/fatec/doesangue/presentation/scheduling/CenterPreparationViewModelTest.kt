package br.edu.fatec.doesangue.presentation.scheduling

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.domain.usecase.GetDonationTimeSlotsUseCase
import br.edu.fatec.doesangue.ui.navigation.FakeAppointmentRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** ViewModel real, repositories fictícios: sem Activity, rede ou reservas no Supabase. */
@RunWith(AndroidJUnit4::class)
class CenterPreparationViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private lateinit var model: SchedulingViewModel
    private val a = DonationCenter("a", "Unidade Sintética A", "Cidade fictícia", "Endereço A",
        "", "", "America/Sao_Paulo")
    private val b = a.copy(id = "b", name = "Unidade Sintética B", address = "Endereço B")
    private val date = LocalDate.of(2026, 11, 20)
    private var reads = 0
    private var centerAnswer: suspend () -> Result<List<DonationCenter>> = { Result.success(listOf(a, b)) }
    private val slotReads = mutableListOf<String>()
    private var slotAnswer: suspend (String) -> Result<List<DonationTimeSlot>> = { id -> Result.success(listOf(slot(id))) }
    private val writes = mutableListOf<Pair<String, String>>()
    private var writeAnswer: suspend (String, String) -> Result<Appointment> = { _, _ ->
        Result.failure(IllegalStateException("falha fictícia"))
    }

    private fun createModel() {
        val centers = object : DonationCenterRepository {
            override suspend fun getCenters(): Result<List<DonationCenter>> {
                reads++
                return centerAnswer()
            }
        }
        val slots = object : DonationTimeSlotRepository {
            override suspend fun getTimeSlots(centerId: String, fromInclusive: Instant,
                untilExclusive: Instant): Result<List<DonationTimeSlot>> {
                slotReads += centerId
                return slotAnswer(centerId)
            }
        }
        val appointments = object : AppointmentRepository by FakeAppointmentRepository() {
            override suspend fun createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment> {
                writes += timeSlotId to idempotencyKey
                return writeAnswer(timeSlotId, idempotencyKey)
            }
        }
        onMain {
            model = ViewModelProvider(store, viewModelFactory {
                initializer { SchedulingViewModel(centers, GetDonationTimeSlotsUseCase(slots), appointments) }
            })[SchedulingViewModel::class.java]
        }
    }

    @After fun clearModel() = onMain { store.clear() }

    @Test fun generalEntryLoadsCatalogWithoutSelectingAnOriginOrWriting() {
        createModel()
        val state = model.uiState.value
        assertEquals(listOf(a, b), state.centers)
        assertEquals(CenterPreparationUiState.Idle, state.centerPreparation)
        assertNull(state.selectedCenter)
        assertFalse(state.isLoading)
        assertEquals(1, reads)
        assertTrue(writes.isEmpty())
        assertTrue(slotReads.isEmpty())
    }

    @Test fun originWaitsForInitialReadAndDuplicateRequestsDoNotStartAnotherRead() {
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        centerAnswer = { pending.await() }
        createModel()
        try {
            onMain {
                model.prepareCenterFromOrigin(b.id)
                model.prepareCenterFromOrigin(b.id)
                model.loadCenters()
            }
            assertEquals(1, reads)
            assertEquals(CenterPreparationUiState.Loading(b.id), model.uiState.value.centerPreparation)
            assertNull(model.uiState.value.selectedCenter)
            val fresh = b.copy(name = "Unidade Sintética B atualizada")
            onMain { pending.complete(Result.success(listOf(a, fresh))) }
            assertEquals(fresh, model.uiState.value.selectedCenter)
            assertEquals(CenterPreparationUiState.Ready(b.id), model.uiState.value.centerPreparation)
            assertFalse(model.uiState.value.isLoading)
            assertTrue(writes.isEmpty())
        } finally { pending.complete(Result.success(listOf(a, b))) }
    }

    @Test fun originAfterInitialReadRequeriesAndUsesCurrentDataRatherThanOldCatalog() {
        createModel()
        val fresh = a.copy(name = "Nome sintético novo", timeZoneId = "UTC")
        centerAnswer = { Result.success(listOf(fresh)) }
        onMain { model.prepareCenterFromOrigin(a.id) }
        assertEquals(2, reads)
        assertEquals(fresh, model.uiState.value.selectedCenter)
        assertEquals(CenterPreparationUiState.Ready(a.id), model.uiState.value.centerPreparation)
        onMain { model.selectDate(date) }
        val beforeDuplicate = model.uiState.value
        onMain { model.prepareCenterFromOrigin(a.id) }
        assertEquals(2, reads)
        assertEquals(beforeDuplicate, model.uiState.value)
    }

    @Test fun blankIdHasSpecificErrorWithoutAnotherReadOrSelection() {
        createModel()
        onMain { model.prepareCenterFromOrigin("   ") }
        assertEquals(1, reads)
        assertEquals(CenterPreparationUiState.Error("   ",
            "Não foi possível identificar a unidade. Volte e tente novamente."), model.uiState.value.centerPreparation)
        assertNull(model.uiState.value.selectedCenter)
        assertTrue(writes.isEmpty())
    }

    @Test fun unavailableOriginNeverFallsBackToAnotherUnitAndCanBeRetried() {
        createModel()
        centerAnswer = { Result.success(listOf(b)) }
        onMain { model.prepareCenterFromOrigin(a.id) }
        assertEquals(CenterPreparationUiState.Error(a.id,
            "Esta unidade não está mais disponível para agendamento."), model.uiState.value.centerPreparation)
        assertNull(model.uiState.value.selectedCenter)
        centerAnswer = { Result.success(listOf(a, b)) }
        onMain { model.prepareCenterFromOrigin(a.id) }
        assertEquals(CenterPreparationUiState.Ready(a.id), model.uiState.value.centerPreparation)
        assertEquals(a, model.uiState.value.selectedCenter)
        assertEquals(3, reads)
        assertTrue(writes.isEmpty())
    }

    @Test fun failedPendingReadEndsLoadingAndRetryCanReachReadyWithoutInternalErrorText() {
        createModel()
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        centerAnswer = { pending.await() }
        try {
            onMain { model.prepareCenterFromOrigin(a.id) }
            assertEquals(CenterPreparationUiState.Loading(a.id), model.uiState.value.centerPreparation)
            onMain { pending.complete(Result.failure(IllegalStateException("detalhe interno fictício"))) }
            assertFalse(model.uiState.value.isLoading)
            assertEquals("Não foi possível carregar as unidades.", model.uiState.value.errorMessage)
            assertEquals(CenterPreparationUiState.Error(a.id, "Não foi possível carregar as unidades."),
                model.uiState.value.centerPreparation)
            assertNull(model.uiState.value.selectedCenter)
            centerAnswer = { Result.success(listOf(a)) }
            onMain { model.prepareCenterFromOrigin(a.id) }
            assertNull(model.uiState.value.errorMessage)
            assertEquals(CenterPreparationUiState.Ready(a.id), model.uiState.value.centerPreparation)
            assertEquals(3, reads)
        } finally { pending.complete(Result.success(listOf(a))) }
    }

    @Test fun newestOriginWinsWhileSharedCatalogReadIsStillPending() {
        val pending = CompletableDeferred<Result<List<DonationCenter>>>()
        centerAnswer = { pending.await() }
        createModel()
        try {
            onMain {
                model.prepareCenterFromOrigin(a.id)
                model.prepareCenterFromOrigin(b.id)
                pending.complete(Result.success(listOf(a, b)))
            }
            assertEquals(1, reads)
            assertEquals(b, model.uiState.value.selectedCenter)
            assertEquals(CenterPreparationUiState.Ready(b.id), model.uiState.value.centerPreparation)
        } finally { pending.complete(Result.success(listOf(a, b))) }
    }

    @Test fun newOriginClearsDateSlotFailedWriteAndIdempotencyKeyEvenForSameUnit() {
        createModel()
        chooseDateAndSlot(a)
        onMain { model.confirmAppointment(); model.confirmAppointment() }
        assertEquals(2, writes.size)
        assertEquals(writes[0].second, writes[1].second) // A tentativa do mesmo pedido reutiliza a chave.
        onMain { model.prepareCenterFromOrigin(a.id) }
        val cleared = model.uiState.value
        assertEquals(CenterPreparationUiState.Ready(a.id), cleared.centerPreparation)
        assertNull(cleared.selectedDate)
        assertNull(cleared.selectedTime)
        assertNull(cleared.selectedTimeSlotId)
        assertTrue(cleared.timeSlots.isEmpty())
        assertEquals(AppointmentCreationUiState(), cleared.appointmentCreation)
        chooseDateAndSlot(a)
        onMain { model.confirmAppointment() }
        assertEquals(3, writes.size)
        assertNotEquals(writes[0].second, writes[2].second)
    }

    @Test fun activeWriteBlocksOriginChangeAndPreservesOriginalRequest() {
        createModel()
        val pending = CompletableDeferred<Result<Appointment>>()
        writeAnswer = { _, _ -> pending.await() }
        chooseDateAndSlot(a)
        try {
            onMain { model.confirmAppointment() }
            val before = model.uiState.value
            assertTrue(before.appointmentCreation.isLoading)
            onMain { model.prepareCenterFromOrigin(b.id); model.prepareCenterFromOrigin("") }
            assertEquals(before, model.uiState.value)
            assertEquals(1, reads)
            assertEquals(1, writes.size)
            onMain { pending.complete(Result.failure(IllegalStateException("falha fictícia"))) }
            writeAnswer = { _, _ -> Result.failure(IllegalStateException("falha fictícia")) }
            onMain { model.confirmAppointment() }
            assertEquals(writes[0].second, writes[1].second)
        } finally { pending.complete(Result.failure(IllegalStateException("falha fictícia"))) }
    }

    @Test fun lateTimeSlotResponseCannotRestoreDraftAfterOriginChanges() {
        createModel()
        val pending = CompletableDeferred<Result<List<DonationTimeSlot>>>()
        slotAnswer = { withContext(NonCancellable) { pending.await() } }
        try {
            onMain { model.selectCenter(a); model.selectDate(date) }
            assertTrue(model.uiState.value.isLoadingTimeSlots)
            onMain { model.prepareCenterFromOrigin(b.id) }
            val beforeLateResponse = model.uiState.value
            onMain { pending.complete(Result.success(listOf(slot(a.id)))) }
            assertEquals(beforeLateResponse, model.uiState.value)
            assertEquals(b, model.uiState.value.selectedCenter)
            assertTrue(model.uiState.value.timeSlots.isEmpty())
        } finally { pending.complete(Result.success(emptyList())) }
    }

    @Test fun clearedViewModelIgnoresLateCatalogSuccessAndFailure() {
        // Duas instâncias independentes exercitam respostas que ignoram o cancelamento da fonte.
        for (response in listOf(Result.success(listOf(a, b)),
            Result.failure<List<DonationCenter>>(IllegalStateException("falha fictícia")))) {
            val pending = CompletableDeferred<Result<List<DonationCenter>>>()
            centerAnswer = { withContext(NonCancellable) { pending.await() } }
            createModel()
            try {
                onMain { model.prepareCenterFromOrigin(a.id) }
                val beforeClear = model.uiState.value
                onMain { store.clear() }
                onMain { pending.complete(response) }
                assertEquals(beforeClear, model.uiState.value)
            } finally {
                pending.complete(Result.success(emptyList()))
                onMain { store.clear() }
            }
        }
    }

    private fun chooseDateAndSlot(center: DonationCenter) = onMain {
        model.selectCenter(center)
        model.selectDate(date)
        model.selectTimeSlot(slot(center.id).id)
        assertEquals(slot(center.id).id, model.uiState.value.selectedTimeSlotId)
    }

    private fun slot(id: String) = DonationTimeSlot("slot-sintetico-$id", id,
        date.atTime(LocalTime.of(8, 0)).atZone(ZoneId.of(a.timeZoneId)).toInstant(), 2)

    private fun onMain(action: () -> Unit) {
        instrumentation.runOnMainSync { action() }
        instrumentation.waitForIdleSync()
    }
}
