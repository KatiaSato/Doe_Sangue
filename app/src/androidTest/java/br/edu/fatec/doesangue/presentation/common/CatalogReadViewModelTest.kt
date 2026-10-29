package br.edu.fatec.doesangue.presentation.common

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.domain.model.*
import br.edu.fatec.doesangue.domain.repository.*
import br.edu.fatec.doesangue.presentation.appointments.*
import br.edu.fatec.doesangue.presentation.stock.BloodStockViewModel
import br.edu.fatec.doesangue.ui.navigation.FakeAppointmentRepository
import kotlinx.coroutines.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class CatalogReadViewModelTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val stores=mutableListOf<ViewModelStore>()
    private val a=DonationCenter("a","Unidade Sintética","Paulínia","","","","UTC")
    private val record=AppointmentSummary(Appointment("kept","synthetic","slot",AppointmentStatus.SCHEDULED,Instant.EPOCH),
        a.name,Instant.parse("2030-12-01T00:00:00Z"),"UTC",a.id)
    private var reads=0
    private val repository=object:AppointmentRepository by FakeAppointmentRepository() {
        override suspend fun getMyAppointments():Result<List<AppointmentSummary>> {reads++;return Result.success(listOf(record))}
    }
    private fun main(block:()->Unit){instrumentation.runOnMainSync(block);instrumentation.waitForIdleSync()}
    private fun await(check:()->Boolean) {
        val until=android.os.SystemClock.uptimeMillis()+8_000
        while(!check()&&android.os.SystemClock.uptimeMillis()<until){Thread.sleep(15);instrumentation.waitForIdleSync()}
        assertTrue("Estado esperado não alcançado",check())
    }
    private fun appointments(source:DonationCenterRepository):AppointmentsViewModel {
        val store=ViewModelStore().also {stores+=it};lateinit var result:AppointmentsViewModel
        main {result=ViewModelProvider(store,viewModelFactory {initializer {AppointmentsViewModel(repository,source)}})[AppointmentsViewModel::class.java]}
        return result
    }
    @After fun close()=main {stores.forEach {it.clear()}}
    @Test fun privateRecordsAppearBeforeCityLookupFinishesAndSurviveItsFailure() {
        val pending=CompletableDeferred<Result<List<DonationCenter>>>()
        val model=appointments(object:DonationCenterRepository{override suspend fun getCenters()=pending.await()})
        await {(model.uiState.value as? AppointmentsUiState.Loaded)?.centersLoading==true}
        assertEquals(listOf(record),(model.uiState.value as AppointmentsUiState.Loaded).appointments)
        main {pending.complete(Result.failure(IllegalStateException("synthetic")))}
        await {(model.uiState.value as? AppointmentsUiState.Loaded)?.centersError!=null}
        assertEquals(listOf(record),(model.uiState.value as AppointmentsUiState.Loaded).appointments)
        assertEquals(1,reads)
    }
    @Test fun retryCityOnlyDoesNotRefetchPrivateRecords() {
        var cityReads=0
        val model=appointments(object:DonationCenterRepository{
            override suspend fun getCenters():Result<List<DonationCenter>> =
                if(++cityReads==1)Result.failure(IllegalStateException())else Result.success(listOf(a))
        })
        await {(model.uiState.value as? AppointmentsUiState.Loaded)?.centersError!=null}
        main {model.loadCities()}
        await {(model.uiState.value as? AppointmentsUiState.Loaded)?.centers?.size==1}
        assertEquals(1,reads);assertEquals(2,cityReads)
        assertNull((model.uiState.value as AppointmentsUiState.Loaded).centersError)
    }
    @Test fun lateCancelledCatalogueCannotReplaceTheNewGeneration() {
        val stale=CompletableDeferred<Result<List<DonationCenter>>>();var calls=0
        val model=appointments(object:DonationCenterRepository{
            override suspend fun getCenters():Result<List<DonationCenter>> = if(++calls==1)withContext(NonCancellable){stale.await()}
                else Result.success(listOf(a.copy(city="Cidade Nova")))
        })
        await {calls==1}
        main {model.loadAppointments()}
        await {(model.uiState.value as? AppointmentsUiState.Loaded)?.centers?.firstOrNull()?.city=="Cidade Nova"}
        main {stale.complete(Result.success(listOf(a.copy(city="Cidade Antiga"))))}
        assertEquals("Cidade Nova",(model.uiState.value as AppointmentsUiState.Loaded).centers.single().city)
        assertEquals(2,reads)
    }
    @Test fun stockIsAvailableWhileCatalogueIsPendingAndCatalogueCanBeRetriedSeparately() {
        var stockReads=0;var cityReads=0
        val pending=CompletableDeferred<Result<List<DonationCenter>>>()
        val stock=BloodStock("a",a.name,BloodType.O_NEGATIVE,StockLevel.CRITICAL,"Fonte sintética",Instant.EPOCH,"UTC")
        val store=ViewModelStore().also {stores+=it};lateinit var model:BloodStockViewModel
        main {model=ViewModelProvider(store,viewModelFactory{initializer{
            BloodStockViewModel(object:BloodStockRepository{override suspend fun getBloodStocks():Result<List<BloodStock>>{
                stockReads++;return Result.success(listOf(stock))}},object:DonationCenterRepository{
                override suspend fun getCenters():Result<List<DonationCenter>> = if(++cityReads==1)pending.await() else Result.success(listOf(a))
            })
        }})[BloodStockViewModel::class.java]}
        await {model.uiState.value is ReadUiState.Loaded}
        assertEquals(listOf(stock),(model.uiState.value as ReadUiState.Loaded).data)
        main {pending.complete(Result.failure(IllegalStateException()))}
        await {model.centers.value is ReadUiState.Error}
        main {model.loadCities()}
        await {model.centers.value is ReadUiState.Loaded}
        assertEquals(1,stockReads);assertEquals(2,cityReads)
    }
}
