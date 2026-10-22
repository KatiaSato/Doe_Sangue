package br.edu.fatec.doesangue.ui.screens.profile

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BirthDateMaskTest {
    @Test fun formatsDigitsAndPastedDatesWithoutRequiringSeparators() {
        assertEquals("", formatBirthDateInput(""))
        assertEquals("08", formatBirthDateInput("08"))
        assertEquals("08/0", formatBirthDateInput("080"))
        assertEquals("08/07", formatBirthDateInput("0807"))
        assertEquals("08/07/1987", formatBirthDateInput("08071987"))
        assertEquals("08/07/1987", formatBirthDateInput("08/07/1987"))
        assertEquals("08/07/1987", formatBirthDateInput("0807198799"))
    }

    @Test fun cursorMappingsStayValidWhileTypingAndDeletingAtEveryLength() {
        for (length in 0..8) {
            val digits = "08071987".take(length)
            val transformed = BirthDateMask.filter(AnnotatedString(digits))
            val offsets = transformed.offsetMapping
            assertEquals(formatBirthDateInput(digits), transformed.text.text)
            for (offset in 0..length) {
                val mapped = offsets.originalToTransformed(offset)
                assertTrue(mapped in 0..transformed.text.length)
                assertEquals(offset, offsets.transformedToOriginal(mapped))
            }
            for (offset in 0..transformed.text.length) {
                assertTrue(offsets.transformedToOriginal(offset) in 0..length)
            }
        }
    }
}
