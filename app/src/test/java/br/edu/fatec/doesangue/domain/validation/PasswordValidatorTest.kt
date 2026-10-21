package br.edu.fatec.doesangue.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordValidatorTest {

    @Test
    fun acceptsPasswordAtMinimumLengthAndLonger() {
        assertTrue(PasswordValidator.isValid("Abcdef1!"))
        assertTrue(PasswordValidator.isValid("SenhaFicticia123!"))
    }

    @Test
    fun rejectsEmptyAndShortPasswordsEvenWithAllCategories() {
        assertFalse(PasswordValidator.isValid(""))
        assertFalse(PasswordValidator.isValid("Abcde1!"))
    }

    @Test
    fun rejectsEachMissingRequiredCategory() {
        listOf("abcdef1!", "ABCDEF1!", "Abcdefg!", "Abcdefg1")
            .forEach { assertFalse(PasswordValidator.isValid(it)) }
    }

    @Test
    fun whitespaceDoesNotReplaceSymbolOrGetTrimmed() {
        assertFalse(PasswordValidator.isValid("Abcdef1 "))
        assertFalse(PasswordValidator.isValid("Abcdef1\t"))
        // Preserve the existing policy: spaces are allowed when all criteria hold.
        assertTrue(PasswordValidator.isValid(" Abcde1!"))
    }
}
