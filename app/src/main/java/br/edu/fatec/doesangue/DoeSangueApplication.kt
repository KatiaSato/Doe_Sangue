package br.edu.fatec.doesangue

import android.app.Application
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationPreferences
import br.edu.fatec.doesangue.data.notifications.RoutineNotificationScheduler
import br.edu.fatec.doesangue.data.notifications.AppointmentNotificationScheduler

open class DoeSangueApplication : Application() {

    open val appContainer: AppContainer by lazy {
        AppContainer(
            schedulingDraftStore = br.edu.fatec.doesangue.data.local.FileSchedulingDraftStore(
                java.io.File(noBackupFilesDir, "scheduling-drafts"),
            ),
            onRoutineNotificationsChanged = { routineNotifications.refresh() },
            onAppointmentNotificationsChanged = { appointmentNotifications.refresh() },
        )
    }

    internal val routineNotifications by lazy {
        RoutineNotificationScheduler(
            applicationContext,
            appContainer.authRepository,
            appContainer.routineRepository,
            RoutineNotificationPreferences(
                getSharedPreferences(RoutineNotificationPreferences.FILE_NAME, MODE_PRIVATE),
            ),
            canUseSession = { !isRecoveryPending() },
        )
    }

    internal val appointmentNotifications by lazy {
        AppointmentNotificationScheduler(
            applicationContext,
            appContainer.authRepository,
            appContainer.appointmentRepository,
            RoutineNotificationPreferences(
                getSharedPreferences(RoutineNotificationPreferences.FILE_NAME, MODE_PRIVATE),
            ),
            canUseSession = { !isRecoveryPending() },
        )
    }

    // Só um marcador, sem código, senha ou token. Recuperação interrompida não abre Home.
    open fun isRecoveryPending(): Boolean =
        getSharedPreferences("recovery_flow", MODE_PRIVATE).getBoolean("pending", false)

    open fun setRecoveryPending(pending: Boolean): Boolean =
        getSharedPreferences("recovery_flow", MODE_PRIVATE).edit()
            .putBoolean("pending", pending).commit()
}
