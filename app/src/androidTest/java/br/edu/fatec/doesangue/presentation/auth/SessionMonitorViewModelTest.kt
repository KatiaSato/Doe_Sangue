package br.edu.fatec.doesangue.presentation.auth

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.AuthRepository
import br.edu.fatec.doesangue.ui.navigation.FakeAuthRepository
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class SessionMonitorViewModelTest {
    private val store = ViewModelStore()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val a = AuthenticatedUser("synthetic-a")
    private val source = MutableStateFlow<SessionPresence>(SessionPresence.Initializing)
    private lateinit var model: SessionMonitorViewModel
    private fun main(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun open(flow: Flow<SessionPresence> = source) {
        val repo = object : AuthRepository by FakeAuthRepository() { override fun observeSession() = flow }
        main { model = ViewModelProvider(store, viewModelFactory { initializer { SessionMonitorViewModel(repo) } })[SessionMonitorViewModel::class.java] }
    }
    private fun emit(value: SessionPresence) = main { source.value = value }
    @After fun close() = main { store.clear() }
    @Test fun renewalsKeepVerifiedUserAndRecoverWithoutAnyLogout() {
        open(); assertEquals(SessionMonitorUiState.Checking, model.uiState.value)
        emit(SessionPresence.Authenticated(a)); assertEquals(SessionMonitorUiState.Available(a), model.uiState.value)
        emit(SessionPresence.RefreshPending); assertEquals(SessionMonitorUiState.Reconnecting(a), model.uiState.value)
        emit(SessionPresence.Authenticated(a)); assertEquals(SessionMonitorUiState.Available(a), model.uiState.value)
    }
    @Test fun definiteLossDropsCachedOwnerAndDoesNotReuseItInLaterRetry() {
        open(); emit(SessionPresence.Authenticated(a)); emit(SessionPresence.NotAuthenticated())
        assertEquals(SessionMonitorUiState.Ended(false), model.uiState.value)
        emit(SessionPresence.RefreshPending); assertEquals(SessionMonitorUiState.Reconnecting(null), model.uiState.value)
        emit(SessionPresence.NotAuthenticated(true)); assertEquals(SessionMonitorUiState.Ended(true), model.uiState.value)
    }
    @Test fun coldRefreshHasNoInventedUserAndClearedModelIgnoresFurtherEvents() {
        open(); emit(SessionPresence.RefreshPending)
        assertEquals(SessionMonitorUiState.Reconnecting(null), model.uiState.value)
        main { store.clear() }; emit(SessionPresence.Authenticated(a))
        assertEquals(SessionMonitorUiState.Reconnecting(null), model.uiState.value)
    }
    @Test fun observerFailureRetainsOwnerAndRetriesInsteadOfEndingSession() {
        var subscriptions = 0
        open(flow {
            subscriptions++
            if (subscriptions == 1) { emit(SessionPresence.Authenticated(a)); error("synthetic internal detail") }
            emitAll(source)
        })
        assertEquals(SessionMonitorUiState.Reconnecting(a), model.uiState.value)
        main { source.value = SessionPresence.Authenticated(a) }
        val deadline = android.os.SystemClock.uptimeMillis() + 5_000
        while (subscriptions < 2 && android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(20)
        instrumentation.waitForIdleSync()
        assertEquals(2, subscriptions)
        assertEquals(SessionMonitorUiState.Available(a), model.uiState.value)
    }
}
