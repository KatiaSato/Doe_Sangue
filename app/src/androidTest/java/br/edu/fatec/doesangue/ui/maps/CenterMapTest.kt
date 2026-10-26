package br.edu.fatec.doesangue.ui.maps

import android.app.Activity
import android.app.Instrumentation
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.model.GeoCoordinates
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/** Sem rede ou aplicativo externo: grava chamadas ou bloqueia a saída pela instrumentação. */
@RunWith(AndroidJUnit4::class)
class CenterMapTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val center = DonationCenter(
        id = "mapa-sintetico", name = "Unidade Sintética A & B #1 (demo)",
        city = "Cidade fictícia", address = "Endereço fictício", openingHours = "",
        phone = "", timeZoneId = "America/Sao_Paulo",
        coordinates = GeoCoordinates(-22.9, -47.05),
    )

    @Before fun foreground() {
        compose.setContent { Text("Teste de mapa demonstrativo") }
        compose.waitForIdle()
    }

    @Test fun absentPositionDoesNotCreateAnIntent() {
        assertNull(createCenterMapIntent(center.copy(coordinates = null)))
    }

    @Test fun encodedQueryPreservesLabelAndAxesRegardlessOfLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val intent = requireNotNull(createCenterMapIntent(center))
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertNull(intent.`package`)
            assertNull(intent.component)
            val uri = requireNotNull(intent.data)
            assertEquals("geo", uri.scheme)
            val text = uri.toString()
            assertTrue(text.startsWith("geo:-22.9,-47.05?q="))
            assertFalse(text.contains(" "))
            assertFalse(text.contains("#"))
            assertEquals(
                "-22.9,-47.05 (Demonstração: ${center.name})",
                Uri.decode(text.substringAfter("?q=")),
            )
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun zeroAndBoundaryPositionsAreNotTreatedAsMissing() {
        listOf(GeoCoordinates(0.0, 0.0), GeoCoordinates(-90.0, -180.0),
            GeoCoordinates(90.0, 180.0)).forEach { position ->
            val intent = requireNotNull(createCenterMapIntent(center.copy(coordinates = position)))
            assertTrue(intent.dataString!!.startsWith("geo:${position.latitude},${position.longitude}?q="))
        }
    }

    @Test fun nonActivityContextStartsOnlyChosenPositionWithNewTask() {
        val context = RecordingContext(compose.activity)
        compose.runOnIdle { openCenterMap(context, center) }
        assertEquals(1, context.started.size)
        val intent = context.started.single()
        assertEquals(createCenterMapIntent(center)?.data, intent.data)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test @SdkSuppress(minSdkVersion = 26)
    fun activityContextUsesItsTaskWithoutActuallyOpeningAnotherApp() {
        var captured: Intent? = null
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.data?.scheme != "geo") return null
                captured = Intent(intent)
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            compose.runOnIdle { openCenterMap(compose.activity, center) }
            assertNotNull(captured)
            assertEquals(0, captured!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
            assertEquals(createCenterMapIntent(center)?.data, captured!!.data)
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test fun missingPositionShowsSpecificToastWithoutStartingActivity() {
        val context = RecordingContext(compose.activity)
        observeToast("Localização não cadastrada.") {
            openCenterMap(context, center.copy(coordinates = null))
        }
        assertTrue(context.started.isEmpty())
    }

    @Test fun missingMapAppShowsSpecificToast() {
        val context = RecordingContext(compose.activity, ActivityNotFoundException("falha fictícia"))
        observeToast("Nenhum aplicativo de mapas disponível.") { openCenterMap(context, center) }
        assertEquals(1, context.started.size)
    }

    @Test fun deniedLaunchShowsSpecificToast() {
        val context = RecordingContext(compose.activity, SecurityException("falha fictícia"))
        observeToast("O aparelho não permitiu abrir o mapa.") { openCenterMap(context, center) }
        assertEquals(1, context.started.size)
    }

    @Suppress("DEPRECATION")
    private fun observeToast(message: String, action: () -> Unit) {
        val event = instrumentation.uiAutomation.executeAndWaitForEvent(
            { compose.runOnIdle(action) },
            { it.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
                it.text.any { text -> text.toString() == message } },
            5_000,
        )
        event.recycle()
    }

    private class RecordingContext(base: Context, private val failure: RuntimeException? = null) :
        ContextWrapper(base) {
        val started = mutableListOf<Intent>()
        override fun startActivity(intent: Intent) {
            started += Intent(intent)
            failure?.let { throw it }
        }
    }
}
