package br.edu.fatec.doesangue.presentation.notifications

import android.content.Context
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreference
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreferences
import br.edu.fatec.doesangue.domain.model.ReminderRoutine
import br.edu.fatec.doesangue.presentation.routines.ReminderRoutineUiState
import br.edu.fatec.doesangue.ui.screens.profile.RoutineNotificationsScreen
import java.time.LocalTime
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineNotificationsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun appointmentPreferenceIsIndependentAndNeedsPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = "test_notification_preferences_${UUID.randomUUID()}"
        val storage = context.getSharedPreferences(file, Context.MODE_PRIVATE)
        val preferences = RoutineNotificationPreferences(storage)
        try {
            val model = RoutineNotificationsViewModel(preferences, "owner-a")
            model.setAppointmentEnabled(true, permissionGranted = false)
            assertFalse(model.appointmentReminders.value)
            model.setAppointmentEnabled(true, permissionGranted = true)
            assertTrue(RoutineNotificationsViewModel(preferences, "owner-a").appointmentReminders.value)
            assertFalse(preferences.appointmentRemindersEnabled("owner-b"))
            assertFalse(model.uiState.value.enabled)
            model.setAppointmentEnabled(false, permissionGranted = false)
            assertFalse(preferences.appointmentRemindersEnabled("owner-a"))
        } finally {
            storage.edit().clear().commit()
        }
    }

    @Test
    fun storesTimeAndOptInByAccountAndRejectsActivationWithoutPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = "test_notification_preferences_${UUID.randomUUID()}"
        val storage = context.getSharedPreferences(file, Context.MODE_PRIVATE)
        try {
            val model = RoutineNotificationsViewModel(RoutineNotificationPreferences(storage), "owner-a")
            val time = LocalTime.of(17, 45)
            assertEquals(RoutineNotificationPreference(), model.uiState.value)

            model.setTime(time)
            model.setEnabled(true, permissionGranted = false)
            val readBack = RoutineNotificationPreferences(storage)
            assertEquals(RoutineNotificationPreference(time = time), readBack.read("owner-a"))

            model.setEnabled(true, permissionGranted = true)
            assertEquals(RoutineNotificationPreference(enabled = true, time = time), readBack.read("owner-a"))
            assertEquals(RoutineNotificationPreference(), readBack.read("owner-b"))

            model.setEnabled(false, permissionGranted = false)
            assertEquals(RoutineNotificationPreference(time = time), readBack.read("owner-a"))
        } finally {
            storage.edit().clear().commit()
        }
    }

    @Test
    fun blockedPermissionShowsSettingsAndLeavesSwitchOff() {
        var settingsOpened = false
        var requestedActivation = false
        compose.setContent {
            RoutineNotificationsScreen(
                preference = RoutineNotificationPreference(),
                routineState = ReminderRoutineUiState(loading = false, loaded = true),
                showPermissionHint = true,
                onToggle = { requestedActivation = it },
                onChooseTime = {},
                onSeeRoutine = {},
                onOpenSettings = { settingsOpened = true },
                onBack = {},
            )
        }

        val toggle = compose.onNodeWithContentDescription("Lembretes da rotina")
        toggle.assertIsOff().performClick()
        compose.onNodeWithText("Abrir configurações do Android").performClick()
        compose.runOnIdle {
            assertTrue(requestedActivation)
            assertTrue(settingsOpened)
            requestedActivation = false
        }
        toggle.assertIsOff()
        compose.onNodeWithText("Configure uma rotina em Lembretes para receber avisos.").assertExists()
        compose.runOnIdle { assertFalse(requestedActivation) }
    }

    @Test
    fun explainsWhichDateAndAllowsOpeningTheRoutine() {
        var routineOpened = false
        val routine = ReminderRoutine(
            "routine", "owner", LocalDate.of(2099, 6, 18), 30, "America/Sao_Paulo", true, 1,
        )
        compose.setContent {
            RoutineNotificationsScreen(
                preference = RoutineNotificationPreference(enabled = true, time = LocalTime.of(17, 45)),
                routineState = ReminderRoutineUiState(loading = false, loaded = true, routine = routine),
                showPermissionHint = false,
                onToggle = {},
                onChooseTime = {},
                onSeeRoutine = { routineOpened = true },
                onOpenSettings = {},
                onBack = {},
            )
        }
        compose.onNodeWithText("Próxima data da rotina: 18/06/2099").assertExists()
        compose.onNodeWithText("Horário do lembrete da rotina").assertExists()
        compose.onNodeWithText("17:45").assertExists()
        compose.onNodeWithText("Ver minha rotina").performClick()
        compose.runOnIdle { assertTrue(routineOpened) }
    }
}
