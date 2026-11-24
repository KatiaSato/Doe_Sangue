package br.edu.fatec.doesangue.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.edu.fatec.doesangue.DoeSangueApplication
import java.time.Instant

class RoutineNotificationWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val owner = inputData.getString(RoutineNotificationScheduler.OWNER) ?: return Result.failure()
        val occurrence = inputData.getString(RoutineNotificationScheduler.OCCURRENCE) ?: return Result.failure()
        val at = inputData.getLong(RoutineNotificationScheduler.AT, Long.MIN_VALUE)
        if (at == Long.MIN_VALUE) return Result.failure()
        val app = applicationContext as DoeSangueApplication
        return if (app.routineNotifications.deliver(id, owner, occurrence, Instant.ofEpochMilli(at))) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
