package br.edu.fatec.doesangue.presentation.common

import br.edu.fatec.doesangue.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class DataErrorMessagesTest {
    @Test fun expectedCausesHaveDistinctPortugueseMessagesAndUnknownKeepsContext() {
        val fallback="Falha ao consultar os dados fictícios."
        val messages=DataError.entries.filter { it!=DataError.UNKNOWN }.map { DataFailure(it).toDataMessage(fallback) }
        assertEquals(messages.size,messages.toSet().size)
        assertFalse(messages.any { it==fallback || it.contains("SQL") || it.contains("P0001") })
        assertEquals(fallback,DataFailure(DataError.UNKNOWN).toDataMessage(fallback))
        assertEquals(fallback,IllegalStateException("Synthetic private SQL/token").toDataMessage(fallback))
        assertTrue(DataFailure(DataError.SLOT_FULL).toDataMessage(fallback).contains("sem vagas"))
        assertTrue(DataFailure(DataError.CANCELLATION_CLOSED).toDataMessage(fallback).contains("prazo para cancelar terminou"))
    }

    @Test fun readStateKeepsValuesAndUsesTypedErrorWithoutLeakingException() {
        assertEquals(ReadUiState.Loaded(listOf(1,2)),Result.success(listOf(1,2)).toReadState("fallback"))
        assertEquals(ReadUiState.Error(DataFailure(DataError.NETWORK).toDataMessage("fallback")),
            Result.failure<List<Int>>(DataFailure(DataError.NETWORK)).toReadState("fallback"))
        assertEquals(ReadUiState.Error("fallback"),Result.failure<Int>(IllegalStateException("private")).toReadState("fallback"))
    }

    @Test fun profileKeepsItsExistingContextAndDoesNotMisdiagnosePermissionAsSession() {
        val denied=ProfileFailure(ProfileError.ACCESS_DENIED).toDataMessage("fallback")
        assertTrue(denied.contains("permissão")); assertFalse(denied.contains("Entre novamente"))
        assertTrue(ProfileFailure(ProfileError.SCHEMA_NOT_READY).toDataMessage("fallback").contains("serviço de perfil"))
        assertEquals("fallback",ProfileFailure(ProfileError.UNKNOWN).toDataMessage("fallback"))
    }
}
