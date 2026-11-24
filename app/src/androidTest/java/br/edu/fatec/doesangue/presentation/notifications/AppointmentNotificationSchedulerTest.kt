package br.edu.fatec.doesangue.presentation.notifications

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.await
import br.edu.fatec.doesangue.data.notifications.AppointmentNotificationScheduler
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreferences
import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.domain.model.SessionPresence
import br.edu.fatec.doesangue.domain.repository.AppointmentRepository
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.ui.navigation.FakeAppointmentRepository
import br.edu.fatec.doesangue.ui.navigation.FakeAuthRepository
import java.time.Clock
import java.time.Instant
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentNotificationSchedulerTest {
    @Test
    fun queuesOnlyEligibleReservationsAndClearsThemWhenPreferenceIsDisabled() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = context.getSharedPreferences("appointment_test_${UUID.randomUUID()}", Context.MODE_PRIVATE)
        val preferences = RoutineNotificationPreferences(storage)
        val owner = AuthenticatedUser("appointment-owner")
        val presence = MutableStateFlow<SessionPresence>(SessionPresence.Authenticated(owner))
        val auth = object : AuthRepository by FakeAuthRepository() {
            override fun observeSession() = presence
        }
        val now = Instant.parse("2030-01-02T08:00:00Z")
        val start = Instant.parse("2030-01-04T09:00:00Z")
        val eligible = AppointmentSummary(
            Appointment("eligible", owner.id, "slot", AppointmentStatus.SCHEDULED, now),
            "Unidade fictícia", start, "UTC",
        )
        val records = MutableStateFlow(listOf(
            eligible,
            eligible.copy(appointment = eligible.appointment.copy(id = "short-notice"), startsAt = now.plusSeconds(60)),
            eligible.copy(appointment = eligible.appointment.copy(id = "cancelled", status = AppointmentStatus.CANCELLED)),
            eligible.copy(appointment = eligible.appointment.copy(id = "another-account", donorId = "other")),
        ))
        val repository = object : AppointmentRepository by FakeAppointmentRepository() {
            override suspend fun getMyAppointments() = Result.success(records.value)
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = WorkManager.getInstance(context)
        val work = manager.getWorkInfosByTagFlow(AppointmentNotificationScheduler.TAG)
        preferences.setAppointmentRemindersEnabled(owner.id, true)
        val scheduler = AppointmentNotificationScheduler(
            context, auth, repository, preferences, { true }, Clock.fixed(now, ZoneOffset.UTC), scope,
        )
        try {
            val queued = withTimeout(15_000) { work.first { jobs -> jobs.any { !it.state.isFinished } } }
            val expected = "AppointmentNotification:${owner.id}:eligible:${Instant.parse("2030-01-03T09:00:00Z").toEpochMilli()}"
            assertTrue(expected in queued.filterNot { it.state.isFinished }.single().tags)
            val replacement = eligible.copy(
                appointment = eligible.appointment.copy(id = "replacement"),
                startsAt = start.plusSeconds(3600),
            )
            records.value = listOf(
                eligible.copy(appointment = eligible.appointment.copy(status = AppointmentStatus.CANCELLED)),
                replacement,
            )
            scheduler.refresh()
            withTimeout(15_000) {
                work.first { jobs ->
                    val active = jobs.filterNot { it.state.isFinished }
                    active.size == 1 && active.single().tags.any { ":replacement:" in it }
                }
            }
            records.value = listOf(replacement.copy(appointment = replacement.appointment.copy(status = AppointmentStatus.CANCELLED)))
            scheduler.refresh()
            withTimeout(15_000) { work.first { jobs -> jobs.none { !it.state.isFinished } } }
            records.value = listOf(replacement)
            scheduler.refresh()
            withTimeout(15_000) { work.first { jobs -> jobs.count { !it.state.isFinished } == 1 } }
            preferences.setAppointmentRemindersEnabled(owner.id, false)
            scheduler.refresh()
            withTimeout(15_000) { work.first { jobs -> jobs.none { !it.state.isFinished } } }
        } finally {
            scope.cancel()
            manager.cancelAllWorkByTag(AppointmentNotificationScheduler.TAG).await()
            storage.edit().clear().commit()
        }
    }
}
