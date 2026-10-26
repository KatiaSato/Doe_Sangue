package br.edu.fatec.doesangue.domain.usecase

import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.repository.DonationTimeSlotRepository
import java.time.Clock
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class GetAvailableDonationDatesUseCase(
    private val repository: DonationTimeSlotRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend operator fun invoke(center: DonationCenter, month: YearMonth): Result<Set<LocalDate>> = try {
        val today = LocalDate.now(clock.withZone(ZoneId.of(center.timeZoneId)))
        repository.getAvailableDates(center.id, month).map { dates ->
            dates.filter { YearMonth.from(it) == month && !it.isBefore(today) }.toSet()
        }
    } catch (exception: DateTimeException) {
        Result.failure(exception)
    }
}
