package br.edu.fatec.doesangue.ui.screens.scheduling

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.presentation.scheduling.*
import br.edu.fatec.doesangue.ui.components.*

@Composable
fun DraftRecoveryScreen(state: DraftRecoveryUiState, onResume: () -> Unit, onDiscard: () -> Unit,
    onRetry: () -> Unit, onExit: () -> Unit) {
    BackHandler { onExit() }
    DsScreen(title = "Retomar agendamento", onBack = onExit) {
        when (state.phase) {
            DraftRecoveryPhase.LOADING, DraftRecoveryPhase.RESTORING -> {
                CircularProgressIndicator()
                Text("Recuperando seu rascunho…", Modifier.padding(top = 16.dp))
            }
            DraftRecoveryPhase.OFFER -> {
                val pending = state.draft?.requestKey != null
                Text(if (pending) "Há um pedido anterior para conferir" else "Você tem um rascunho salvo",
                    style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Text(state.draft?.centerName.orEmpty())
                Spacer(Modifier.height(16.dp))
                Text(if (pending) "A confirmação anterior pode ter sido interrompida. Ao retomar, manteremos a mesma identificação. Nada será enviado antes de você confirmar novamente."
                    else "Podemos recuperar suas escolhas e conferir novamente se a unidade e o horário continuam disponíveis.")
                Spacer(Modifier.height(24.dp))
                PrimaryButton("Retomar", onResume)
                if (state.canDiscard) { Spacer(Modifier.height(12.dp)); SecondaryButton("Descartar rascunho", onDiscard) }
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Voltar ao início", onExit)
            }
            DraftRecoveryPhase.ERROR -> {
                Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(24.dp)); PrimaryButton("Tentar novamente", onRetry)
                if (state.canDiscard) { Spacer(Modifier.height(12.dp)); SecondaryButton("Descartar rascunho", onDiscard) }
                Spacer(Modifier.height(12.dp)); SecondaryButton("Voltar ao início", onExit)
            }
            DraftRecoveryPhase.READY -> Unit
        }
    }
}
