package br.edu.fatec.doesangue.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.edu.fatec.doesangue.presentation.auth.LogoutUiState
import br.edu.fatec.doesangue.presentation.profile.ProfileUiState
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.presentation.auth.ChangePasswordUiState
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
import br.edu.fatec.doesangue.R

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
    onBloodTypeChange: (BloodType) -> Unit,
    onSaveBloodType: () -> Unit,
    onStartBloodTypeEdit: () -> Unit,
    onCancelBloodTypeEdit: () -> Unit,
) {
    val saving = (uiState as? ProfileUiState.Loaded)?.isSavingBloodType == true
    // A aba pode ser a única tela da pilha. O Voltar do Android usa a mesma saída da seta.
    BackHandler { if (!saving) onBack() }
    DsScreen(
        title = "Meu perfil",
        onBack = if (saving) null else onBack,
        bottomRoute = AppRoute.Profile,
        onNavigate = { if (!saving) onNavigate(it) },
    ) {
        ProfileContent(
            uiState = uiState,
            onRetry = onRetry,
            onDisplayNameChange = onDisplayNameChange,
            onCreateProfile = onCreateProfile,
            onBloodTypeChange = onBloodTypeChange,
            onSaveBloodType = onSaveBloodType,
            onStartBloodTypeEdit = onStartBloodTypeEdit,
            onCancelBloodTypeEdit = onCancelBloodTypeEdit,
        )

        ProfileMenuRow("Minhas doações", R.drawable.home_ic_heart, onDonations, enabled = !saving)
        ProfileMenuRow("Meu agendamento", R.drawable.home_ic_calendar, onAppointment, enabled = !saving)
        ProfileMenuRow("Configurações", R.drawable.profile_ic_settings, onSettings, enabled = !saving)
        Spacer(Modifier.height(24.dp))
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
    logoutUiState: LogoutUiState,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSecurity: () -> Unit,
    onNotifications: () -> Unit,
    onFaq: () -> Unit,
    onContact: () -> Unit,
    onAbout: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
) {

    val canNavigate =
        !logoutUiState.isLoading && !logoutUiState.isSignedOut
    BackHandler(enabled = !canNavigate) {
        // Aguarda o logout terminar antes de permitir voltar.
    }

    DsScreen(
        title = "Configurações",
        onBack = if (canNavigate) onBack else null,
        bottomRoute = AppRoute.Profile,
        onNavigate = { route ->
            if (canNavigate) {
                onNavigate(route)
            }
        },
    ) {
        SectionHeader("Conta")
        SettingsRow("Editar perfil", onEditProfile, enabled = canNavigate)
        SettingsRow("Segurança", onSecurity, enabled = canNavigate)
        SettingsRow("Notificações", onNotifications, enabled = canNavigate)
        Spacer(Modifier.height(28.dp))
        SectionHeader("Ajuda")
        SettingsRow("Dúvidas frequentes", onFaq, enabled = canNavigate)
        SettingsRow("Fale conosco", onContact, enabled = canNavigate)
        SettingsRow("Sobre o app", onAbout, enabled = canNavigate)
        Spacer(Modifier.height(60.dp))
        logoutUiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
        }

        SecondaryButton(
            text = if (logoutUiState.isLoading) "Saindo..." else "Sair da conta",
            onClick = onSignOut,
            enabled = !logoutUiState.isLoading && !logoutUiState.isSignedOut,
        )
    }
}

@Composable
private fun SettingsRow(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = Ink)
        Text("›", style = MaterialTheme.typography.titleLarge, color = BloodRed)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
}

@Composable
fun SecurityScreen(
    state: ChangePasswordUiState,
    onSave: (String, String, String) -> Unit,
    onBack: () -> Unit,
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }

    val fieldsEnabled = !state.isSaving && !state.passwordChanged

    val context = LocalContext.current
    val finish by rememberUpdatedState(onBack)

    LaunchedEffect(state.passwordChanged) {
        if (state.passwordChanged) {
            currentPassword = ""
            newPassword = ""
            confirmation = ""

            currentPasswordVisible = false
            newPasswordVisible = false
            confirmationVisible = false

            Toast.makeText(
                context,
                "Senha salva com sucesso.",
                Toast.LENGTH_SHORT,
            ).show()

            finish()
        }
    }
    BackHandler(enabled = !fieldsEnabled) {
        // Aguarda a conclusão antes de permitir voltar.
    }

    DsScreen(
        title = "Segurança",
        onBack = if (fieldsEnabled) onBack else null,
    ) {
        Spacer(Modifier.height(12.dp))
        SectionHeader("Senha")
        Spacer(Modifier.height(14.dp))
        SecurityPasswordField(
            label = "Senha atual",
            value = currentPassword,
            onValueChange = { currentPassword = it },
            visible = currentPasswordVisible,
            onToggleVisibility = {
                currentPasswordVisible = !currentPasswordVisible
            },
            enabled = fieldsEnabled,
        )

        Spacer(Modifier.height(16.dp))

        SecurityPasswordField(
            label = "Nova senha",
            value = newPassword,
            onValueChange = { newPassword = it },
            visible = newPasswordVisible,
            onToggleVisibility = {
                newPasswordVisible = !newPasswordVisible
            },
            enabled = fieldsEnabled,
        )

        Spacer(Modifier.height(16.dp))

        SecurityPasswordField(
            label = "Confirmar nova senha",
            value = confirmation,
            onValueChange = { confirmation = it },
            visible = confirmationVisible,
            onToggleVisibility = {
                confirmationVisible = !confirmationVisible
            },
            enabled = fieldsEnabled,
        )
        Spacer(Modifier.height(12.dp))

        Text(
            "Use ao menos 8 caracteres com maiúscula, minúscula, número e símbolo.",
            style = MaterialTheme.typography.bodySmall,
        )

        state.errorMessage?.let { message ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(38.dp))
        PrimaryButton(
            text = if (state.isSaving) "Salvando…" else "Alterar senha",
            onClick = {
                onSave(
                    currentPassword,
                    newPassword,
                    confirmation,
                )
            },
            enabled = fieldsEnabled,
        )
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
private fun SecurityPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (visible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            TextButton(
                onClick = onToggleVisibility,
                enabled = enabled,
            ) {
                Text(if (visible) "Ocultar" else "Mostrar")
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
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
        Text(
            "Encontre orientações sobre a doação e sobre o uso do aplicativo.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary,
        )
        Spacer(Modifier.height(16.dp))
        listOf(
            "Quem pode doar sangue?" to
                "A equipe do hemocentro avalia os critérios de saúde e a aptidão na triagem. " +
                "Consulte as orientações em Antes de doar, na tela Início, e confirme suas dúvidas com a unidade.",
            "Quanto tempo dura uma doação?" to
                "O Ministério da Saúde estima cerca de 40 minutos para o processo completo, " +
                "incluindo cadastro, avaliações, coleta e lanche. O tempo pode variar conforme o atendimento da unidade.",
            "Com que frequência posso doar?" to
                "O intervalo depende do tipo de doação e da avaliação do hemocentro. " +
                "Confirme com a equipe quando poderá retornar. O aplicativo não calcula uma próxima data de aptidão.",
            "Preciso estar em jejum?" to
                "Não. Vá alimentado e descansado e siga as orientações de preparo do hemocentro.",
            "O agendamento garante que poderei doar?" to
                "Não. O agendamento organiza o atendimento; a aptidão é avaliada na triagem. " +
                "Neste projeto acadêmico, unidades e horários são fictícios e não representam reservas em hemocentros reais.",
            "Como cancelar um agendamento?" to
                "Abra Doações, selecione Agendamentos e toque em Ver detalhes no registro desejado. " +
                "Depois, escolha Cancelar agendamento e confirme. O cancelamento é permitido somente antes do horário de início.",
            "Onde vejo meus agendamentos e minhas doações?" to
                "Consulte seus registros em Doações → Agendamentos. Um agendamento não comprova uma doação realizada. " +
                "A aba Histórico consulta registros fictícios publicados pela administração e calcula o total desses registros.",
            "Posso me cadastrar sem saber meu tipo sanguíneo?" to
                "Sim. Escolha Não sei no cadastro. Você pode atualizar essa informação na tela Perfil. " +
                "O tipo informado é autodeclarado e não substitui a avaliação do serviço de saúde.",
            "Como funciona o filtro de campanhas por tipo sanguíneo?" to
                "Selecione Todos ou um tipo na tela Campanhas. As campanhas gerais aparecem em qualquer filtro; " +
                "as demais aparecem para os tipos divulgados. O filtro não calcula compatibilidade sanguínea.",
        ).forEach { (question, answer) ->
            InfoCard {
                Text(question, style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(answer, style = MaterialTheme.typography.bodyMedium, color = InkSecondary)
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            "Orientações gerais: Ministério da Saúde (gov.br/saude) e Hemocentro Unicamp. " +
                "Conteúdo revisado em 15/10/2026. A avaliação individual é feita pelo hemocentro.",
            style = MaterialTheme.typography.bodySmall,
            color = InkSecondary,
        )
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
fun AboutScreen(onBack: () -> Unit, onPrivacy: () -> Unit) {
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
            Text(br.edu.fatec.doesangue.BuildConfig.VERSION_NAME, style = MaterialTheme.typography.labelMedium)
        }
        SettingsRow("Privacidade e termos", onPrivacy)
    }
}

@Composable
fun PrivacyTermsScreen(onBack: () -> Unit) {
    DsScreen(title = "Privacidade e termos", onBack = onBack) {
        Text("Projeto acadêmico Doe Sangue", style = MaterialTheme.typography.titleLarge)
        Text("Informações sobre esta versão • 15/10/2026", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        listOf(
            "Uso de demonstração" to "Unidades, horários, campanhas, estoques e histórico são fictícios. " +
                "O aplicativo não realiza reservas em hemocentros reais e não substitui atendimento, triagem ou avaliação de aptidão.",
            "Conta e perfil" to "O Supabase Auth gerencia e-mail, senha e sessão. O perfil guarda nome de exibição, tipo sanguíneo autodeclarado " +
                "(incluindo Não sei) e, se informados, telefone e data de nascimento. Use somente dados fictícios nesta demonstração; " +
                "não informe dados clínicos ou de terceiros.",
            "Dados consultados e persistidos" to "O aplicativo consulta unidades e campanhas publicadas. Reservas, cancelamentos, perfil e histórico " +
                "são vinculados à conta. Estoques e registros de doação desta versão são demonstrações publicadas manualmente; agendar não confirma coleta.",
            "Acesso aos dados" to "As consultas privadas identificam a conta pela sessão. As regras do servidor limitam o acesso do doador aos próprios " +
                "registros e impedem que ele publique estoques ou confirme doações. A administração do projeto possui acesso pelo Supabase.",
            "Sessão neste dispositivo" to "A sessão permite reabrir o aplicativo conectado. Use Configurações → Sair da conta ao terminar em um dispositivo compartilhado. " +
                "Sair encerra a sessão local; não exclui a conta nem os registros no servidor.",
            "Limites desta versão" to "A exclusão de conta pelo aplicativo, um canal de solicitações de privacidade e prazos de retenção ainda não foram definidos " +
                "para uma operação pública. Fale conosco permanece demonstrativo. Esta página informa o funcionamento do protótipo acadêmico e deverá ser revista antes de uso público.",
        ).forEach { (title, body) ->
            InfoCard {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
