package br.edu.fatec.doesangue.ui.calendar

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.ui.components.SecondaryButton
import java.time.Instant

internal fun openAppointmentCalendar(
    startsAt: Instant,
    unit: String,
    startActivity: (Intent) -> Unit,
): Boolean {
    val event = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startsAt.toEpochMilli())
        putExtra(CalendarContract.Events.TITLE, "Agendamento Doe Sangue")
        putExtra(CalendarContract.Events.EVENT_LOCATION, unit)
    }
    return try {
        startActivity(event)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

@Composable
internal fun AppointmentCalendarButton(
    status: AppointmentStatus,
    startsAt: Instant?,
    unit: String?,
    enabled: Boolean = true,
) {
    if (status != AppointmentStatus.SCHEDULED || startsAt == null || startsAt <= Instant.now() || unit.isNullOrBlank()) return

    val context = LocalContext.current
    SecondaryButton(
        "Adicionar ao calendário",
        enabled = enabled,
        onClick = {
            val message = when {
                startsAt <= Instant.now() -> "O horário deste agendamento já passou."
                !openAppointmentCalendar(startsAt, unit, context::startActivity) ->
                    "Não encontramos um app de calendário disponível neste aparelho."
                else -> null
            }
            if (message != null) Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        },
    )
}
