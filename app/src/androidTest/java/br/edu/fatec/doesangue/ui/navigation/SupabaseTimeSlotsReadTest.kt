package br.edu.fatec.doesangue.ui.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.BuildConfig
import br.edu.fatec.doesangue.data.repository.SupabaseDonationTimeSlotRepository
import br.edu.fatec.doesangue.data.supabase.SupabaseDonationCenterDataSource
import br.edu.fatec.doesangue.data.supabase.SupabaseDonationTimeSlotDataSource
import br.edu.fatec.doesangue.domain.usecase.GetAvailableDonationDatesUseCase
import br.edu.fatec.doesangue.domain.usecase.GetDonationTimeSlotsUseCase
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.*

/** Somente leitura pública; fixture positiva exige seed autorizado. Não cria oferta/reserva. */
@RunWith(AndroidJUnit4::class)
class SupabaseTimeSlotsReadTest {
    @Test fun evaluationUnitReturnsPositiveFutureDaysAndSlotsUsingCurrentPublicApi() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("enableSupabaseReadTests") == "true")
        val today = LocalDate.now(ZoneId.of("America/Sao_Paulo"))
        assumeTrue("Massa de avaliação tem ofertas até 09/12/2026; não criar ofertas neste teste.",
            today <= LocalDate.of(2026,12,9))
        val client = createSupabaseClient(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }
        try {
            val center = kotlinx.coroutines.withTimeout(25_000) {
                SupabaseDonationCenterDataSource(client).getCenters().first { it.name.startsWith("Unidade Escola Demo") }
            }
            val repository = SupabaseDonationTimeSlotRepository(SupabaseDonationTimeSlotDataSource(client))
            val getDates = GetAvailableDonationDatesUseCase(repository)
            val getSlots = GetDonationTimeSlotsUseCase(repository)
            val dates = kotlinx.coroutines.withTimeout(25_000) {
                (0L..2L).flatMap { offset -> getDates(center,YearMonth.from(today).plusMonths(offset)).getOrThrow() }
                    .sorted()
            }
            assertTrue("Oferta positiva da massa não retornada pela consulta pública",dates.isNotEmpty())
            val slots = kotlinx.coroutines.withTimeout(25_000) { getSlots(center,dates.first()).getOrThrow() }
            assertTrue(slots.isNotEmpty())
            assertTrue(slots.all { it.centerId == center.id && it.availableSpots > 0 && it.startsAt > Instant.now() })
        } finally { client.close() }
    }

    @Test fun currentAvailabilityUsesPublishedUnitAndPositiveFutureSlotsInItsZone() = runBlocking {
        assumeTrue("Consulta remota exige execução explícita no projeto acadêmico.",
            InstrumentationRegistry.getArguments().getString("enableSupabaseReadTests") == "true")
        // Sem plugin Auth: não lê, renova nem grava a sessão da usuária.
        val client = createSupabaseClient(BuildConfig.SUPABASE_URL,BuildConfig.SUPABASE_PUBLISHABLE_KEY) { install(Postgrest) }
        try {
        val repository=SupabaseDonationTimeSlotRepository(SupabaseDonationTimeSlotDataSource(client))
        val getDates=GetAvailableDonationDatesUseCase(repository)
        val getSlots=GetDonationTimeSlotsUseCase(repository)
        val center = SupabaseDonationCenterDataSource(client).getCenters()
            .single { it.name == "Unidade Demo A" && it.city == "Cidade fictícia" }
        val zone = ZoneId.of(center.timeZoneId)
        val month = YearMonth.now(zone)
        val dates = getDates(center,month).getOrThrow()
        val expectSeed = InstrumentationRegistry.getArguments().getString("expectAvailabilitySeed") == "true"
        if(expectSeed) assertTrue(dates.containsAll(setOf(LocalDate.of(2026,10,28),LocalDate.of(2026,10,29))))
        val seedSpots=mutableMapOf<Instant,Int>()
        assertTrue(dates.all { YearMonth.from(it)==month && !it.isBefore(LocalDate.now(zone)) })
        for (date in dates) {
            val queriedAt = Instant.now()
            val slots = getSlots(center,date).getOrThrow()
            slots.forEach { seedSpots[it.startsAt]=it.availableSpots }
            assertTrue(slots.all { it.centerId==center.id && it.availableSpots>0 &&
                it.startsAt>queriedAt && it.startsAt.atZone(zone).toLocalDate()==date })
        }
        // Exercita também RPC de horários quando o calendário real estiver vazio.
        val slots = getSlots(center,LocalDate.now(zone)).getOrThrow()
        assertTrue(slots.all { it.availableSpots>0 && it.centerId==center.id })
        if(expectSeed) {
            assertEquals(2,seedSpots[Instant.parse("2026-10-28T11:00:00Z")])
            assertEquals(3,seedSpots[Instant.parse("2026-10-28T13:00:00Z")])
            assertEquals(1,seedSpots[Instant.parse("2026-10-29T11:00:00Z")])
        }
        } finally { client.close() }
    }
}
