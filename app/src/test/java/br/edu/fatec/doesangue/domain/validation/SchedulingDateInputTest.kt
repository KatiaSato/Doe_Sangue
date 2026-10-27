package br.edu.fatec.doesangue.domain.validation

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class SchedulingDateInputTest {
    @Test fun acceptsSingleOrDoubleDigitDayAndMonthWithoutChangingTheDate() {
        for (value in listOf("12/8/2027","12/08/2027","12082027"," 12/8/2027 "))
            assertEquals(value,LocalDate.of(2027,8,12),SchedulingDateInput.parse(value))
        assertEquals(LocalDate.of(2027,8,2),SchedulingDateInput.parse("2/8/2027"))
        assertEquals(LocalDate.of(2027,8,2),SchedulingDateInput.parse("02/08/2027"))
    }
    @Test fun rejectsNonexistentDatesInsteadOfRollingToTheNextMonth() {
        for (value in listOf("31/4/2027","29/2/2027","31/13/2027","00/8/2027","12/0/2027","12/8/0000"))
            assertNull(value,SchedulingDateInput.parse(value))
        assertEquals(LocalDate.of(2028,2,29),SchedulingDateInput.parse("29/2/2028"))
    }
    @Test fun rejectsIncompleteAmbiguousOrUnexpectedFormats() {
        for (value in listOf("","12/8/27","12/8/202","1282027","2027-08-12","12/8/2027x","１２/８/２０２７","12//8/2027"))
            assertNull(value,SchedulingDateInput.parse(value))
    }
}
