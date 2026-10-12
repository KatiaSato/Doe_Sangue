package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.SupabaseAppointmentDataSource
import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import kotlin.coroutines.cancellation.CancellationException

class SupabaseAppointmentRepository(
    private val dataSource: SupabaseAppointmentDataSource,
) : AppointmentRepository {             //: AppointmentRepository indica que a classe cumpre o contrato do domínio

    override suspend fun createAppointment(
        timeSlotId: String,
        idempotencyKey: String,
    ): Result<Appointment> {
        return try {
            val appointment = dataSource.createAppointment(
                timeSlotId = timeSlotId,
                idempotencyKey = idempotencyKey,
            )
            Result.success(appointment)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}