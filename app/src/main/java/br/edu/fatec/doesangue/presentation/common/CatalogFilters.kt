package br.edu.fatec.doesangue.presentation.common

import br.edu.fatec.doesangue.domain.model.*
import java.text.Normalizer
import java.time.Instant
import java.util.Locale

fun normalizedSearch(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()

fun matchesNameOrCity(query: String, name: String, city: String? = null): Boolean {
    val needle = normalizedSearch(query)
    return needle.isEmpty() || normalizedSearch(name).contains(needle) ||
        (city != null && normalizedSearch(city).contains(needle))
}

enum class AppointmentListFilter(val label: String) {
    ALL("Todos"), SCHEDULED("Agendados"), CANCELLED("Cancelados"), RESCHEDULED("Remarcados");

    fun matches(summary: AppointmentSummary): Boolean = when (this) {
        ALL -> true
        SCHEDULED -> summary.appointment.status == AppointmentStatus.SCHEDULED
        CANCELLED -> summary.appointment.status == AppointmentStatus.CANCELLED && summary.rescheduledToId == null
        RESCHEDULED -> summary.appointment.status == AppointmentStatus.CANCELLED && summary.rescheduledToId != null
    }
}

fun AppointmentSummary.isUpcoming(now: Instant): Boolean =
    appointment.status == AppointmentStatus.SCHEDULED && startsAt > now

fun filterAppointments(
    appointments: List<AppointmentSummary>, centers: List<DonationCenter>, query: String,
    filter: AppointmentListFilter, now: Instant,
): List<AppointmentSummary> {
    val cities = centers.associate { it.id to it.city }
    return appointments.filter { filter.matches(it) && matchesNameOrCity(query, it.centerName, cities[it.centerId]) }
        .sortedWith(compareBy<AppointmentSummary> { !it.isUpcoming(now) }
            .thenComparator { a, b -> if (a.isUpcoming(now)) a.startsAt.compareTo(b.startsAt) else b.startsAt.compareTo(a.startsAt) }
            .thenBy { it.appointment.id })
}

data class StockUnitGroup(val centerId: String, val centerName: String, val city: String?, val records: List<BloodStock>)

fun filterStockUnits(
    stocks: List<BloodStock>, centers: List<DonationCenter>, query: String,
    level: StockLevel?, bloodType: BloodType?,
): List<StockUnitGroup> {
    val cities = centers.associate { it.id to it.city }
    return stocks.filter { (level == null || it.level == level) && (bloodType == null || it.bloodType == bloodType) &&
        matchesNameOrCity(query, it.centerName, cities[it.centerId]) }
        .groupBy { it.centerId }.map { (id, records) ->
            StockUnitGroup(id, records.first().centerName, cities[id], records.sortedBy { it.bloodType.ordinal })
        }.sortedWith(compareBy<StockUnitGroup> { normalizedSearch(it.centerName) }.thenBy { it.centerId })
}
