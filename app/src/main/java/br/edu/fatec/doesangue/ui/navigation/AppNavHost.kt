package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Snackbar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.navigation.compose.currentBackStackEntryAsState
import br.edu.fatec.doesangue.presentation.auth.SessionMonitorUiState
import br.edu.fatec.doesangue.ui.screens.auth.SessionWaitScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import br.edu.fatec.doesangue.ui.screens.onboarding.SplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.fatec.doesangue.presentation.auth.RecoveryLinkUiState
import br.edu.fatec.doesangue.presentation.auth.SessionViewModel
import br.edu.fatec.doesangue.presentation.auth.ResetPasswordViewModel
import br.edu.fatec.doesangue.ui.screens.auth.ResetPasswordScreen
import br.edu.fatec.doesangue.ui.screens.auth.RecoveryStatusScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import br.edu.fatec.doesangue.presentation.auth.EmailConfirmationUiState
import br.edu.fatec.doesangue.presentation.auth.ConfirmationLinkState
import br.edu.fatec.doesangue.ui.screens.auth.EmailConfirmationDialog
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavDestination.Companion.hasRoute

private val idleConfirmationState: StateFlow<EmailConfirmationUiState> = MutableStateFlow(EmailConfirmationUiState())

@Composable
fun AppNavHost(
    navController: NavHostController,
    recoveryLinkState: StateFlow<RecoveryLinkUiState>,
    sessionState: StateFlow<SessionMonitorUiState>? = null,
    onRetryRecovery: () -> Unit = {},
    onFinishRecovery: () -> Unit = {},
    onRecoveryFinished: () -> Unit = {},
    emailConfirmationState: StateFlow<EmailConfirmationUiState> = idleConfirmationState,
    onConfirmationEmailChange: (String) -> Unit = {},
    onResendConfirmation: () -> Unit = {},
    onRetryConfirmation: () -> Unit = {},
    onDismissConfirmation: () -> Unit = {},
    schedulingViewModelFactory: ViewModelProvider.Factory,
    loginViewModelFactory: ViewModelProvider.Factory,
    passwordRecoveryViewModelFactory: ViewModelProvider.Factory,
    resetPasswordViewModelFactory: (Long) -> ViewModelProvider.Factory,
    changePasswordViewModelFactory: ViewModelProvider.Factory,
    profileViewModelFactory: ViewModelProvider.Factory,
    editProfileViewModelFactory: ViewModelProvider.Factory,
    appointmentsViewModelFactory: ViewModelProvider.Factory,
    appointmentDetailViewModelFactory: (String) -> ViewModelProvider.Factory,
    logoutViewModelFactory: ViewModelProvider.Factory,
    sessionViewModelFactory: ViewModelProvider.Factory,
    registrationViewModelFactory: ViewModelProvider.Factory,
    campaignsViewModelFactory: ViewModelProvider.Factory,
    campaignDetailViewModelFactory: (String) -> ViewModelProvider.Factory,
    homeViewModelFactory: ViewModelProvider.Factory,
    bloodStockViewModelFactory: ViewModelProvider.Factory,
    donationHistoryViewModelFactory: ViewModelProvider.Factory,
    centersViewModelFactory: ViewModelProvider.Factory,
    centerDetailViewModelFactory: (String) -> ViewModelProvider.Factory,
) {
    val currentRecoveryLinkState by
    recoveryLinkState.collectAsStateWithLifecycle()
    val confirmation by emailConfirmationState.collectAsStateWithLifecycle()
    val monitoredSession = sessionState?.collectAsStateWithLifecycle()?.value
    val entry by navController.currentBackStackEntryAsState()
    var privateOwnerId by rememberSaveable { mutableStateOf<String?>(null) }
    var sessionNotice by rememberSaveable { mutableStateOf<String?>(null) }
    fun privateAreaNow() = navController.currentDestination?.hierarchy?.any {
        it.hasRoute<AppGraph.Main>() || it.hasRoute<AppGraph.Scheduling>()
    } == true
    fun knownUserId(state: SessionMonitorUiState?) = when (state) {
        is SessionMonitorUiState.Available -> state.user.id
        is SessionMonitorUiState.Reconnecting -> state.lastVerifiedUser?.id
        else -> null
    }
    val sessionAction = sessionNavigationAction(privateAreaNow(),
        currentRecoveryLinkState != RecoveryLinkUiState.Idle, privateOwnerId, monitoredSession)
    val hidePrivateArea = sessionAction in setOf(SessionNavigationAction.WAIT, SessionNavigationAction.LOGIN,
        SessionNavigationAction.WELCOME, SessionNavigationAction.LOGIN_CHANGED_ACCOUNT)
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(hidePrivateArea) {
        if (hidePrivateArea) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    LaunchedEffect(monitoredSession, entry?.id, currentRecoveryLinkState) {
        // Refaz a conferência com os valores atuais: callbacks de logout podem já ter navegado.
        val current = sessionState?.value
        when (sessionNavigationAction(privateAreaNow(), recoveryLinkState.value != RecoveryLinkUiState.Idle,
            privateOwnerId, current)) {
            SessionNavigationAction.BIND_OWNER -> privateOwnerId = knownUserId(current)
            SessionNavigationAction.LOGIN, SessionNavigationAction.LOGIN_CHANGED_ACCOUNT -> {
                sessionNotice = if (current is SessionMonitorUiState.Ended)
                    "Sua sessão foi encerrada. Entre novamente para continuar."
                else "A conta conectada mudou. Entre novamente para continuar."
                navController.navigate(AppRoute.Login) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
                privateOwnerId = null
            }
            SessionNavigationAction.WELCOME -> {
                sessionNotice = null
                navController.navigate(AppGraph.Auth) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
                privateOwnerId = null
            }
            SessionNavigationAction.WAIT, SessionNavigationAction.NONE -> Unit
        }
    }
    BackHandler(enabled = hidePrivateArea) {}

    // Cada transição remove as telas e ViewModels da conta/tentativa anterior.
    LaunchedEffect(currentRecoveryLinkState) {
        if (currentRecoveryLinkState != RecoveryLinkUiState.Idle) sessionNotice = null
        when (currentRecoveryLinkState) {
            RecoveryLinkUiState.Idle -> Unit
            RecoveryLinkUiState.Finished -> {
                navController.navigate(AppRoute.Login) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
                onRecoveryFinished()
            }
            else -> navController.navigate(AppRoute.ResetPassword) {
                popUpTo(navController.graph.id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    BackHandler(enabled = currentRecoveryLinkState != RecoveryLinkUiState.Idle) {
        onFinishRecovery()
    }

    // Define como abrir destinos solicitados pelas telas
    // e pela barra inferior.
    fun navigateMain(route: AppRoute) {
        if ((sessionState != null && !privateAreaNow()) || recoveryLinkState.value != RecoveryLinkUiState.Idle || sessionNavigationAction(privateAreaNow(), false,
                privateOwnerId, sessionState?.value) in setOf(SessionNavigationAction.WAIT, SessionNavigationAction.LOGIN,
                SessionNavigationAction.WELCOME, SessionNavigationAction.LOGIN_CHANGED_ACCOUNT)) return
        when (route) {
            AppRoute.ScheduleCenter -> {
                // Entramos pelo grupo para respeitar seu destino inicial.
                navController.navigate(AppGraph.Scheduling) {
                    popUpTo<AppGraph.Main> {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }

            AppRoute.Home,
            AppRoute.DonationsHistory,
            AppRoute.Centers,
            AppRoute.Profile -> {
                // Nesta primeira versão, trocar de seção
                // remove o percurso anterior da área principal.
                navController.navigate(route) {
                    popUpTo<AppGraph.Main> {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }

            else -> {
                // Destinos internos mantêm a origem para permitir Voltar.
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Mantém o grafo montado durante uma renovação sem identidade confirmada.
        // Oculta conteúdo/semântica e intercepta toques até a decisão do monitor.
        NavHost(
            navController = navController,
            startDestination = AppRoute.Splash,
            modifier = if (hidePrivateArea) Modifier.alpha(0f).clearAndSetSemantics {}.onPreviewKeyEvent { true } else Modifier,
        ) {
            composable<AppRoute.ResetPassword> {
                when (val recovery = currentRecoveryLinkState) {
                    is RecoveryLinkUiState.Validated -> {
                        val model: ResetPasswordViewModel = viewModel(
                            key = "reset-${recovery.validationId}",
                            factory = resetPasswordViewModelFactory(recovery.validationId),
                        )
                        val state by model.uiState.collectAsStateWithLifecycle()
                        ResetPasswordScreen(
                            state = state,
                            onSave = { password, confirmation ->
                                if (recoveryLinkState.value == recovery) {
                                    model.savePassword(password, confirmation)
                                }
                            },
                            onFinish = onFinishRecovery,
                        )
                    }
                    is RecoveryLinkUiState.Error -> RecoveryStatusScreen(
                        message = recovery.message,
                        onRetry = if (recovery.canRetry) onRetryRecovery else null,
                        onExit = onFinishRecovery,
                    )
                    is RecoveryLinkUiState.Leaving -> RecoveryStatusScreen(
                        message = recovery.errorMessage ?: "Encerrando a sessão…",
                        onRetry = if (recovery.errorMessage != null) onFinishRecovery else null,
                    )
                    RecoveryLinkUiState.Processing -> RecoveryStatusScreen(
                        message = "Validando seu link…",
                        onExit = onFinishRecovery,
                    )
                    RecoveryLinkUiState.Finished -> RecoveryStatusScreen("Concluindo…")
                    RecoveryLinkUiState.Idle -> {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppRoute.Login) {
                                popUpTo(navController.graph.id) { inclusive = false }
                            }
                        }
                    }
                }
            }
            composable<AppRoute.Splash> {

                val sessionViewModel: SessionViewModel = viewModel(
                    factory = sessionViewModelFactory,
                )

                val sessionUiState by
                sessionViewModel.uiState.collectAsStateWithLifecycle()
                SplashScreen(
                    uiState = sessionUiState,
                    onRetry = sessionViewModel::restoreSession,
                    onFinished = { authenticated ->
                        if (recoveryLinkState.value != RecoveryLinkUiState.Idle) {
                            return@SplashScreen
                        }
                        val destination = if (authenticated) {
                            AppGraph.Main
                        } else {
                            AppGraph.Auth
                        }

                        navController.navigate(destination) {
                            // Remove a Splash após concluir a abertura.
                            popUpTo<AppRoute.Splash> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }

            authNavGraph(
                sessionState = sessionState,
                navController = navController,
                loginViewModelFactory = loginViewModelFactory,
                passwordRecoveryViewModelFactory = passwordRecoveryViewModelFactory,
                registrationViewModelFactory = registrationViewModelFactory,
                confirmationState = emailConfirmationState,
                onConfirmationEmailChange = onConfirmationEmailChange,
                onResendConfirmation = onResendConfirmation,
                onEnterMain = {
                    if (recoveryLinkState.value != RecoveryLinkUiState.Idle) {
                        return@authNavGraph
                    }
                    privateOwnerId = knownUserId(sessionState?.value)
                    sessionNotice = null
                    // Abre a área principal após o login bem-sucedido.
                    navController.navigate(AppGraph.Main) {
                        // Ao entrar, remove todo o fluxo de autenticação.
                        popUpTo<AppGraph.Auth> {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )

            mainNavGraph(
                navController = navController,
                schedulingViewModelFactory = schedulingViewModelFactory,
                profileViewModelFactory = profileViewModelFactory,
                editProfileViewModelFactory = editProfileViewModelFactory,
                changePasswordViewModelFactory = changePasswordViewModelFactory,
                appointmentsViewModelFactory = appointmentsViewModelFactory,
                appointmentDetailViewModelFactory = appointmentDetailViewModelFactory,
                logoutViewModelFactory = logoutViewModelFactory,
                campaignsViewModelFactory = campaignsViewModelFactory,
                campaignDetailViewModelFactory = campaignDetailViewModelFactory,
                homeViewModelFactory = homeViewModelFactory,
                bloodStockViewModelFactory = bloodStockViewModelFactory,
                donationHistoryViewModelFactory = donationHistoryViewModelFactory,
                centersViewModelFactory = centersViewModelFactory,
                centerDetailViewModelFactory = centerDetailViewModelFactory,
                onSignedOut = {
                    if (recoveryLinkState.value == RecoveryLinkUiState.Idle && privateAreaNow()) {
                        navController.navigate(AppGraph.Auth) {
                            // Remove as telas da conta que acabou de sair.
                            popUpTo<AppGraph.Main> {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                        privateOwnerId = null
                        sessionNotice = null
                    }
                },
                onNavigateMain = ::navigateMain,
                onFinishScheduling = {
                    if (recoveryLinkState.value == RecoveryLinkUiState.Idle && privateAreaNow()) {
                        navController.navigate(AppRoute.Home) {
                            // Encerra o percurso visual de agendamento.
                            // Voltar não deve reabrir as etapas concluídas.
                            popUpTo<AppGraph.Main> {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        if (hidePrivateArea) Box(Modifier.fillMaxSize().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {},
        )) { SessionWaitScreen(monitoredSession is SessionMonitorUiState.Reconnecting) }
        if (monitoredSession is SessionMonitorUiState.Reconnecting && !hidePrivateArea && privateAreaNow() &&
            currentRecoveryLinkState == RecoveryLinkUiState.Idle) {
            Snackbar(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp)) {
                Text("Estamos tentando renovar sua sessão. Aguarde e tente novamente.")
            }
        }
    }
    if (sessionNotice != null && currentRecoveryLinkState == RecoveryLinkUiState.Idle && confirmation.link == ConfirmationLinkState.Idle) {
        AlertDialog(onDismissRequest = { sessionNotice = null }, title = { Text("Entre novamente") },
            text = { Text(sessionNotice.orEmpty()) }, confirmButton = {
                TextButton(onClick = { sessionNotice = null }) { Text("Entendi") }
            })
    }
    if (confirmation.link != ConfirmationLinkState.Idle && currentRecoveryLinkState == RecoveryLinkUiState.Idle) {
        EmailConfirmationDialog(confirmation.link, onRetryConfirmation, onClose = {
            if (recoveryLinkState.value == RecoveryLinkUiState.Idle) {
                // Preserva uma conta já conectada; confirmação nunca troca a sessão do app.
                val privateArea = navController.currentDestination?.hierarchy?.any {
                    it.hasRoute<AppGraph.Main>() || it.hasRoute<AppGraph.Scheduling>()
                } == true
                if (!privateArea) navController.navigate(AppRoute.Login) {
                    popUpTo(navController.graph.id) { inclusive = false }
                    launchSingleTop = true
                }
                onDismissConfirmation()
            }
        })
    }
}
