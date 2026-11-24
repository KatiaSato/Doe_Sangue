package br.edu.fatec.doesangue.ui.screens.profile

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreference
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreferences
import br.edu.fatec.doesangue.presentation.notifications.RoutineNotificationsViewModel
import br.edu.fatec.doesangue.presentation.routines.ReminderRoutineUiState
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.SecondaryButton
import br.edu.fatec.doesangue.ui.components.TextAction
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun RoutineNotificationsRoute(
    userId: String,
    routineState: ReminderRoutineUiState,
    canEdit: () -> Boolean,
    onPreferenceChanged: () -> Unit,
    onSeeRoutine: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val factory = remember(context, userId) {
        viewModelFactory {
            initializer {
                val storage = context.applicationContext.getSharedPreferences(
                    RoutineNotificationPreferences.FILE_NAME, Context.MODE_PRIVATE,
                )
                RoutineNotificationsViewModel(RoutineNotificationPreferences(storage), userId, onPreferenceChanged)
            }
        }
    }
    val model: RoutineNotificationsViewModel = viewModel(key = userId, factory = factory)
    val preference by model.uiState.collectAsStateWithLifecycle()
    val appointmentReminders by model.appointmentReminders.collectAsStateWithLifecycle()

    key(userId) {
        var permissionGranted by remember(context) { mutableStateOf(notificationsAllowed(context)) }
        var activationAttempted by rememberSaveable { mutableStateOf(false) }
        var choosingTime by rememberSaveable { mutableStateOf(false) }
        var requestForAppointment by rememberSaveable { mutableStateOf<Boolean?>(null) }
        val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGranted = granted && notificationsAllowed(context)
            val appointment = requestForAppointment
            requestForAppointment = null
            if (canEdit()) when (appointment) {
                true -> model.setAppointmentEnabled(true, permissionGranted)
                false -> model.setEnabled(true, permissionGranted)
                null -> Unit
            }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            permissionGranted = notificationsAllowed(context)
        }

        fun toggle(enabled: Boolean, appointment: Boolean) {
            if (!canEdit() || requestForAppointment != null) return
            permissionGranted = notificationsAllowed(context)
            if (!enabled || permissionGranted) {
                if (appointment) model.setAppointmentEnabled(enabled, permissionGranted)
                else model.setEnabled(enabled, permissionGranted)
            } else {
                activationAttempted = true
                if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestForAppointment = appointment
                    requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if (choosingTime) {
            androidx.compose.runtime.DisposableEffect(context) {
                val dialog = TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        if (canEdit()) model.setTime(LocalTime.of(hour, minute))
                    },
                    preference.time.hour,
                    preference.time.minute,
                    DateFormat.is24HourFormat(context),
                )
                dialog.setOnDismissListener { choosingTime = false }
                dialog.show()
                onDispose {
                    dialog.setOnDismissListener(null)
                    dialog.dismiss()
                }
            }
        }

        RoutineNotificationsScreen(
            preference = preference,
            routineState = routineState,
            showPermissionHint = !permissionGranted && (activationAttempted || preference.enabled || appointmentReminders),
            onToggle = { toggle(it, appointment = false) },
            appointmentReminders = appointmentReminders,
            onAppointmentToggle = { toggle(it, appointment = true) },
            onChooseTime = { choosingTime = true },
            onSeeRoutine = onSeeRoutine,
            onOpenSettings = {
                val intent = if (Build.VERSION.SDK_INT >= 26) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null))
                }
                context.startActivity(intent)
            },
            onBack = onBack,
        )
    }
}

@Composable
internal fun RoutineNotificationsScreen(
    preference: RoutineNotificationPreference,
    routineState: ReminderRoutineUiState,
    showPermissionHint: Boolean,
    onToggle: (Boolean) -> Unit,
    onChooseTime: () -> Unit,
    onSeeRoutine: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    appointmentReminders: Boolean = false,
    onAppointmentToggle: (Boolean) -> Unit = {},
) {
    DsScreen(title = "Notificações", onBack = onBack) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lembretes da rotina", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Receba um aviso nas datas que você planejou em Minhas doações → Lembretes.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = preference.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics { contentDescription = "Lembretes da rotina" },
                )
            }
            val routine = routineState.routine
            val nextDate = routine?.let { it.nextDate(LocalDate.now(ZoneId.of(it.timeZoneId))) }
            val routineDescription = when {
                routineState.loading -> "Consultando sua rotina…"
                routineState.error != null || !routineState.loaded -> "Não foi possível consultar a próxima data da rotina."
                routine == null -> "Configure uma rotina em Lembretes para receber avisos."
                !routine.active -> "Sua rotina está pausada. Retome-a em Lembretes para receber avisos."
                nextDate != null -> "Próxima data da rotina: ${nextDate.format(DateTimeFormatter.ofPattern("dd/MM/uuuu"))}"
                else -> "Sua rotina não tem próxima data planejada."
            }
            Text(routineDescription, style = MaterialTheme.typography.bodyMedium)
            TextAction("Ver minha rotina", onSeeRoutine)
            Text("Horário do lembrete da rotina")
            val time = preference.time.format(DateTimeFormatter.ofPattern("HH:mm"))
            SecondaryButton(time, onChooseTime)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Avisos de agendamentos", style = MaterialTheme.typography.titleMedium)
                    Text("Receba um aviso 24 horas antes de um agendamento confirmado.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = appointmentReminders,
                    onCheckedChange = onAppointmentToggle,
                    modifier = Modifier.semantics { contentDescription = "Avisos de agendamentos" },
                )
            }
            if (showPermissionHint) {
                Text("As notificações estão bloqueadas nas configurações do Android.")
                SecondaryButton("Abrir configurações do Android", onOpenSettings)
            }
        }
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()
