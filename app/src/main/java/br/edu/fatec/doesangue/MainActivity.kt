package br.edu.fatec.doesangue

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkViewModel
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkUiState
import br.edu.fatec.doesangue.ui.navigation.recoveryCodeFromUri
import br.edu.fatec.doesangue.ui.navigation.DoeSangueApp
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import br.edu.fatec.doesangue.presentation.auth.EmailConfirmationViewModel
import br.edu.fatec.doesangue.ui.navigation.confirmationCredentialFromUri
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private val emailConfirmationViewModel by viewModels<EmailConfirmationViewModel> {
        (application as DoeSangueApplication).appContainer.emailConfirmationViewModelFactory
    }
    private val recoveryLinkViewModel by viewModels<RecoveryLinkViewModel> {
        (application as DoeSangueApplication)
            .appContainer.recoveryLinkViewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer =
            (application as DoeSangueApplication).appContainer
        if (savedInstanceState == null) handleRecoveryIntent(intent)
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
        handleRecoveryIntent(intent)
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
