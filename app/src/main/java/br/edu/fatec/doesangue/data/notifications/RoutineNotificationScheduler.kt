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
import br.edu.fatec.doesangue.domain.model.ReminderRoutine
import br.edu.fatec.doesangue.domain.model.SessionPresence
import br.edu.fatec.doesangue.domain.notifications.nextRoutineNotification
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.domain.repository.ReminderRoutineRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

internal class RoutineNotificationScheduler(
    private val context: Context,
    private val auth: AuthRepository,
    private val routines: ReminderRoutineRepository,
    private val preferences: RoutineNotificationPreferences,
    private val clock: Clock = Clock.systemUTC(),
    private val canUseSession: () -> Boolean = { true },
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val manager = NotificationManagerCompat.from(context)
    private val workManager by lazy { WorkManager.getInstance(context) }
    private val lock = Mutex()
    private val refreshes = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch {
            auth.observeSession()
                .combine(refreshes.receiveAsFlow().onStart { emit(Unit) }) { session, _ -> session }
                .collect { session ->
                    try {
                        lock.withLock { reconcile(session) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        Log.w(TAG, "Falha ao atualizar lembrete: ${failure.javaClass.simpleName}")
                    }
                }
        }
    }

    fun refresh() {
        refreshes.trySend(Unit)
    }

    private suspend fun reconcile(session: SessionPresence) {
        val latest = currentSession()
        if (latest != session || !canUseSession()) return
        when (session) {
            is SessionPresence.NotAuthenticated -> clear()
            is SessionPresence.Authenticated -> {
                val owner = session.user.id
                if (preferences.notificationOwner != owner) {
                    clear()
                    preferences.notificationOwner = owner
                }
                val preference = preferences.read(owner)
                if (!preference.enabled || !notificationsAllowed()) {
                    clear(keepOwner = true)
                    return
                }
                val result = loadRoutine(owner) ?: return
                if (result.isFailure || currentSession() != session || !canUseSession() ||
                    preference != preferences.read(owner)
                ) return
                val routine = result.getOrNull()
                if (routine == null || !routine.active || routine.ownerId != owner) {
                    clear(keepOwner = true)
                    return
                }
                replaceWork(routine, preference)
            }
            SessionPresence.Initializing, SessionPresence.RefreshPending -> Unit
        }
    }

    suspend fun deliver(workId: UUID, owner: String, occurrence: String, at: Instant): Boolean = lock.withLock {
        val session = currentSession()
        if (session == null || session == SessionPresence.RefreshPending || !canUseSession()) return false
        if ((session as? SessionPresence.Authenticated)?.user?.id != owner) return true
        val preference = preferences.read(owner)
        if (!preference.enabled || !notificationsAllowed()) return true
        val result = loadRoutine(owner) ?: return false
        if (result.isFailure) return false
        val routine = result.getOrNull()?.takeIf { it.active && it.ownerId == owner } ?: return true
        if (currentSession() != session || !canUseSession()) return false
        if (preference != preferences.read(owner) || !notificationsAllowed()) return true

        val scheduled = nextRoutineNotification(routine, preference.time, Clock.fixed(at, clock.zone))
        val now = clock.instant()
        if (scheduled?.toInstant() == at && occurrence == workName(routine, scheduled)) {
            val date = scheduled.toLocalDate()
            val today = now.atZone(scheduled.zone).toLocalDate()
            if (now < at) return false
            if (today == date && !preferences.wasDelivered(owner, routine.id, date)) {
                currentCoroutineContext().ensureActive()
                if (publish(owner)) {
                    preferences.recordDelivery(owner, routine.id, date)
                } else {
                    return true
                }
            }
        }
        replaceWork(routine, preference, runningId = workId)
        true
    }

    private suspend fun replaceWork(
        routine: ReminderRoutine,
        preference: RoutineNotificationPreference,
        runningId: UUID? = null,
    ) {
        val pending = workManager.getWorkInfosByTagFlow(TAG).first()
            .filter { !it.state.isFinished && it.id != runningId }
        val zone = ZoneId.of(routine.timeZoneId)
        val today = clock.instant().atZone(zone).toLocalDate()
        val todayAt = today.atTime(preference.time).atZone(zone)
        val deliveredToday = preferences.wasDelivered(routine.ownerId, routine.id, today)
        val keepDueToday = !deliveredToday && routine.nextDate(today) == today &&
            pending.any { workName(routine, todayAt) in it.tags }
        val scheduleClock = if (deliveredToday) Clock.fixed(today.plusDays(1).atStartOfDay(zone).toInstant(), zone) else clock
        val next = if (keepDueToday) todayAt else nextRoutineNotification(routine, preference.time, scheduleClock)
        val name = next?.let { workName(routine, it) }
        pending.filter { name == null || name !in it.tags }.forEach {
            workManager.cancelWorkById(it.id).await()
        }
        if (next == null || name == null || pending.any { name in it.tags }) return
        val delay = Duration.between(clock.instant(), next.toInstant()).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<RoutineNotificationWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(OWNER to routine.ownerId, OCCURRENCE to name, AT to next.toInstant().toEpochMilli()))
            .addTag(TAG)
            .addTag(name)
            .build()
        workManager.enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request).await()
    }

    private fun workName(routine: ReminderRoutine, time: ZonedDateTime): String =
        "$TAG:${routine.ownerId}:${routine.id}:${routine.version}:${time.toInstant().toEpochMilli()}"

    private suspend fun loadRoutine(owner: String) = withTimeoutOrNull(15_000) { routines.get(owner) }

    private suspend fun currentSession(): SessionPresence? = withTimeoutOrNull(15_000) {
        auth.observeSession().first { it != SessionPresence.Initializing }
    }

    private suspend fun clear(keepOwner: Boolean = false) {
        manager.cancel(NOTIFICATION_ID)
        workManager.cancelAllWorkByTag(TAG).await()
        if (!keepOwner) preferences.notificationOwner = null
    }

    private fun notificationsAllowed(): Boolean =
        manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 26 || manager.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)

    @SuppressLint("MissingPermission")
    private fun publish(owner: String): Boolean {
        if (!notificationsAllowed()) return false
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL, "Lembretes da rotina", NotificationManager.IMPORTANCE_DEFAULT,
            ))
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Doe Sangue")
            .setContentText("Você tem um lembrete da sua rotina.")
            .setContentIntent(routineNotificationPendingIntent(context, owner))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .build()
        return try {
            manager.notify(NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        private const val TAG = "RoutineNotification"
        private const val CHANNEL = "routine_reminders"
        private const val NOTIFICATION_ID = 15
        const val OWNER = "owner"
        const val OCCURRENCE = "occurrence"
        const val AT = "at"
    }
}
