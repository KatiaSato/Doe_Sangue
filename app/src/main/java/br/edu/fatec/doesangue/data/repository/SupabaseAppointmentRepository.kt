package br.edu.fatec.doesangue.data.repository

import br.edu.fatec.doesangue.data.supabase.mapper.toDataFailure

import br.edu.fatec.doesangue.data.supabase.mapper.toAppointmentFailure

import br.edu.fatec.doesangue.data.supabase.SupabaseAppointmentDataSource
import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
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
            Result.failure(exception.toAppointmentFailure())
        }
    }

    override suspend fun getMyAppointments(): Result<List<AppointmentSummary>> {
        return try {
            Result.success(dataSource.getMyAppointments())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception.toDataFailure())
        }
    }

    /*
    O repository entrega o resultado ao ViewModel como sucesso ou falha
    Preservamos CancellationException para respeitar a interrupção da corrotina
     */
    override suspend fun cancelAppointment(
        appointmentId: String,
    ): Result<Appointment> {
        return try {
            Result.success(
                dataSource.cancelAppointment(appointmentId)
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception.toAppointmentFailure())
        }
    }
}