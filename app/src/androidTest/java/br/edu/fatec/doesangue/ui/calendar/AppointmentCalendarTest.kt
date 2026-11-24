package br.edu.fatec.doesangue.ui.calendar

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.domain.model.Appointment
import br.edu.fatec.doesangue.domain.model.AppointmentSummary
import br.edu.fatec.doesangue.domain.model.AppointmentStatus
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.presentation.appointments.AppointmentDetailUiState
import br.edu.fatec.doesangue.presentation.scheduling.AppointmentCreationUiState
import br.edu.fatec.doesangue.presentation.scheduling.SchedulingUiState
import br.edu.fatec.doesangue.ui.screens.donations.AppointmentDetailByIdScreen
import br.edu.fatec.doesangue.ui.screens.scheduling.ScheduleSuccessScreen
import java.time.Instant
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppointmentCalendarTest {
    @get:Rule val compose = createComposeRule()
    private val start = Instant.parse("2099-06-18T12:30:00Z")
    private val unit = "Unidade fictícia"
    private val booking = Appointment("reservation", "owner", "slot", AppointmentStatus.SCHEDULED, Instant.EPOCH)

    @Test
    fun editorReceivesConfirmedStartAndUnit() {
        val start = OffsetDateTime.parse("2099-06-18T09:30:00-03:00").toInstant()
        assertTrue(openAppointmentCalendar(start, "Unidade fictícia") { event ->
            assertEquals(Intent.ACTION_INSERT, event.action)
            assertEquals(CalendarContract.Events.CONTENT_URI, event.data)
            assertEquals(
                Instant.parse("2099-06-18T12:30:00Z").toEpochMilli(),
                event.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0L),
            )
            assertEquals("Agendamento Doe Sangue", event.getStringExtra(CalendarContract.Events.TITLE))
            assertEquals("Unidade fictícia", event.getStringExtra(CalendarContract.Events.EVENT_LOCATION))
            assertFalse(event.hasExtra(CalendarContract.EXTRA_EVENT_END_TIME))
        })
    }

    @Test
    fun missingEditorIsReported() {
        assertFalse(openAppointmentCalendar(Instant.EPOCH, "Unidade fictícia") {
            throw ActivityNotFoundException()
        })
    }

    @Test
    fun cancelledAndPastReservationsDoNotOfferExport() {
        compose.setContent {
            AppointmentCalendarButton(AppointmentStatus.CANCELLED, Instant.parse("2099-06-18T12:30:00Z"), "Unidade")
            AppointmentCalendarButton(AppointmentStatus.SCHEDULED, Instant.EPOCH, "Unidade")
        }
        compose.onNodeWithText("Adicionar ao calendário").assertDoesNotExist()
    }

    @Test
    fun confirmationOpensTheEditorWithTheConfirmedStart() {
        assertEditorOpens(checkActionSpacing = true) {
            ScheduleSuccessScreen(
                uiState = SchedulingUiState(
                    selectedCenter = DonationCenter("unit", unit, "", "", "", "", "America/Sao_Paulo"),
                    appointmentCreation = AppointmentCreationUiState(appointment = booking),
                ),
                onHome = {},
                confirmedStartsAt = start,
            )
        }
    }

    @Test
    fun detailOpensTheEditorWithTheConfirmedStart() {
        assertEditorOpens {
            AppointmentDetailByIdScreen(
                uiState = AppointmentDetailUiState.Loaded(
                    AppointmentSummary(booking, unit, start, "America/Sao_Paulo"),
                ),
                onRetry = {},
                onCancel = {},
                onBack = {},
                onNavigate = {},
            )
        }
    }

    private fun assertEditorOpens(
        checkActionSpacing: Boolean = false,
        screen: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        var launched: Intent? = null
        compose.setContent {
            val context = LocalContext.current
            val capturingContext = remember(context) {
                object : ContextWrapper(context) {
                    override fun startActivity(intent: Intent) {
                        launched = intent
                    }
                }
            }
            CompositionLocalProvider(LocalContext provides capturingContext, content = screen)
        }
        val calendar = compose.onNodeWithText("Adicionar ao calendário")
        calendar.performScrollTo()
        if (checkActionSpacing) {
            val home = compose.onNodeWithText("Ir para o início")
            val gap = home.getUnclippedBoundsInRoot().top - calendar.getUnclippedBoundsInRoot().bottom
            assertTrue("As ações precisam ter espaço entre si.", gap >= 11.dp)
        }
        calendar.performClick()
        compose.runOnIdle {
            val event = checkNotNull(launched)
            assertEquals(start.toEpochMilli(), event.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0L))
            assertEquals(unit, event.getStringExtra(CalendarContract.Events.EVENT_LOCATION))
        }
    }
}
