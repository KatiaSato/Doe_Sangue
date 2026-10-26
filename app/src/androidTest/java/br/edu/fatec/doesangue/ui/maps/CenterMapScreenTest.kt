package br.edu.fatec.doesangue.ui.maps

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.model.GeoCoordinates
import br.edu.fatec.doesangue.presentation.centers.CenterDetailUiState
import br.edu.fatec.doesangue.ui.screens.centers.CenterDetailReadScreen
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CenterMapScreenTest {
    @get:Rule val compose = createComposeRule()
    private val center = DonationCenter("a", "Unidade Sintética A", "Cidade fictícia",
        "Endereço fictício", "", "", "America/Sao_Paulo", GeoCoordinates(-22.9, -47.05))

    @Test fun mapClickPassesCurrentUnitAndKeepsSchedulingIndependent() {
        val state = mutableStateOf<CenterDetailUiState>(CenterDetailUiState.Loaded(center))
        val opened = mutableListOf<DonationCenter>()
        var schedules = 0
        compose.setContent {
            DoeSangueTheme {
                CenterDetailReadScreen(state.value, {}, {}, { schedules++ }, {}, { opened += it })
            }
        }
        compose.onNodeWithText("Posição fictícia para demonstração.").assertExists()
        compose.onNodeWithText("Abrir no mapa").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(center), opened); assertEquals(0, schedules) }
        val another = center.copy(id = "c", name = "Unidade Sintética C", coordinates = GeoCoordinates(0.0, 0.0))
        compose.runOnIdle { state.value = CenterDetailUiState.Loaded(another) }
        compose.onNodeWithText("Abrir no mapa").performScrollTo().performClick()
        compose.onNodeWithText("Agendar doação").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(center, another), opened); assertEquals(1, schedules) }
    }

    @Test fun absentPositionShowsFallbackWithoutMapAction() {
        compose.setContent {
            DoeSangueTheme {
                CenterDetailReadScreen(CenterDetailUiState.Loaded(center.copy(coordinates = null)),
                    {}, {}, {}, {}, { fail("Não deve abrir mapa sem posição") })
            }
        }
        compose.onNodeWithText("Localização não cadastrada.").assertExists()
        compose.onNodeWithText("Abrir no mapa").assertDoesNotExist()
        compose.onNodeWithText("Posição fictícia para demonstração.").assertDoesNotExist()
        compose.onNodeWithText("Agendar doação").assertExists()
    }

    @Test fun loadingErrorAndUnavailableStatesDoNotOfferMap() {
        val state = mutableStateOf<CenterDetailUiState>(CenterDetailUiState.Loading)
        compose.setContent {
            DoeSangueTheme { CenterDetailReadScreen(state.value, {}, {}, {}, {}, { fail("Sem unidade carregada") }) }
        }
        listOf(CenterDetailUiState.Loading, CenterDetailUiState.Error("Falha fictícia"),
            CenterDetailUiState.NotFound).forEach { next ->
            compose.runOnIdle { state.value = next }
            compose.onNodeWithText("Abrir no mapa").assertDoesNotExist()
        }
    }
}
