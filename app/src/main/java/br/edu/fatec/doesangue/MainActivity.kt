package br.edu.fatec.doesangue

import android.os.Bundle
import android.os.SystemClock
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkViewModel
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkUiState
import br.edu.fatec.doesangue.ui.navigation.recoveryCodeFromUri
import br.edu.fatec.doesangue.ui.navigation.DoeSangueApp
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import br.edu.fatec.doesangue.presentation.auth.EmailConfirmationViewModel
import br.edu.fatec.doesangue.ui.navigation.confirmationCredentialFromUri
import android.widget.Toast
import br.edu.fatec.doesangue.data.notifications.NotificationOpenRequest
import br.edu.fatec.doesangue.data.notifications.notificationOpenRequest
import kotlinx.coroutines.flow.MutableStateFlow

private const val PENDING_NOTIFICATION_OWNER = "pending_notification_owner"
private const val PENDING_NOTIFICATION_APPOINTMENT = "pending_notification_appointment"

class MainActivity : ComponentActivity() {
    private var startupReady = false
    private val pendingNotification = MutableStateFlow<NotificationOpenRequest?>(null)
    private val emailConfirmationViewModel by viewModels<EmailConfirmationViewModel> {
        (application as DoeSangueApplication).appContainer.emailConfirmationViewModelFactory
    }
    private val recoveryLinkViewModel by viewModels<RecoveryLinkViewModel> {
        (application as DoeSangueApplication)
            .appContainer.recoveryLinkViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val splashDeadline = SystemClock.uptimeMillis() + 3_000
        splash.setKeepOnScreenCondition { !startupReady && SystemClock.uptimeMillis() < splashDeadline }
        val appContainer =
            (application as DoeSangueApplication).appContainer
        pendingNotification.value = savedInstanceState?.getString(PENDING_NOTIFICATION_OWNER)?.let { owner ->
            NotificationOpenRequest(owner, savedInstanceState.getString(PENDING_NOTIFICATION_APPOINTMENT))
        }
        if (savedInstanceState == null) handleIncomingIntent(intent)
        val app = application as DoeSangueApplication
        if (app.isRecoveryPending() && recoveryLinkViewModel.uiState.value == RecoveryLinkUiState.Idle) {
            recoveryLinkViewModel.validateCode("")
        }
        enableEdgeToEdge()
        setContent {
            DoeSangueTheme {
                DoeSangueApp(
                    emailConfirmationViewModel = emailConfirmationViewModel,
                    appContainer = appContainer,
                    recoveryLinkState = recoveryLinkViewModel.uiState,
                    requestedNotification = pendingNotification,
                    onStartupReady = { startupReady = true },
                    onNotificationHandled = { request -> pendingNotification.compareAndSet(request, null) },
                    onUpdateRecoveryPassword = recoveryLinkViewModel::updatePassword,
                    onRetryRecovery = recoveryLinkViewModel::retry,
                    onFinishRecovery = recoveryLinkViewModel::finishRecovery,
                    onRecoveryFinished = {
                        app.setRecoveryPending(false)
                        recoveryLinkViewModel.acknowledgeFinished()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val request = pendingNotification.value
        outState.putString(PENDING_NOTIFICATION_OWNER, request?.ownerId)
        outState.putString(PENDING_NOTIFICATION_APPOINTMENT, request?.appointmentId)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        val container = (application as DoeSangueApplication).appContainer
        container.onRoutineNotificationsChanged()
        container.onAppointmentNotificationsChanged()
    }

    private fun handleIncomingIntent(incoming: Intent) {
        val request = incoming.notificationOpenRequest()
        if (request == null) {
            handleRecoveryIntent(incoming)
            return
        }
        setIntent(Intent(incoming).apply { action = null; data = null })
        pendingNotification.value = request
    }

    private fun handleRecoveryIntent(incoming: Intent) {
        val uri = incoming.data ?: return
        if (incoming.action != Intent.ACTION_VIEW ||
            uri.scheme != "br.edu.fatec.doesangue" || uri.host != "auth"
        ) return
        if (uri.path?.startsWith("/confirmation") == true) {
            setIntent(Intent(incoming).apply { data = null })
            val app = application as DoeSangueApplication
            if (app.isRecoveryPending() || recoveryLinkViewModel.uiState.value != RecoveryLinkUiState.Idle) {
                Toast.makeText(this, "Conclua a recuperação de senha antes de abrir a confirmação.", Toast.LENGTH_LONG).show()
            } else emailConfirmationViewModel.handleLink(confirmationCredentialFromUri(uri))
            return
        }
        emailConfirmationViewModel.dismissLink()
        val persisted = (application as DoeSangueApplication).setRecoveryPending(true)
        recoveryLinkViewModel.validateCode(if (persisted) recoveryCodeFromUri(uri).orEmpty() else "")
        // Não manter o código na instância da Activity nem colocá-lo no estado salvo.
        setIntent(Intent(incoming).apply { data = null })
    }
}
