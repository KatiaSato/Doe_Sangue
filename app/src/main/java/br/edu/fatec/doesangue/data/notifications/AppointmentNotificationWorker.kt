package br.edu.fatec.doesangue.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.edu.fatec.doesangue.DoeSangueApplication
import java.time.Instant
import java.time.format.DateTimeParseException

class AppointmentNotificationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val owner = inputData.getString(AppointmentNotificationScheduler.OWNER) ?: return Result.failure()
        val appointment = inputData.getString(AppointmentNotificationScheduler.APPOINTMENT) ?: return Result.failure()
        val at = readInstant(AppointmentNotificationScheduler.AT) ?: return Result.failure()
        val start = readInstant(AppointmentNotificationScheduler.START) ?: return Result.failure()
        val app = applicationContext as DoeSangueApplication
        return if (app.appointmentNotifications.deliver(owner, appointment, at, start)) Result.success() else Result.retry()
    }

    private fun readInstant(key: String): Instant? = try {
        inputData.getString(key)?.let(Instant::parse)
    } catch (_: DateTimeParseException) {
        null
    }
}
