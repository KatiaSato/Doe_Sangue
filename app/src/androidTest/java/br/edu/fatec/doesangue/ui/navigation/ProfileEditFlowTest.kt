package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import java.time.LocalDate
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.input.key.Key
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProfileEditFlowTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var nav: NavHostController
    private var stored = DonorProfile("test-user-1", "Doador sintético", BloodType.O_POSITIVE,
        "00900000000", LocalDate.of(2000, 2, 29))
    private var writes = 0
    private var answer: suspend (DonorProfile) -> Result<DonorProfile> = { Result.success(it) }
    private val repository = object : DonorProfileRepository by FakeDonorProfileRepository() {
        override suspend fun getCurrentProfile() = Result.success(stored)
        override suspend fun updateCurrentProfile(displayName: String, bloodType: BloodType, phone: String?, birthDate: LocalDate?): Result<DonorProfile> {
            writes++
            return answer(stored.copy(displayName = displayName, bloodType = bloodType, phone = phone, birthDate = birthDate))
                .onSuccess { stored = it }
        }
        override suspend fun updateCurrentBloodType(bloodType: BloodType): Result<DonorProfile> {
            writes++
            stored = stored.copy(bloodType = bloodType)
            return Result.success(stored)
        }
    }
    private fun open() {
        val auth = object : br.edu.fatec.doesangue.domain.repository.AuthRepository by FakeAuthRepository() {
            override suspend fun restoreSession() = Result.success(AuthenticatedUser("test-user-1"))
        }
        val container = AppContainer(routineRepositoryOverride = br.edu.fatec.doesangue.ui.navigation.FakeReminderRoutineRepository(), authRepositoryOverride = auth, donorProfileRepositoryOverride = repository,
            centerRepositoryOverride = FakeDonationCenterRepository(), timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            appointmentRepositoryOverride = FakeAppointmentRepository(), campaignRepositoryOverride = FakeCampaignRepository(),
            bloodStockRepositoryOverride = FakeBloodStockRepository(), donationHistoryRepositoryOverride = FakeDonationHistoryRepository())
        compose.setContent { nav = rememberNavController(); DoeSangueTheme { FeatureTestHost(container, nav, homeFactory = container.homeViewModelFactory) } }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Home>() == true }
        compose.onAllNodesWithText("Perfil").onLast().performClick()
        compose.waitForIdle()
    }
    private fun tap(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick(); compose.waitForIdle()
    }
    private fun replace(label: String, value: String) {
        compose.onNodeWithText(label).performScrollTo().performTextReplacement(value); compose.waitForIdle()
    }
    private fun edit() { tap("Configurações"); tap("Editar perfil") }
    private fun capture(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "profile-edit-validation").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    @Test fun bloodTypeOptionsRequireExplicitEditingAndCancelDiscardsSelection() {
        open()
        capture("meu-perfil")
        compose.onNodeWithText("O+").assertDoesNotExist()
        compose.onNodeWithText("Salvar tipo sanguíneo").assertDoesNotExist()
        tap("Alterar tipo sanguíneo"); tap("AB-"); tap("Cancelar")
        compose.onNodeWithText("Tipo sanguíneo informado: O+").assertExists()
        compose.onNodeWithText("AB-").assertDoesNotExist()
        tap("Alterar tipo sanguíneo")
        compose.onNodeWithText("O+").assertIsSelected()
        tap("Não sei"); tap("Salvar tipo sanguíneo")
        compose.onNodeWithText("Tipo sanguíneo informado: Não sei").assertExists()
        compose.onNodeWithText("Salvar tipo sanguíneo").assertDoesNotExist()
        assertEquals(1, writes)
    }

    @Test fun completeEditClearsOptionalFieldsReturnsAndRefreshesProfileAndHome() {
        open(); edit()
        capture("editar-perfil")
        compose.onNodeWithText("Nome de exibição").assertTextContains("Doador sintético")
        compose.onNodeWithText("Data de nascimento (opcional)").assertTextContains("29/02/2000")
        compose.onNodeWithText("Salvar alterações").assertIsNotEnabled()
        replace("Nome de exibição", "  Doador atualizado  ")
        replace("Telefone com DDD (opcional)", "")
        replace("Data de nascimento (opcional)", "")
        tap("Não sei"); tap("Salvar alterações")
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Settings>() == true }
        compose.onNodeWithText("‹").performClick(); compose.waitForIdle()
        compose.onNodeWithText("Doador atualizado").assertExists()
        compose.onNodeWithText("Telefone: Não informado").assertExists()
        compose.onNodeWithText("Nascimento: Não informado").assertExists()
        assertNull(stored.phone); assertNull(stored.birthDate); assertEquals(1, writes)
        compose.onAllNodesWithText("Início").onLast().performClick(); compose.waitForIdle()
        compose.onNodeWithText("Olá, Doador atualizado").assertExists()
    }

    @Test fun invalidValuesStayInFormAndCancelReopensPersistedValues() {
        open(); edit()
        replace("Nome de exibição", "  ")
        replace("Telefone com DDD (opcional)", "123")
        replace("Data de nascimento (opcional)", "31/02/2000")
        tap("Salvar alterações")
        compose.onNodeWithText("Informe o nome de exibição.").assertExists()
        compose.onNodeWithText("Informe uma data válida no formato DD/MM/AAAA.").assertExists()
        assertEquals(0, writes)
        tap("Cancelar"); tap("Editar perfil")
        compose.onNodeWithText("Nome de exibição").assertTextContains("Doador sintético")
        compose.onNodeWithText("Telefone com DDD (opcional)").assertTextContains("00900000000")
        compose.onNodeWithText("O+").assertIsSelected()
    }

    @Test fun pendingSaveBlocksFieldsBackAndDuplicateThenFailurePreservesDraftForRetry() {
        val pending = CompletableDeferred<Result<DonorProfile>>()
        answer = { pending.await() }
        open(); edit()
        replace("Nome de exibição", "Doador alterado")
        tap("Salvar alterações")
        compose.onNodeWithText("Salvando perfil…").assertIsNotEnabled()
        compose.onNodeWithText("Nome de exibição").assertIsNotEnabled()
        compose.onNodeWithText("Cancelar").assertIsNotEnabled()
        compose.onNodeWithText("‹").assertDoesNotExist()
        compose.onNodeWithText("A+").assertIsNotEnabled()
        compose.runOnIdle { pending.complete(Result.failure(ProfileFailure(ProfileError.NETWORK))) }
        compose.waitForIdle()
        compose.onNodeWithText("Verifique sua conexão e tente novamente.").assertExists()
        compose.onNodeWithText("Nome de exibição").assertTextContains("Doador alterado")
        compose.runOnIdle { answer = { Result.success(it) } }
        tap("Salvar alterações")
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Settings>() == true }
        assertEquals(2, writes)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun birthDateAcceptsDigitsPasteCursorEditingAndClearing() {
        open(); edit()
        val date = compose.onNodeWithText("Data de nascimento (opcional)")
        replace("Data de nascimento (opcional)", "")
        date.performTextInput("08071987")
        date.assertTextContains("08/07/1987")
        // Cursor à direita do primeiro dígito: apagar e reinserir sem editar as barras.
        date.performTextInputSelection(TextRange(1))
        date.performKeyInput { pressKey(Key.Backspace) }
        date.performTextInput("0")
        date.assertTextContains("08/07/1987")
        tap("Salvar alterações")
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Settings>() == true }
        assertEquals(LocalDate.of(1987, 7, 8), stored.birthDate)
        tap("Editar perfil")
        replace("Data de nascimento (opcional)", "29/02/2000")
        date.assertTextContains("29/02/2000")
        tap("Salvar alterações")
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Settings>() == true }
        assertEquals(LocalDate.of(2000, 2, 29), stored.birthDate)
        tap("Editar perfil")
        replace("Data de nascimento (opcional)", "")
        tap("Salvar alterações")
        compose.waitUntil(10_000) { nav.currentDestination?.hasRoute<AppRoute.Settings>() == true }
        assertNull(stored.birthDate)
        assertEquals(3, writes)
    }
}
