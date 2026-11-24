package br.edu.fatec.doesangue.presentation.notifications

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreference
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreferences
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationScheduler
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.ReminderRoutine
import br.edu.fatec.doesangue.domain.model.SessionPresence
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.domain.repository.ReminderRoutineRepository
import br.edu.fatec.doesangue.ui.navigation.FakeAuthRepository
import br.edu.fatec.doesangue.ui.navigation.FakeReminderRoutineRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineNotificationSchedulerTest {
    @Test
    fun replacesEditedWorkSkipsExpiredOccurrenceAndCancelsOnPauseAndLogout() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = context.getSharedPreferences("scheduler_test_${UUID.randomUUID()}", Context.MODE_PRIVATE)
        val preferences = RoutineNotificationPreferences(storage)
        val presence = MutableStateFlow<SessionPresence>(SessionPresence.Authenticated(AuthenticatedUser("owner")))
        val auth = object : AuthRepository by FakeAuthRepository() {
            override fun observeSession() = presence
        }
        val routine = ReminderRoutine("routine", "owner", LocalDate.parse("2030-01-01"), 1, "UTC", true, 1)
        val current = MutableStateFlow(routine)
        val repository = object : ReminderRoutineRepository by FakeReminderRoutineRepository() {
            override suspend fun get(ownerId: String) = Result.success(current.value)
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = WorkManager.getInstance(context)
        val work = manager.getWorkInfosByTagFlow("RoutineNotification")
        val now = Instant.parse("2030-01-02T08:00:00Z")
        val time = LocalTime.of(9, 0)
        preferences.write("owner", RoutineNotificationPreference(true, time))
        val scheduler = RoutineNotificationScheduler(
            context, auth, repository, preferences, Clock.fixed(now, ZoneOffset.UTC), scope = scope,
        )
        try {
            val original = withTimeout(15_000) { work.first { it.pending().size == 1 }.pending().single() }
            current.value = routine.copy(version = 2)
            scheduler.refresh()
            withTimeout(15_000) {
                work.first { jobs -> jobs.pending().singleOrNull()?.id?.let { it != original.id } == true }
            }

            val expiredAt = Instant.parse("2030-01-01T09:00:00Z")
            val expiredName = "RoutineNotification:owner:routine:2:${expiredAt.toEpochMilli()}"
            assertTrue(scheduler.deliver(UUID.randomUUID(), "owner", expiredName, expiredAt))
            assertFalse(preferences.wasDelivered("owner", "routine", LocalDate.parse("2030-01-01")))

            current.value = current.value.copy(active = false, version = 3)
            scheduler.refresh()
            withTimeout(15_000) { work.first { it.pending().isEmpty() } }
            current.value = current.value.copy(active = true, version = 4)
            scheduler.refresh()
            withTimeout(15_000) { work.first { it.pending().size == 1 } }
            presence.value = SessionPresence.NotAuthenticated(explicitSignOut = true)
            withTimeout(15_000) { work.first { it.pending().isEmpty() } }
        } finally {
            scope.cancel()
            manager.cancelAllWorkByTag("RoutineNotification")
            storage.edit().clear().commit()
        }
        Unit
    }

    private fun List<WorkInfo>.pending() = filterNot { it.state.isFinished }
}
