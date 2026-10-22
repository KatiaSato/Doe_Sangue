package br.edu.fatec.doesangue.presentation.profile

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.DonorProfileRepository
import br.edu.fatec.doesangue.ui.navigation.FakeDonorProfileRepository
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditProfileViewModelTest {
    private val store = ViewModelStore()
    private lateinit var model: EditProfileViewModel
    private val original = DonorProfile("test-user-1", "Doador sintético")
    private var calls = 0
    private var answer: suspend (DonorProfile) -> Result<DonorProfile> = { Result.success(it) }
    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun open(load: Result<DonorProfile?> = Result.success(original)) {
        val repository = object : DonorProfileRepository by FakeDonorProfileRepository() {
            override suspend fun getCurrentProfile() = load
            override suspend fun updateCurrentProfile(displayName: String, bloodType: BloodType, phone: String?, birthDate: LocalDate?): Result<DonorProfile> {
                calls++
                return answer(original.copy(displayName = displayName, bloodType = bloodType, phone = phone, birthDate = birthDate))
            }
        }
        main { model = ViewModelProvider(store, viewModelFactory { initializer { EditProfileViewModel(repository) } })[EditProfileViewModel::class.java] }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
    @After fun close() = main { store.clear() }

    @Test fun invalidInputsNeverReachRepositoryAndFieldsHaveSpecificErrors() {
        open()
        main {
            model.updateName("  ")
            model.updatePhone("abc")
            model.updateBirthDate("31/02/2000")
            model.save()
            val state = model.uiState.value as EditProfileUiState.Ready
            assertNotNull(state.nameError); assertNotNull(state.phoneError); assertNotNull(state.birthDateError)
            model.updateName("Doador sintético")
            model.updatePhone("")
            model.updateBirthDate(LocalDate.now().plusDays(1).format(br.edu.fatec.doesangue.domain.validation.ProfileValidator.birthDateFormat))
            model.save()
            assertEquals("A data de nascimento não pode estar no futuro.", (model.uiState.value as EditProfileUiState.Ready).birthDateError)
            assertEquals(0, calls)
        }
    }

    @Test fun normalizationDuplicateGuardAndClearingOptionalFieldsWork() {
        val pending = CompletableDeferred<Result<DonorProfile>>()
        var sent: DonorProfile? = null
        answer = { sent = it; pending.await() }
        open()
        main {
            model.save(); assertEquals(0, calls)
            model.updateName("  Doador atualizado  ")
            model.updatePhone("(00) 90000-0000")
            model.updateBirthDate("29/02/2000")
            model.updateBloodType(BloodType.UNKNOWN)
            model.save(); model.save(); model.load(); model.updateName("Outro")
            assertEquals(1, calls)
            assertEquals("Doador atualizado", sent!!.displayName)
            assertEquals("00900000000", sent!!.phone)
            assertEquals(LocalDate.of(2000, 2, 29), sent!!.birthDate)
            pending.complete(Result.success(sent!!))
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        main { assertTrue((model.uiState.value as EditProfileUiState.Ready).saved); model.save(); assertEquals(1, calls) }
    }

    @Test fun failurePreservesDraftAndRetryConfirmsOnlyMatchingProfile() {
        answer = { Result.failure(ProfileFailure(ProfileError.NETWORK)) }
        open()
        main { model.updateName("Doador alterado"); model.save() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        main {
            val state = model.uiState.value as EditProfileUiState.Ready
            assertEquals("Doador alterado", state.displayName)
            assertEquals("Verifique sua conexão e tente novamente.", state.errorMessage)
            answer = { Result.success(it.copy(id = "outro-doador")) }
            model.save()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        main {
            assertFalse((model.uiState.value as EditProfileUiState.Ready).saved)
            answer = { Result.success(it) }; model.save()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        main { assertTrue((model.uiState.value as EditProfileUiState.Ready).saved) }
    }

    @Test fun missingProfileIsNotPresentedAsEditable() {
        open(Result.success(null))
        assertEquals(EditProfileUiState.Missing, model.uiState.value)
    }

    @Test fun loadFailureUsesSpecificSessionMessage() {
        open(Result.failure(ProfileFailure(ProfileError.SESSION_UNAVAILABLE)))
        assertEquals(EditProfileUiState.Error("Sua sessão não está disponível. Entre novamente para continuar."), model.uiState.value)
    }

    @Test fun lateSaveAfterViewModelClearedCannotProduceSuccess() {
        val pending = CompletableDeferred<Result<DonorProfile>>()
        answer = { profile -> withContext(NonCancellable) { pending.await() }; Result.success(profile) }
        open()
        main { model.updateName("Doador alterado"); model.save(); store.clear(); pending.complete(Result.success(original)) }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertFalse((model.uiState.value as EditProfileUiState.Ready).saved)
    }
}
