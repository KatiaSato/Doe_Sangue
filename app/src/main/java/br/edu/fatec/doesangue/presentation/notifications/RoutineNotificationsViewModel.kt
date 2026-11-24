package br.edu.fatec.doesangue.presentation.notifications

import androidx.lifecycle.ViewModel
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreference
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreferences
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RoutineNotificationsViewModel(
    private val preferences: RoutineNotificationPreferences,
    private val userId: String,
    private val onPreferenceChanged: () -> Unit = {},
) : ViewModel() {
    private val state = MutableStateFlow(preferences.read(userId))
    val uiState = state.asStateFlow()
    private val appointmentEnabled = MutableStateFlow(preferences.appointmentRemindersEnabled(userId))
    val appointmentReminders = appointmentEnabled.asStateFlow()

    fun setEnabled(enabled: Boolean, permissionGranted: Boolean) {
        if (enabled && !permissionGranted) return
        save(state.value.copy(enabled = enabled))
    }

    fun setTime(time: LocalTime) {
        save(state.value.copy(time = time))
    }

    fun setAppointmentEnabled(enabled: Boolean, permissionGranted: Boolean) {
        if ((enabled && !permissionGranted) || enabled == appointmentEnabled.value) return
        preferences.setAppointmentRemindersEnabled(userId, enabled)
        appointmentEnabled.value = enabled
        onPreferenceChanged()
    }

    private fun save(value: RoutineNotificationPreference) {
        if (value == state.value) return
        preferences.write(userId, value)
        state.value = value
        onPreferenceChanged()
    }
}
