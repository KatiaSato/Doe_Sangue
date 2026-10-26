package br.edu.fatec.doesangue.domain.usecase

import br.edu.fatec.doesangue.data.supabase.dto.DonationTimeSlotDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.DonationTimeSlotRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class AvailabilityUseCasesTest {
    private val now = Instant.parse("2026-10-26T15:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val center = DonationCenter("1", "Unidade Sintética", "Cidade fictícia", "Endereço fictício",
        "", "", "America/Sao_Paulo")
    private val date = LocalDate.of(2026, 10, 26)
    private class Source : DonationTimeSlotRepository {
        var slots: Result<List<DonationTimeSlot>> = Result.success(emptyList())
        var dates: Result<Set<LocalDate>> = Result.success(emptySet())
        var bounds: Pair<Instant, Instant>? = null
        var cancellation = false
        override suspend fun getTimeSlots(centerId: String, fromInclusive: Instant, untilExclusive: Instant): Result<List<DonationTimeSlot>> {
            if (cancellation) throw CancellationException("Cancelamento sintético")
            bounds = fromInclusive to untilExclusive
            return slots
        }
        override suspend fun getAvailableDates(centerId: String, month: YearMonth): Result<Set<LocalDate>> {
            if (cancellation) throw CancellationException("Cancelamento sintético")
            return dates
        }
    }

    @Test fun filtersPastExactNowFullWrongCenterAndWrongLocalDayThenOrders() = runBlocking<Unit> {
        val repo = Source()
        fun slot(id: String, time: Instant, spots: Int = 1, unit: String = center.id) =
            DonationTimeSlot(id, unit, time, 3, spots)
        repo.slots = Result.success(listOf(slot("late", now.plusSeconds(7_200)), slot("early", now.plusSeconds(60)),
            slot("past", now.minusSeconds(1)), slot("now", now), slot("full", now.plusSeconds(60), 0),
            slot("other", now.plusSeconds(60), unit = "2"), slot("tomorrow", Instant.parse("2026-10-27T03:00:00Z"))))
        assertEquals(listOf("early", "late"), GetDonationTimeSlotsUseCase(repo, clock)(center, date).getOrThrow().map { it.id })
        assertEquals(Instant.parse("2026-10-26T03:00:00Z") to Instant.parse("2026-10-27T03:00:00Z"), repo.bounds)
    }

    @Test fun localDayBoundsHandleDaylightSavingWithoutAssuming24Hours() = runBlocking<Unit> {
        val repo = Source()
        GetDonationTimeSlotsUseCase(repo, Clock.fixed(Instant.parse("2018-11-03T00:00:00Z"), ZoneOffset.UTC))(
            center, LocalDate.of(2018, 11, 4)).getOrThrow()
        val bounds = requireNotNull(repo.bounds)
        assertEquals(23, Duration.between(bounds.first, bounds.second).toHours())
    }

    @Test fun calendarDropsWrongMonthAndPastDatesButKeepsTodayInUnitZone() = runBlocking<Unit> {
        val repo = Source()
        repo.dates = Result.success(setOf(date.minusDays(1), date, date.plusDays(1), date.plusMonths(1)))
        assertEquals(setOf(date, date.plusDays(1)), GetAvailableDonationDatesUseCase(repo, clock)(
            center, YearMonth.from(date)).getOrThrow())
        val utcStart = Clock.fixed(Instant.parse("2026-10-27T01:00:00Z"), ZoneOffset.UTC)
        assertTrue(date in GetAvailableDonationDatesUseCase(repo, utcStart)(center, YearMonth.from(date)).getOrThrow())
    }

    @Test fun failuresAndCancellationArePreserved() = runBlocking<Unit> {
        val repo = Source()
        val failure = IllegalStateException("falha sintética")
        repo.slots = Result.failure(failure); repo.dates = Result.failure(failure)
        assertSame(failure, GetDonationTimeSlotsUseCase(repo, clock)(center, date).exceptionOrNull())
        assertSame(failure, GetAvailableDonationDatesUseCase(repo, clock)(center, YearMonth.from(date)).exceptionOrNull())
        repo.cancellation = true
        assertThrows(CancellationException::class.java) { runBlocking { GetDonationTimeSlotsUseCase(repo, clock)(center, date) } }
        assertThrows(CancellationException::class.java) { runBlocking { GetAvailableDonationDatesUseCase(repo, clock)(center, YearMonth.from(date)) } }
    }

    @Test fun dtoRequiresAvailabilityAndPreservesOffsetsAndLargeIds() {
        val json = Json { ignoreUnknownKeys = true }
        val payload = """{"id":9000000000,"unidade_id":1,"inicio":"2026-11-20T08:00:00-03:00","capacidade":3,"vagas_disponiveis":1}"""
        val slot = json.decodeFromString<DonationTimeSlotDto>(payload).toDomain()
        assertEquals("9000000000", slot.id)
        assertEquals(Instant.parse("2026-11-20T11:00:00Z"), slot.startsAt)
        assertEquals(1, slot.availableSpots)
        assertThrows(Exception::class.java) { json.decodeFromString<DonationTimeSlotDto>(payload.replace(",\"vagas_disponiveis\":1", "")) }
        assertThrows(IllegalArgumentException::class.java) { DonationTimeSlot("x", "1", now, 2, 3) }
        assertThrows(IllegalArgumentException::class.java) { DonationTimeSlot("x", "1", now, 2, -1) }
    }
}
