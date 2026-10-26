package br.edu.fatec.doesangue.ui.screens.scheduling

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import br.edu.fatec.doesangue.presentation.scheduling.CenterPreparationUiState
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.SecondaryButton

@Composable
fun CenterPreparationScreen(
    state: CenterPreparationUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    DsScreen(
        title = "Agendar doação",
        onBack = onBack,
    ) {
        when (state) {
            CenterPreparationUiState.Idle,
            is CenterPreparationUiState.Loading ->
                Text("Consultando unidade…")

            is CenterPreparationUiState.Ready ->
                Text("Unidade selecionada.")

            is CenterPreparationUiState.Error -> {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                )

                if (state.centerId.isNotBlank()) {
                    SecondaryButton("Tentar novamente", onRetry)
                }
            }
        }
    }
}