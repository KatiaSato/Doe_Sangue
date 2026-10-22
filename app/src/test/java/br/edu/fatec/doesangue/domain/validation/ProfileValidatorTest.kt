package br.edu.fatec.doesangue.domain.validation

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ProfileValidatorTest {
    @Test fun optionalPhoneCanBeClearedAndFormattingIsRemoved() {
        assertNull(ProfileValidator.normalizePhone("  "))
        assertEquals("00900000000", ProfileValidator.normalizePhone("(00) 90000-0000"))
        assertEquals("0000000000", ProfileValidator.normalizePhone("(00) 0000-0000"))
    }
    @Test fun invalidPhoneCharactersAndLengthsAreRejected() {
        for (input in listOf("123", "000000000000", "telefone0000000000", "()", "１２３４５６７８９０")) {
            assertFalse(input, ProfileValidator.isValidPhone(input))
            assertThrows(IllegalArgumentException::class.java) { ProfileValidator.normalizePhone(input) }
        }
    }
    @Test fun calendarDatesAreStrictAndLeapYearsAreRespected() {
        assertEquals(LocalDate.of(2000, 2, 29), ProfileValidator.parseBirthDate("29/02/2000"))
        for (input in listOf("31/02/2000", "29/02/2001", "2020-01-01", "1/1/2000", "", "00/01/2000", "01/01/0000")) {
            assertNull(input, ProfileValidator.parseBirthDate(input))
        }
    }
}
