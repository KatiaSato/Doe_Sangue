package br.edu.fatec.doesangue.ui.navigation

import br.edu.fatec.doesangue.domain.model.AuthenticatedUser
import br.edu.fatec.doesangue.presentation.auth.SessionMonitorUiState
import org.junit.Assert.*
import org.junit.Test

class SessionNavigationPolicyTest {
    private val a = AuthenticatedUser("synthetic-a")
    private val b = AuthenticatedUser("synthetic-b")
    private fun decide(state: SessionMonitorUiState, owner: String? = a.id) =
        sessionNavigationAction(true, false, owner, state)

    @Test fun publicRoutesAndRecoveryNeverTriggerGlobalRedirect() {
        for (state in listOf(SessionMonitorUiState.Checking, SessionMonitorUiState.Ended(false),
            SessionMonitorUiState.Ended(true), SessionMonitorUiState.Available(b))) {
            assertEquals(SessionNavigationAction.NONE, sessionNavigationAction(false, false, a.id, state))
            assertEquals(SessionNavigationAction.NONE, sessionNavigationAction(true, true, a.id, state))
        }
    }
    @Test fun definiteLossAndRequestedLogoutHaveDifferentDestinations() {
        assertEquals(SessionNavigationAction.LOGIN, decide(SessionMonitorUiState.Ended(false)))
        assertEquals(SessionNavigationAction.WELCOME, decide(SessionMonitorUiState.Ended(true)))
    }
    @Test fun unknownIdentityWaitsWithoutAssumingLogout() {
        assertEquals(SessionNavigationAction.WAIT, decide(SessionMonitorUiState.Checking))
        assertEquals(SessionNavigationAction.WAIT, decide(SessionMonitorUiState.Reconnecting(null)))
    }
    @Test fun pendingRefreshOfKnownAccountRetainsPrivateState() {
        assertEquals(SessionNavigationAction.NONE, decide(SessionMonitorUiState.Reconnecting(a)))
        assertEquals(SessionNavigationAction.NONE, decide(SessionMonitorUiState.Available(a)))
    }
    @Test fun firstOwnerIsBoundAndDifferentAccountRequiresNewEntry() {
        assertEquals(SessionNavigationAction.BIND_OWNER, decide(SessionMonitorUiState.Available(a), null))
        assertEquals(SessionNavigationAction.LOGIN_CHANGED_ACCOUNT, decide(SessionMonitorUiState.Available(b)))
        assertEquals(SessionNavigationAction.LOGIN_CHANGED_ACCOUNT, decide(SessionMonitorUiState.Reconnecting(b)))
    }
    @Test fun loginRequiresMatchingVerifiedAccountIncludingAfterObserverLag() {
        assertFalse(SessionMonitorUiState.Checking.permitsLoginUser(a.id))
        assertFalse(SessionMonitorUiState.Ended(false).permitsLoginUser(a.id))
        assertFalse(SessionMonitorUiState.Reconnecting(a).permitsLoginUser(a.id))
        assertFalse(SessionMonitorUiState.Available(b).permitsLoginUser(a.id))
        assertTrue(SessionMonitorUiState.Available(a).permitsLoginUser(a.id))
    }
}
