package br.edu.fatec.doesangue.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.presentation.profile.ProfileUiState
import br.edu.fatec.doesangue.ui.components.CenteredMessage
import br.edu.fatec.doesangue.ui.components.DsScreen
import br.edu.fatec.doesangue.ui.components.InfoCard
import br.edu.fatec.doesangue.ui.components.PrimaryButton
import br.edu.fatec.doesangue.ui.components.SecondaryButton
import br.edu.fatec.doesangue.ui.components.SectionHeader
import br.edu.fatec.doesangue.ui.components.TextAction
import br.edu.fatec.doesangue.ui.components.VisualField
import br.edu.fatec.doesangue.ui.navigation.AppRoute
import br.edu.fatec.doesangue.ui.theme.BloodRed
import br.edu.fatec.doesangue.ui.theme.BloodRedSoft
import br.edu.fatec.doesangue.ui.theme.Border
import br.edu.fatec.doesangue.ui.theme.Ink
import br.edu.fatec.doesangue.ui.theme.InkSecondary

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onDonations: () -> Unit,
    onAppointment: () -> Unit,
    onSettings: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onCreateProfile: () -> Unit,
) {
    DsScreen(
        title = "Meu perfil",
        onBack = onBack,
        bottomRoute = AppRoute.Profile,
        onNavigate = onNavigate,
    ) {
        ProfileContent(
            uiState = uiState,
            onRetry = onRetry,
            onDisplayNameChange = onDisplayNameChange,
            onCreateProfile = onCreateProfile,
        )

        Spacer(Modifier.height(16.dp))
        SecondaryButton("Minhas doações", onDonations)
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Meu agendamento", onAppointment)
        Spacer(Modifier.height(10.dp))
        SecondaryButton("Configurações", onSettings)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileInfo(icon: String, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, modifier = Modifier.width(34.dp), color = BloodRed, style = MaterialTheme.typography.titleLarge)
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Ink)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSecurity: () -> Unit,
    onNotifications: () -> Unit,
    onFaq: () -> Unit,
    onContact: () -> Unit,
    onAbout: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {
    DsScreen(title = "Configurações", onBack = onBack, bottomRoute = AppRoute.Profile, onNavigate = onNavigate) {
        SectionHeader("Conta")
        SettingsRow("Editar perfil", onEditProfile)
        SettingsRow("Segurança", onSecurity)
        SettingsRow("Notificações", onNotifications)
        Spacer(Modifier.height(28.dp))
        SectionHeader("Ajuda")
        SettingsRow("Dúvidas frequentes", onFaq)
        SettingsRow("Fale conosco", onContact)
        SettingsRow("Sobre o app", onAbout)
        Spacer(Modifier.height(60.dp))
        SecondaryButton("Sair da conta", {})
    }
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Ink)
        Text("›", style = MaterialTheme.typography.titleLarge, color = BloodRed)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
}

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    DsScreen(title = "Editar perfil", onBack = onBack) {
        Spacer(Modifier.height(12.dp))
        VisualField("Nome completo", "Mariana Silva")
        Spacer(Modifier.height(12.dp))
        VisualField("E-mail", "mariana@email.com")
        Spacer(Modifier.height(12.dp))
        VisualField("Telefone", "(19) 99999-9999")
        Spacer(Modifier.height(12.dp))
        VisualField("Data de nascimento", "15/04/1996")
        Spacer(Modifier.height(12.dp))
        VisualField("Tipo sanguíneo", "O+")
        Spacer(Modifier.height(60.dp))
        PrimaryButton("Salvar alterações", {})
    }
}

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    DsScreen(title = "Segurança", onBack = onBack) {
        Spacer(Modifier.height(12.dp))
        SectionHeader("Senha")
        Spacer(Modifier.height(14.dp))
        VisualField("Senha atual", "••••••••")
        Spacer(Modifier.height(16.dp))
        VisualField("Nova senha", "••••••••")
        Spacer(Modifier.height(16.dp))
        VisualField("Confirmar nova senha", "••••••••")
        Spacer(Modifier.height(38.dp))
        PrimaryButton("Alterar senha", {})
        Spacer(Modifier.height(32.dp))
        Card(colors = CardDefaults.cardColors(containerColor = BloodRedSoft), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Proteja sua conta", style = MaterialTheme.typography.titleMedium, color = BloodRed)
                Text("Use uma senha forte e não compartilhe suas credenciais.", style = MaterialTheme.typography.bodySmall, color = InkSecondary)
            }
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    DsScreen(title = "Notificações", onBack = onBack) {
        NotificationRow("Lembrete de doação", "Receber aviso antes do agendamento", true)
        NotificationRow("Rotina de doações", "Avisar quando chegar o próximo período", true)
        NotificationRow("Campanhas", "Novas campanhas e estoques críticos", true)
        NotificationRow("Atualizações do app", "Novidades importantes do Doe Sangue", false)
    }
}

@Composable
private fun NotificationRow(title: String, subtitle: String, checked: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = BloodRed, checkedThumbColor = Color.White),
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
}

@Composable
fun FaqScreen(onBack: () -> Unit) {
    DsScreen(title = "Dúvidas frequentes", onBack = onBack) {
        listOf(
            "Quem pode doar sangue?",
            "Quanto tempo dura uma doação?",
            "Com que frequência posso doar?",
            "Preciso estar em jejum?",
            "Como cancelar um agendamento?",
            "Onde vejo minhas doações?",
        ).forEach { question ->
            InfoCard {
                Row(Modifier.fillMaxWidth()) {
                    Text(question, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("›", color = BloodRed)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun ContactScreen(onBack: () -> Unit) {
    DsScreen(title = "Fale conosco", onBack = onBack) {
        Spacer(Modifier.height(24.dp))
        CenteredMessage("♡", "Como podemos ajudar?", "Envie sua dúvida, sugestão ou relato.")
        Spacer(Modifier.height(32.dp))
        VisualField("Assunto", "Selecione um assunto")
        Spacer(Modifier.height(16.dp))
        VisualField("Mensagem", "Escreva sua mensagem...", tall = true)
        Spacer(Modifier.height(34.dp))
        PrimaryButton("Enviar mensagem", {})
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    DsScreen(title = "Sobre o app", onBack = onBack) {
        Spacer(Modifier.height(100.dp))
        CenteredMessage("♦", "Doe Sangue", "Um pequeno ato que salva vidas.")
        Spacer(Modifier.height(34.dp))
        Text(
            "O Doe Sangue foi pensado para facilitar o agendamento de doações, acompanhar seu histórico e aproximar doadores de campanhas e hemocentros.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
        )
        Spacer(Modifier.height(64.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("Versão", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text("1.0.0", style = MaterialTheme.typography.labelMedium)
        }
        SettingsRow("Privacidade e termos", {})
    }
}
