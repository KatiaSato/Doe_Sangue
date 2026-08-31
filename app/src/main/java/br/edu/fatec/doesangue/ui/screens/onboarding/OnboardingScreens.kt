package br.edu.fatec.doesangue.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.R
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.TextAction
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.BloodRedStrong
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1_800)
        onFinished()
    }
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BloodRedStrong, Color(0xFFF0141A)))),
    ) {
        Box(Modifier.fillMaxWidth().height(170.dp).align(Alignment.BottomStart).background(Color(0x22FFFFFF)))
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.figma_raw_1),
                contentDescription = "Símbolo Doe Sangue",
                modifier = Modifier.size(190.dp).clip(RoundedCornerShape(34.dp)),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.height(120.dp))
            Text("Doe Sangue", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text(
                "Um pequeno ato\nque salva vidas.",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            Text("—    ♡    —", style = MaterialTheme.typography.titleLarge, color = Color.White)
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

