package br.edu.fatec.doesangue.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import br.edu.fatec.doesangue.presentation.profile.ProfileViewModel
import br.edu.fatec.doesangue.ui.theme.DoeSangueTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Usa o grafo, a tela e o ViewModel reais; os dados ficam apenas em memória. */
@RunWith(AndroidJUnit4::class)
class ProfileFlowTest {
    @get:Rule
    val compose = createComposeRule()

    private val repository = ControlledProfileRepository()
    private lateinit var nav: NavHostController
    private lateinit var model: ProfileViewModel

    private fun openProfile() {
        val container = AppContainer(
            centerRepositoryOverride = FakeDonationCenterRepository(),
            timeSlotRepositoryOverride = FakeDonationTimeSlotRepository(),
            authRepositoryOverride = FakeAuthRepository(),
            donorProfileRepositoryOverride = repository,
        )
        val profileFactory = viewModelFactory {
            initializer { ProfileViewModel(repository).also { model = it } }
        }
        compose.setContent {
            nav = rememberNavController()
            DoeSangueTheme {
                AppNavHost(
                    navController = nav,
                    schedulingViewModelFactory = container.schedulingViewModelFactory,
                    loginViewModelFactory = container.loginViewModelFactory,
                    profileViewModelFactory = profileFactory,
                )
            }
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onNodeWithText("Já tenho uma conta").performClick()
        compose.onNodeWithText("E-mail").performTextInput("teste@example.com")
        compose.onNodeWithText("Senha").performTextInput("senha-ficticia")
        compose.onNodeWithText("Entrar").performScrollTo().performClick()
        compose.waitForIdle()
        openProfileTab()
    }

    private fun openProfileTab() {
        compose.onAllNodesWithText("Perfil").onLast().performClick()
        compose.waitForIdle()
    }

    private fun createProfile() {
        compose.onNodeWithText("Criar perfil").performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun emptyAndWhitespaceNamesDoNotSendCreation() {
        openProfile()
        createProfile()
        compose.onNodeWithText("Informe o nome de exibição.").assertExists()
        compose.onNodeWithText("Nome de exibição").performTextInput("   ")
        compose.onNodeWithText("Informe o nome de exibição.").assertDoesNotExist()
        createProfile()
        compose.onNodeWithText("Informe o nome de exibição.").assertExists()
        compose.runOnIdle { assertEquals(0, repository.creations) }
    }

    @Test
    fun creationDisplaysNormalizedNameAndReturningToProfileReadsItAgain() {
        openProfile()
        compose.onNodeWithText("Nome de exibição").performTextInput("  Doador Demo A  ")
        createProfile()
        compose.onNodeWithText("Doador Demo A").assertExists()
        compose.onNodeWithText("Criar perfil").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals("Doador Demo A", repository.lastName)
            model.createProfile()
            assertEquals(1, repository.creations)
        }
        compose.onNodeWithText("‹").performClick()
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(nav.currentDestination?.hasRoute<AppRoute.Home>() == true) }
        openProfileTab()
        compose.onNodeWithText("Doador Demo A").assertExists()
        compose.runOnIdle {
            assertEquals(2, repository.reads)
            assertEquals(1, repository.creations)
        }
    }

    @Test
    fun failedCreationPreservesInputAndAllowsSuccessfulRetry() {
        repository.createAnswer = { Result.failure(IllegalStateException("detalhe-interno-sintetico")) }
        openProfile()
        compose.onNodeWithText("Nome de exibição").performTextInput("Doador Demo A")
        createProfile()
        compose.onNodeWithText("Não foi possível concluir a criação do perfil.").assertExists()
        compose.onNodeWithText("Nome de exibição").assertTextContains("Doador Demo A")
        compose.onNodeWithText("detalhe-interno-sintetico").assertDoesNotExist()
        compose.onNodeWithText("Criar perfil").assertIsEnabled()
        compose.runOnIdle {
            repository.createAnswer = { name -> Result.success(DonorProfile("test-user-1", name)) }
        }
        createProfile()
        compose.onNodeWithText("Doador Demo A").assertExists()
        compose.onNodeWithText("Não foi possível concluir a criação do perfil.").assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, repository.creations) }
    }

    @Test
    fun pendingCreationBlocksChangesDuplicateWritesAndCompetingReads() {
        val response = CompletableDeferred<Result<DonorProfile>>()
        repository.createAnswer = { response.await() }
        openProfile()
        compose.onNodeWithText("Nome de exibição").performTextInput("Doador Demo A")
        createProfile()
        compose.onNodeWithText("Salvando...").assertIsNotEnabled()
        compose.onNodeWithText("Nome de exibição").assertIsNotEnabled()
        compose.runOnIdle {
            model.createProfile()
            model.updateDisplayName("Outro nome")
            model.loadProfile()
            assertEquals(1, repository.creations)
            assertEquals(1, repository.reads)
            assertEquals("Doador Demo A", repository.lastName)
            response.complete(Result.success(DonorProfile("test-user-1", "Doador Demo A")))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Doador Demo A").assertExists()
        compose.onNodeWithText("Salvando...").assertDoesNotExist()
    }

    @Test
    fun pendingReadThenFailureAllowsRetryToMissingProfileForm() {
        val response = CompletableDeferred<Result<DonorProfile?>>()
        repository.loadAnswer = { response.await() }
        openProfile()
        compose.onNodeWithText("Carregando perfil...").assertExists()
        compose.onNodeWithText("Criar perfil").assertDoesNotExist()
        compose.runOnIdle {
            model.loadProfile()
            assertEquals(1, repository.reads)
            response.complete(Result.failure(IllegalStateException("detalhe-interno-sintetico")))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Não foi possível carregar o perfil. Tente novamente.").assertExists()
        compose.onNodeWithText("detalhe-interno-sintetico").assertDoesNotExist()
        compose.runOnIdle { repository.loadAnswer = { Result.success(null) } }
        compose.onNodeWithText("Tentar novamente").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Você ainda não criou seu perfil.").assertExists()
        compose.onNodeWithText("Nome de exibição").assertExists()
        compose.runOnIdle { assertEquals(2, repository.reads) }
    }

    private class ControlledProfileRepository : DonorProfileRepository {
        var reads = 0
        var creations = 0
        var lastName: String? = null
        private var storedProfile: DonorProfile? = null
        var loadAnswer: suspend () -> Result<DonorProfile?> = { Result.success(storedProfile) }
        var createAnswer: suspend (String) -> Result<DonorProfile> = { name ->
            Result.success(DonorProfile("test-user-1", name))
        }

        override suspend fun getCurrentProfile(): Result<DonorProfile?> {
            reads++
            return loadAnswer()
        }

        override suspend fun createCurrentProfile(displayName: String): Result<DonorProfile> {
            creations++
            lastName = displayName
            return createAnswer(displayName).also { result ->
                result.onSuccess { storedProfile = it }
            }
        }
    }
}
