package br.edu.fatec.doesangue.ui.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.edu.fatec.doesangue.di.AppContainer
import br.edu.fatec.doesangue.domain.model.BloodType
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.format.DateTimeParseException

/** Leitura pública real do seed; execução remota explícita, sem login ou escrita. */
@RunWith(AndroidJUnit4::class)
class SupabaseBloodStockReadTest {
    @Test fun publishedStockTraversesSdkDtoAndMapper() = runBlocking {
        assumeTrue(
            "Consulta remota exige execução explícita no desenvolvimento.",
            InstrumentationRegistry.getArguments().getString("enableSupabaseReadTests") == "true",
        )
        val result = AppContainer().bloodStockRepository.getBloodStocks()
        result.exceptionOrNull()?.let { failure ->
            // Não propagar a exceção SDK inteira: ela pode conter headers de autenticação.
            val detail = when (failure) {
                is PostgrestRestException -> "code=${failure.code}"
                is SerializationException, is DateTimeParseException -> failure.message.orEmpty()
                else -> "sem detalhes de transporte"
            }
            throw AssertionError("Estoque: ${failure.javaClass.simpleName}; $detail")
        }
        val stocks = result.getOrThrow()
        assertEquals(8, stocks.size)
        assertEquals(BloodType.knownTypes.toSet(), stocks.map { it.bloodType }.toSet())
        assertTrue(stocks.all { it.centerName == "Unidade Demo A" })
        assertTrue(stocks.all { it.source == "Demonstração acadêmica Doe Sangue — estoque e histórico v1" })
    }
}
