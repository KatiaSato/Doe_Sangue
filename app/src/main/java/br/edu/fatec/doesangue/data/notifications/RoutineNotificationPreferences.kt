package br.edu.fatec.doesangue.data.notifications

import android.content.SharedPreferences
import java.time.LocalTime
import java.time.LocalDate

internal data class RoutineNotificationPreference(
    val enabled: Boolean = false,
    val time: LocalTime = LocalTime.of(9, 0),
)

internal class RoutineNotificationPreferences(private val preferences: SharedPreferences) {
    fun read(userId: String): RoutineNotificationPreference {
        val minutes = preferences.getInt("time:$userId", 9 * 60).coerceIn(0, 1439)
        return RoutineNotificationPreference(
            enabled = preferences.getBoolean("enabled:$userId", false),
            time = LocalTime.of(minutes / 60, minutes % 60),
        )
    }

    fun write(userId: String, value: RoutineNotificationPreference) {
        preferences.edit()
            .putBoolean("enabled:$userId", value.enabled)
            .putInt("time:$userId", value.time.hour * 60 + value.time.minute)
            .apply()
    }

    var notificationOwner: String?
        get() = preferences.getString("notification_owner", null)
        set(value) { preferences.edit().putString("notification_owner", value).apply() }

    fun appointmentRemindersEnabled(userId: String): Boolean =
        preferences.getBoolean("appointment_enabled:$userId", false)

    fun setAppointmentRemindersEnabled(userId: String, enabled: Boolean) {
        preferences.edit().putBoolean("appointment_enabled:$userId", enabled).apply()
    }

    var appointmentNotificationOwner: String?
        get() = preferences.getString("appointment_notification_owner", null)
        set(value) { preferences.edit().putString("appointment_notification_owner", value).apply() }

    fun deliveredAppointments(userId: String): Set<String> =
        preferences.getStringSet("appointments_delivered:$userId", null).orEmpty()

    fun recordDeliveredAppointments(userId: String, values: Set<String>) {
        preferences.edit().putStringSet("appointments_delivered:$userId", values).apply()
    }

    fun wasDelivered(userId: String, routineId: String, date: LocalDate): Boolean =
        preferences.getString("delivered:$userId", null) == "$routineId:$date"

    fun recordDelivery(userId: String, routineId: String, date: LocalDate) {
        preferences.edit().putString("delivered:$userId", "$routineId:$date").apply()
    }

    companion object {
        const val FILE_NAME = "routine_notifications"
    }
}
