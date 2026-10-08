package br.edu.fatec.doesangue.domain.usecase

import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.model.DonationTimeSlot
import br.edu.fatec.doesangue.domain.repository.DonationTimeSlotRepository
import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZoneId

class GetDonationTimeSlotsUseCase(
    private val repository: DonationTimeSlotRepository,
) {
    suspend operator fun invoke(
        center: DonationCenter,
        date: LocalDate,
    ): Result<List<DonationTimeSlot>> {
        return try {
            val zone = ZoneId.of(center.timeZoneId) // interpreta o identificador de fuso recebido da unidade

            val fromInclusive = date
                .atStartOfDay(zone)                 // encontra o início do dia naquele fuso
                .toInstant()                        // transforma esse início em um instante preciso

            val untilExclusive = date
                .plusDays(1)            // permite calcular o início do dia seguinte, que será o limite excluído da consulta
                .atStartOfDay(zone)
                .toInstant()

            repository.getTimeSlots(
                centerId = center.id,
                fromInclusive = fromInclusive,
                untilExclusive = untilExclusive,
            )
        } catch (error: DateTimeException) {        // O catch transforma um problema de data ou fuso em Result.failure. Ele captura especificamente DateTimeException, preservando o cancelamento das corrotinas
            Result.failure(error)
        }
    }
}