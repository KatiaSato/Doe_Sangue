package br.edu.fatec.doesangue.presentation.scheduling

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.domain.usecase.*
import br.edu.fatec.doesangue.ui.navigation.FakeAppointmentRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

@RunWith(AndroidJUnit4::class)
class AvailabilityViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val store = ViewModelStore()
    private lateinit var model: SchedulingViewModel
    private val clock = MovingClock()
    private val a = DonationCenter("1", "Unidade Sintética A", "Cidade fictícia", "Endereço fictício", "", "", "America/Sao_Paulo")
    private val b = a.copy(id = "2", name = "Unidade Sintética B")
    private val month = YearMonth.of(2026, 11)
    private val date = LocalDate.of(2026, 10, 26)
    private val slot = DonationTimeSlot("slot", a.id, clock.instant().plusSeconds(1800), 3, 1)
    private val dateReads = mutableListOf<Pair<String, YearMonth>>()
    private var slotReads = 0
    private val keys = mutableListOf<String>()
    private var datesAnswer: suspend (String, YearMonth) -> Result<Set<LocalDate>> = { _, m -> Result.success(setOf(m.atDay(28))) }
    private var slotsAnswer: suspend () -> Result<List<DonationTimeSlot>> = { Result.success(listOf(slot)) }

    private fun create() {
        val centers = object : DonationCenterRepository { override suspend fun getCenters() = Result.success(listOf(a,b)) }
        val times = object : DonationTimeSlotRepository {
            override suspend fun getAvailableDates(centerId: String, month: YearMonth): Result<Set<LocalDate>> {
                dateReads += centerId to month
                return datesAnswer(centerId, month)
            }
            override suspend fun getTimeSlots(centerId: String, fromInclusive: Instant, untilExclusive: Instant): Result<List<DonationTimeSlot>> {
                slotReads++
                return slotsAnswer()
            }
        }
        val appointments = object : AppointmentRepository by FakeAppointmentRepository() {
            override suspend fun createAppointment(timeSlotId: String, idempotencyKey: String): Result<Appointment> {
                keys += idempotencyKey
                return Result.failure(IllegalStateException("Resposta fictícia indisponível"))
            }
        }
        onMain {
            model = ViewModelProvider(store, viewModelFactory { initializer {
                SchedulingViewModel(centers, GetDonationTimeSlotsUseCase(times,clock), appointments,
                    GetAvailableDonationDatesUseCase(times,clock), clock)
            } })[SchedulingViewModel::class.java]
            model.selectCenter(a)
        }
    }
    @After fun clear() = onMain { store.clear() }

    @Test fun calendarIsLazyBlocksDuplicatePendingReadAndSupportsFailureRetryAndRefresh() {
        create()
        assertTrue(dateReads.isEmpty())
        val pending = CompletableDeferred<Result<Set<LocalDate>>>()
        datesAnswer = { _, _ -> pending.await() }
        try {
            onMain { model.loadAvailableDates(month); model.loadAvailableDates(month) }
            assertEquals(1,dateReads.size)
            assertTrue(model.uiState.value.calendarAvailability.isLoading)
            onMain { pending.complete(Result.failure(IllegalStateException("detalhe interno"))) }
            assertEquals("Não foi possível consultar os dias disponíveis. Tente novamente.", model.uiState.value.calendarAvailability.errorMessage)
            datesAnswer = { _, _ -> Result.success(setOf(month.atDay(20))) }
            onMain { model.loadAvailableDates(month) }
            assertEquals(setOf(month.atDay(20)),model.uiState.value.calendarAvailability.dates)
            datesAnswer = { _, _ -> Result.success(emptySet()) }
            onMain { model.loadAvailableDates(month) }
            assertEquals(3,dateReads.size)
            assertTrue(model.uiState.value.calendarAvailability.dates.isEmpty())
            assertNull(model.uiState.value.calendarAvailability.errorMessage)
        } finally { pending.complete(Result.success(emptySet())) }
    }

    @Test fun lateMonthAndUnitResponsesCannotReplaceCurrentCalendar() {
        create()
        val old = CompletableDeferred<Result<Set<LocalDate>>>()
        val other = CompletableDeferred<Result<Set<LocalDate>>>()
        datesAnswer = { _, m -> when(m) {
            month -> withContext(NonCancellable) { old.await() }
            month.plusMonths(2) -> withContext(NonCancellable) { other.await() }
            else -> Result.success(setOf(m.atDay(20)))
        } }
        try {
            onMain { model.loadAvailableDates(month); model.loadAvailableDates(month.plusMonths(1)) }
            val current = model.uiState.value.calendarAvailability
            onMain { old.complete(Result.success(setOf(month.atDay(21)))) }
            assertEquals(current,model.uiState.value.calendarAvailability)
            onMain { model.loadAvailableDates(month.plusMonths(2)); model.selectCenter(b) }
            onMain { other.complete(Result.success(setOf(month.plusMonths(2).atDay(20)))) }
            assertEquals(CalendarAvailabilityUiState(),model.uiState.value.calendarAvailability)
            assertEquals(b,model.uiState.value.selectedCenter)
        } finally { old.complete(Result.success(emptySet())); other.complete(Result.success(emptySet())) }
    }

    @Test fun pastDayAndExpiredFullOrForeignSlotsCannotBeChosen() {
        create()
        onMain { model.selectDate(date.minusDays(1)) }
        assertEquals(0,slotReads)
        slotsAnswer = { Result.success(listOf(slot.copy(id="past", startsAt=clock.instant().minusSeconds(1)),
            slot.copy(id="now", startsAt=clock.instant()), slot.copy(id="full",availableSpots=0),
            slot.copy(id="foreign",centerId=b.id),slot)) }
        onMain { model.selectDate(date); model.selectTimeSlot("full") }
        assertEquals(listOf(slot),model.uiState.value.timeSlots)
        assertNull(model.uiState.value.selectedTimeSlotId)
        onMain { model.selectTimeSlot(slot.id) }
        assertEquals(slot.id,model.uiState.value.selectedTimeSlotId)
        assertTrue(keys.isEmpty())
    }

    @Test fun firstConfirmationAfterSlotExpiresHasSpecificMessageAndDoesNotWrite() {
        create()
        chooseSlot()
        clock.current = slot.startsAt
        onMain { model.confirmAppointment() }
        assertTrue(keys.isEmpty())
        assertEquals("Este horário não está mais disponível. Volte e escolha outro horário.",
            model.uiState.value.appointmentCreation.errorMessage)
    }

    @Test fun retryOfAlreadySentRequestKeepsSameKeyEvenAfterStart() {
        create()
        chooseSlot()
        onMain { model.confirmAppointment() }
        clock.current = slot.startsAt.plusSeconds(1)
        onMain { model.confirmAppointment() }
        assertEquals(2,keys.size)
        assertEquals(keys[0],keys[1])
    }

    @Test fun explicitRefreshClearsChoiceWhenLastVacancyIsGone() {
        create()
        chooseSlot()
        slotsAnswer = { Result.success(emptyList()) }
        onMain { model.loadTimeSlots() }
        assertTrue(model.uiState.value.timeSlots.isEmpty())
        assertNull(model.uiState.value.selectedTimeSlotId)
        assertNull(model.uiState.value.selectedTime)
        assertFalse(model.uiState.value.isLoadingTimeSlots)
        assertTrue(keys.isEmpty())
    }

    @Test fun calendarResponseAfterViewModelClearIsIgnored() {
        create()
        val pending = CompletableDeferred<Result<Set<LocalDate>>>()
        datesAnswer = { _, _ -> withContext(NonCancellable) { pending.await() } }
        try {
            onMain { model.loadAvailableDates(month) }
            val before = model.uiState.value
            onMain { store.clear(); pending.complete(Result.success(setOf(month.atDay(20)))) }
            assertEquals(before,model.uiState.value)
        } finally { pending.complete(Result.success(emptySet())) }
    }

    private fun chooseSlot() = onMain { model.selectDate(date); model.selectTimeSlot(slot.id) }
    private fun onMain(action: () -> Unit) { instrumentation.runOnMainSync { action() }; instrumentation.waitForIdleSync() }
    private class MovingClock : Clock() {
        var current: Instant = Instant.parse("2026-10-26T15:00:00Z")
        override fun instant() = current
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(current,zone)
    }
}
