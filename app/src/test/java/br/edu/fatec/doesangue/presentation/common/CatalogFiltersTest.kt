package br.edu.fatec.doesangue.presentation.common

import br.edu.fatec.doesangue.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class CatalogFiltersTest {
    private val now = Instant.parse("2026-10-29T12:00:00Z")
    private val center = DonationCenter("1", "Unidade Escola", "Paulínia", "Endereço fictício", "", "", "America/Sao_Paulo")
    private fun appointment(id: String, status: AppointmentStatus = AppointmentStatus.SCHEDULED, date: Instant = now.plusSeconds(100)) =
        AppointmentSummary(Appointment(id, "sintetico", "horario", status, now), center.name, date, center.timeZoneId, center.id)
    private fun stock(type: BloodType, level: StockLevel, id: String = "1") =
        BloodStock(id, if (id == "1") center.name else "Unidade Azul", type, level, "Fonte fictícia", now, center.timeZoneId)

    @Test fun nameAndCityIgnoreCaseAccentsAndSurroundingSpaces() {
        assertTrue(matchesNameOrCity(" PAULINIA ", center.name, center.city))
        assertTrue(matchesNameOrCity("escola", center.name))
        assertTrue(matchesNameOrCity("  ", center.name))
        assertFalse(matchesNameOrCity("Paulínia", center.name))
    }
    @Test fun rescheduledOriginalIsSeparateFromCancelledAndReplacementIsScheduled() {
        val cancelled = appointment("cancel", AppointmentStatus.CANCELLED)
        val original = cancelled.copy(appointment = cancelled.appointment.copy(id = "old"), rescheduledToId = "new")
        val replacement = appointment("new").copy(rescheduledFromId = "old")
        val data = listOf(cancelled, original, replacement)
        assertEquals(listOf(cancelled), data.filter(AppointmentListFilter.CANCELLED::matches))
        assertEquals(listOf(original), data.filter(AppointmentListFilter.RESCHEDULED::matches))
        assertEquals(listOf(replacement), data.filter(AppointmentListFilter.SCHEDULED::matches))
        assertEquals(data, data.filter(AppointmentListFilter.ALL::matches))
    }
    @Test fun missingOrHiddenCenterDoesNotHideOwnAppointmentAndCityCanBeCombinedWithState() {
        val a = appointment("a")
        val old = a.copy(appointment = a.appointment.copy(id = "cancel", status = AppointmentStatus.CANCELLED))
        assertEquals(listOf(a), filterAppointments(listOf(a), emptyList(), "", AppointmentListFilter.ALL, now))
        assertEquals(listOf(a), filterAppointments(listOf(a), emptyList(), "ESCOLA", AppointmentListFilter.ALL, now))
        assertEquals(listOf(old), filterAppointments(listOf(a, old), listOf(center), "paulinia", AppointmentListFilter.CANCELLED, now))
        assertTrue(filterAppointments(listOf(a), emptyList(), "paulinia", AppointmentListFilter.ALL, now).isEmpty())
        assertNull(a.centerId?.let { emptyList<DonationCenter>().find { c -> c.id == it } })
    }
    @Test fun upcomingAreAscendingThenPreviousAreDescendingAndCancelledFutureIsNotUpcoming() {
        val early = appointment("early", date = now.plusSeconds(100))
        val later = appointment("later", date = now.plusSeconds(200))
        val past = appointment("past", date = now.minusSeconds(100))
        val boundary = appointment("boundary", date = now)
        val cancelled = appointment("cancelled", AppointmentStatus.CANCELLED, now.plusSeconds(300))
        val output = filterAppointments(listOf(past, later, cancelled, boundary, early), emptyList(), "", AppointmentListFilter.ALL, now)
        assertEquals(listOf("early", "later", "cancelled", "boundary", "past"), output.map { it.appointment.id })
        assertFalse(boundary.isUpcoming(now))
    }
    @Test fun stockFiltersUseSameRecordForTypeAndLevelAndKeepUnitsSeparate() {
        val data = listOf(stock(BloodType.O_NEGATIVE, StockLevel.CRITICAL), stock(BloodType.O_POSITIVE, StockLevel.STABLE),
            stock(BloodType.A_POSITIVE, StockLevel.CRITICAL, "2"))
        assertTrue(filterStockUnits(data, listOf(center), "", StockLevel.CRITICAL, BloodType.O_POSITIVE).isEmpty())
        val result = filterStockUnits(data, listOf(center), "paulinia", StockLevel.CRITICAL, BloodType.O_NEGATIVE)
        assertEquals(1, result.size); assertEquals(listOf(data[0]), result.single().records)
        assertEquals("Paulínia", result.single().city)
        assertEquals(2, filterStockUnits(data, emptyList(), "", null, null).size)
        assertEquals(2, filterStockUnits(data, emptyList(), "", StockLevel.CRITICAL, null).size)
    }
    @Test fun allEightTypesCanBeSelectedWithoutUsingUnknownOrCompatibilityRules() {
        val data = BloodType.knownTypes.map { stock(it, StockLevel.ALERT) }
        BloodType.knownTypes.forEach { type ->
            assertEquals(listOf(type), filterStockUnits(data, emptyList(), "", StockLevel.ALERT, type).single().records.map { it.bloodType })
        }
        assertTrue(filterStockUnits(data, emptyList(), "", null, BloodType.UNKNOWN).isEmpty())
        assertEquals(8, filterStockUnits(data, emptyList(), "", null, null).single().records.size)
    }
    @Test fun metadataAndMissingCitiesArePreservedAndRowsAreNotInvented() {
        val first = stock(BloodType.O_NEGATIVE, StockLevel.CRITICAL)
        val other = stock(BloodType.A_POSITIVE, StockLevel.CRITICAL).copy(source = "Outra fonte", updatedAt = now.minusSeconds(300))
        val records = filterStockUnits(listOf(other, first), emptyList(), "escola", null, null).single()
        assertNull(records.city)
        assertEquals(setOf("Fonte fictícia", "Outra fonte"), records.records.map { it.source }.toSet())
        assertEquals(setOf(now, now.minusSeconds(300)), records.records.map { it.updatedAt }.toSet())
        assertTrue(filterStockUnits(emptyList(), listOf(center), "", null, null).isEmpty())
    }
}
