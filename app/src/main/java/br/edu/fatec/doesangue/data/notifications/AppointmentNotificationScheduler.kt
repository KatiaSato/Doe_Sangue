package br.edu.fatec.doesangue.data.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import br.edu.fatec.doesangue.domain.model.SessionPresence
import br.edu.fatec.doesangue.domain.notifications.appointmentReminderTime
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

internal class AppointmentNotificationScheduler(
    private val context: Context,
    private val auth: AuthRepository,
    private val appointments: AppointmentRepository,
    private val preferences: RoutineNotificationPreferences,
    private val canUseSession: () -> Boolean,
    private val clock: Clock = Clock.systemUTC(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val manager = NotificationManagerCompat.from(context)
    private val platformManager = context.getSystemService(NotificationManager::class.java)
    private val workManager by lazy { WorkManager.getInstance(context) }
    private val lock = Mutex()
    private val refreshes = Channel<Unit>(Channel.CONFLATED)

    init {
        refresh()
        scope.launch {
            auth.observeSession().combine(refreshes.receiveAsFlow()) { session, _ -> session }.collect { session ->
                try {
                    lock.withLock { reconcile(session) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Log.w(TAG, "Falha ao atualizar aviso: ${failure.javaClass.simpleName}")
                }
            }
        }
    }

    fun refresh() {
        refreshes.trySend(Unit)
    }

    private suspend fun reconcile(session: SessionPresence) {
        if (session == SessionPresence.Initializing || session == SessionPresence.RefreshPending ||
            !canUseSession() || currentSession() != session
        ) return

        val owner = (session as? SessionPresence.Authenticated)?.user?.id
        if (preferences.appointmentNotificationOwner != owner) {
            clear()
            preferences.appointmentNotificationOwner = owner
        }
        if (owner == null || !enabled(owner)) {
            clear()
            return
        }

        val records = loadAppointments() ?: return
        if (currentSession() != session || !canUseSession() || !enabled(owner)) return
        val now = clock.instant()
        val upcoming = records.filter { it.appointment.donorId == owner && it.startsAt > now }
            .mapNotNull { summary -> summary.appointmentReminderTime()?.let { summary to it } }
            .associateBy { (summary, at) -> workName(summary, at) }
        val delivered = preferences.deliveredAppointments(owner).intersect(upcoming.keys)
        preferences.recordDeliveredAppointments(owner, delivered)

        val pending = workManager.getWorkInfosByTagFlow(TAG).first().filterNot { it.state.isFinished }
        pending.filter { job -> upcoming.keys.none { it in job.tags } }.forEach {
            workManager.cancelWorkById(it.id).await()
        }
        removePostedExcept(upcoming.keys)
        for ((name, occurrence) in upcoming) {
            val (summary, at) = occurrence
            if (name !in delivered && at >= now && pending.none { name in it.tags }) {
                enqueue(summary, at, name)
            }
        }
    }

    suspend fun deliver(owner: String, appointmentId: String, reminderAt: Instant, startsAt: Instant): Boolean =
        lock.withLock {
            val now = clock.instant()
            if (now >= startsAt) return true
            val session = currentSession()
            if (session == null || session == SessionPresence.RefreshPending || !canUseSession()) return false
            if ((session as? SessionPresence.Authenticated)?.user?.id != owner || !enabled(owner)) return true
            val records = loadAppointments() ?: return false
            val summary = records.firstOrNull { it.appointment.id == appointmentId && it.appointment.donorId == owner }
                ?: return true
            if (summary.startsAt != startsAt || summary.appointmentReminderTime() != reminderAt) return true
            if (currentSession() != session || !canUseSession()) return false
            if (!enabled(owner)) return true

            val confirmedNow = clock.instant()
            if (confirmedNow < reminderAt) return false
            if (confirmedNow >= summary.startsAt) return true
            val name = workName(summary, reminderAt)
            val delivered = preferences.deliveredAppointments(owner)
            if (name !in delivered) {
                currentCoroutineContext().ensureActive()
                if (publish(name, NotificationOpenRequest(owner, appointmentId))) {
                    preferences.recordDeliveredAppointments(owner, delivered + name)
                }
            }
            true
        }

    private suspend fun enqueue(summary: AppointmentSummary, at: Instant, name: String) {
        val delay = Duration.between(clock.instant(), at).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<AppointmentNotificationWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(
                OWNER to summary.appointment.donorId,
                APPOINTMENT to summary.appointment.id,
                AT to at.toString(),
                START to summary.startsAt.toString(),
            ))
            .addTag(TAG)
            .addTag(name)
            .build()
        workManager.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request).await()
    }

    private fun workName(summary: AppointmentSummary, at: Instant): String =
        "$TAG:${summary.appointment.donorId}:${summary.appointment.id}:${at.toEpochMilli()}"

    private suspend fun currentSession(): SessionPresence? =
        withTimeoutOrNull(15_000) { auth.observeSession().first { it != SessionPresence.Initializing } }

    private suspend fun loadAppointments(): List<AppointmentSummary>? =
        withTimeoutOrNull(15_000) { appointments.getMyAppointments().getOrNull() }

    private suspend fun clear() {
        removePostedExcept(emptySet())
        workManager.cancelAllWorkByTag(TAG).await()
    }

    private fun removePostedExcept(names: Set<String>) {
        platformManager.activeNotifications
            .filter { it.tag?.startsWith("$TAG:") == true && it.tag !in names }
            .forEach { manager.cancel(it.tag, it.id) }
    }

    private fun enabled(owner: String): Boolean =
        preferences.appointmentRemindersEnabled(owner) && manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)

    @SuppressLint("MissingPermission")
    private fun publish(name: String, request: NotificationOpenRequest): Boolean {
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL, "Avisos de agendamentos", NotificationManager.IMPORTANCE_DEFAULT,
            ))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Doe Sangue")
            .setContentText("Você tem um agendamento nas próximas 24 horas.")
            .setContentIntent(reminderPendingIntent(context, request))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(name, NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val TAG = "AppointmentNotification"
        const val OWNER = "owner"
        const val APPOINTMENT = "appointment"
        const val AT = "at"
        const val START = "starts_at"
        private const val CHANNEL = "appointments"
        private const val NOTIFICATION_ID = 16
    }
}
