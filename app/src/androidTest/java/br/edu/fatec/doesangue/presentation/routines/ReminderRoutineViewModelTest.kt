package br.edu.fatec.doesangue.presentation.routines

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.ui.navigation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class ReminderRoutineViewModelTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val stores=mutableListOf<ViewModelStore>()
    private val a=AuthenticatedUser("synthetic-a")
    private val presence=MutableStateFlow<SessionPresence>(SessionPresence.Authenticated(a))
    private val repository=FakeReminderRoutineRepository()
    private var source:ReminderRoutineRepository=repository
    private val clock=Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"),ZoneOffset.UTC)
    private fun main(block:()->Unit) {instrumentation.runOnMainSync(block);instrumentation.waitForIdleSync()}
    private fun await(test:()->Boolean) {
        val end=android.os.SystemClock.uptimeMillis()+8_000
        while(!test() && android.os.SystemClock.uptimeMillis()<end) {Thread.sleep(15);instrumentation.waitForIdleSync()}
        assertTrue("Estado esperado não alcançado",test())
    }
    private fun model():ReminderRoutineViewModel {
        val store=ViewModelStore().also {stores+=it}
        val auth=object:AuthRepository by FakeAuthRepository() {override fun observeSession()=presence}
        lateinit var model:ReminderRoutineViewModel
        main {model=ViewModelProvider(store,viewModelFactory {initializer {ReminderRoutineViewModel(source,auth,clock)}})[ReminderRoutineViewModel::class.java]}
        return model
    }
    @After fun close()=main {stores.forEach {it.clear()}}
    @Test fun shortExplicitOrNumericDatesSaveTheirRecognizedDate() {
        val model=model();await {model.state.value.loaded}
        for (text in listOf("1/11/2027", "11/1/2027", "1282027", "182027")) {
            main {model.updateDate(text);model.updateInterval("14");model.save()}
            await {!model.state.value.saving && model.state.value.routine != null}
            assertNull(model.state.value.dateError)
            assertEquals(br.edu.fatec.doesangue.domain.validation.RoutineDateInput.candidates(text).single(), model.state.value.routine!!.startsOn)
        }
    }
    @Test fun ambiguityCannotWriteEvenWhenOnlyOneInterpretationIsFuture() {
        val model=model();await {model.state.value.loaded};val initial=repository.calls
        for (text in listOf("1112026", "1122026")) {
            main {model.updateDate(text);model.updateInterval("14");model.save()}
            assertEquals("Esta data tem duas interpretações. Escolha uma das opções.",model.state.value.dateError)
            assertEquals(text,model.state.value.dateText)
            assertEquals(initial,repository.calls)
        }
        main {model.updateDate("1/12/2026");model.save()}
        await {model.state.value.routine != null}
        assertEquals(LocalDate.of(2026,12,1),model.state.value.routine!!.startsOn)
    }
    @Test fun explicitPastDateStillCannotBeUsedForANewRoutine() {
        val model=model();await {model.state.value.loaded};val initial=repository.calls
        main {model.updateDate("1/11/2026");model.updateInterval("14");model.save()}
        assertEquals("Escolha uma data inicial a partir de hoje.",model.state.value.dateError)
        assertEquals(initial,repository.calls)
    }
    @Test fun savePauseEditResumeDeletePreserveFieldsAndAccountScope() {
        val model=model();await {model.state.value.loaded}
        main {model.updateDate("20/11/2026");model.updateInterval("14");model.save()}
        await {model.state.value.routine!=null};assertEquals(a.id,model.state.value.routine!!.ownerId)
        main {model.setActive(false)};await {model.state.value.routine?.active==false}
        main {model.updateInterval("15");model.save()};await {model.state.value.routine?.intervalDays==15}
        assertFalse(model.state.value.routine!!.active)
        main {model.setActive(true)};await {model.state.value.routine?.active==true}
        main {model.delete()};await {model.state.value.routine==null && !model.state.value.saving}
        assertEquals("Rotina excluída.",model.state.value.message)
    }
    @Test fun invalidDatesPastDateAndIntervalNeverCallSave() {
        val model=model();await {model.state.value.loaded};val initial=repository.calls
        for(date in listOf("31/02/2026","01/11/2026","")) {
            main {model.updateDate(date);model.updateInterval("0");model.save()}
            assertNotNull(model.state.value.dateError);assertNotNull(model.state.value.intervalError)
        }
        assertEquals(initial,repository.calls)
    }
    @Test fun structuredFailureKeepsChoicesAndReloadCanResolveChangedVersion() {
        val model=model();await {model.state.value.loaded}
        repository.failure=DataFailure(DataError.ROUTINE_CHANGED)
        main {model.updateDate("20/11/2026");model.updateInterval("21");model.save()}
        await {model.state.value.error!=null}
        assertTrue(model.state.value.error!!.contains("Recarregue"));assertEquals("21",model.state.value.intervalText)
        repository.failure=null;main {model.load()};await {model.state.value.loaded}
        assertNull(model.state.value.error)
    }
    @Test fun pastExistingBaseCanBeKeptButCannotBeChangedToAnotherPastDate() {
        repository.routines[a.id]=ReminderRoutine("1",a.id,LocalDate.of(2026,10,1),8,"UTC",true,7)
        val model=model();await {model.state.value.loaded}
        main {assertNotNull(model.choice());model.updateDate("02/10/2026");assertNull(model.choice())}
    }
    @Test fun refreshKeepsChoicesButAnotherAccountAndLogoutRemovePrivateFields() {
        val model=model();await {model.state.value.loaded}
        main {model.updateDate("20/11/2026");model.updateInterval("21");presence.value=SessionPresence.RefreshPending}
        assertEquals("21",model.state.value.intervalText)
        main {presence.value=SessionPresence.Authenticated(AuthenticatedUser("synthetic-b"))};await {model.state.value.loaded && model.state.value.intervalText.isEmpty()}
        main {presence.value=SessionPresence.NotAuthenticated()};await {!model.state.value.loaded}
        assertEquals("",model.state.value.dateText)
    }
    @Test fun lateOldAccountReplyDoesNotPopulateNewAccount() {
        val pending=CompletableDeferred<Result<ReminderRoutine?>>()
        source=object:ReminderRoutineRepository by repository {
            override suspend fun get(ownerId:String)=if(ownerId==a.id)withContext(NonCancellable) {pending.await()} else Result.success(null)
        }
        try {
            val model=model();main {presence.value=SessionPresence.Authenticated(AuthenticatedUser("synthetic-b"))}
            await {model.state.value.loaded}
            main {pending.complete(Result.success(ReminderRoutine("1",a.id,LocalDate.of(2026,11,20),7,"UTC",true,1)))}
            assertNull(model.state.value.routine);assertEquals("",model.state.value.intervalText)
        } finally {pending.complete(Result.success(null))}
    }
}
