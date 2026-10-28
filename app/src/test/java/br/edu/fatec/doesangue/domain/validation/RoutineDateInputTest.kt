package br.edu.fatec.doesangue.domain.validation

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RoutineDateInputTest {
    @Test fun unpaddedAndPaddedExplicitDatesHaveTheSameMeaning() {
        for (text in listOf("1/11/2026", "01/11/2026", "01112026", " 1/11/2026 "))
            assertEquals(text, listOf(LocalDate.of(2026,11,1)), RoutineDateInput.candidates(text))
        assertEquals(listOf(LocalDate.of(2026,1,11)), RoutineDateInput.candidates("11/1/2026"))
    }
    @Test fun shortNumericInputIsAcceptedWhenOnlyOneInterpretationExists() {
        assertEquals(listOf(LocalDate.of(2027,8,1)), RoutineDateInput.candidates("182027"))
        assertEquals(listOf(LocalDate.of(2027,8,12)), RoutineDateInput.candidates("1282027"))
        assertEquals(listOf(LocalDate.of(2027,10,1)), RoutineDateInput.candidates("1102027"))
    }
    @Test fun ambiguousDigitsReturnBothDatesWithoutChoosingForTheUser() {
        assertEquals(listOf(LocalDate.of(2026,11,1), LocalDate.of(2026,1,11)), RoutineDateInput.candidates("1112026"))
        assertEquals(listOf(LocalDate.of(2026,12,1), LocalDate.of(2026,2,11)), RoutineDateInput.candidates("1122026"))
    }
    @Test fun invalidAndIncompleteDatesNeverRollOverOrDropCharacters() {
        for (text in listOf("", "11120", "111202666", "31/4/2027", "29/2/2027", "2922027", "11/1/26", "1/1/0000", "2027-01-11", "1112026x", "１１１２０２６"))
            assertTrue(text, RoutineDateInput.candidates(text).isEmpty())
        assertEquals(listOf(LocalDate.of(2028,2,29)), RoutineDateInput.candidates("2922028"))
    }
}
