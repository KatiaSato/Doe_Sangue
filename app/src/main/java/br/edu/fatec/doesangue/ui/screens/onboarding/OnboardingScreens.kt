package br.edu.fatec.doesangue.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.presentation.auth.SessionUiState
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.TextAction
import br.edu.fatec.doesangue.ui.theme.BloodRed

@Composable
fun StartupSessionScreen(
    uiState: SessionUiState,
    onRetry: () -> Unit,
    onFinished: (Boolean) -> Unit,
) {
    LaunchedEffect(uiState) {
        when (uiState) {
            is SessionUiState.Authenticated -> onFinished(true)
            SessionUiState.Unauthenticated -> onFinished(false)

            SessionUiState.Loading,
            is SessionUiState.Error -> Unit
        }
    }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = uiState) {
            SessionUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Verificando sua sessão...")
            }

            is SessionUiState.Error -> {
                Text(state.message, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Tentar novamente", onRetry)
            }

            else -> Unit
        }
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Bem-vindo!", style = MaterialTheme.typography.headlineLarge, color = BloodRed)
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.figma_raw_3),
            contentDescription = "Mão segurando uma gota de sangue",
            modifier = Modifier.fillMaxWidth().height(250.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            "Sua doação pode transformar histórias. Agende, doe e acompanhe seu impacto.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Text("●  ○  ○", color = BloodRed)
        Spacer(Modifier.weight(1f))
        PrimaryButton("Começar", onStart)
        TextAction("Já tenho uma conta", onLogin, Modifier.clickable(onClick = onLogin))
    }
}

