package br.edu.fatec.doesangue.data.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import br.edu.fatec.doesangue.MainActivity

private const val OPEN_ROUTINE = "br.edu.fatec.doesangue.action.OPEN_ROUTINE_REMINDER"
private const val OPEN_APPOINTMENT = "br.edu.fatec.doesangue.action.OPEN_APPOINTMENT_REMINDER"

data class NotificationOpenRequest(val ownerId: String, val appointmentId: String? = null)

internal fun routineNotificationPendingIntent(context: Context, ownerId: String): PendingIntent =
    reminderPendingIntent(context, NotificationOpenRequest(ownerId))

internal fun reminderPendingIntent(context: Context, request: NotificationOpenRequest): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        action = if (request.appointmentId == null) OPEN_ROUTINE else OPEN_APPOINTMENT
        data = Uri.Builder().scheme("br.edu.fatec.doesangue").authority("reminder")
            .appendPath(request.ownerId)
            .apply { request.appointmentId?.let { appendPath(it) } }
            .build()
    }
    return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
}

internal fun Intent.notificationOpenRequest(): NotificationOpenRequest? {
    val uri = data ?: return null
    if (uri.scheme != "br.edu.fatec.doesangue" || uri.host != "reminder") return null
    val segments = uri.pathSegments
    if (segments.any { it.isBlank() || it.length > 64 }) return null
    return when {
        action == OPEN_ROUTINE && segments.size == 1 -> NotificationOpenRequest(segments[0])
        action == OPEN_APPOINTMENT && segments.size == 2 -> NotificationOpenRequest(segments[0], segments[1])
        else -> null
    }
}
