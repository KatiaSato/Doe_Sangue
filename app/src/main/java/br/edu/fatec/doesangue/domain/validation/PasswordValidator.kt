package br.edu.fatec.doesangue.domain.validation

object PasswordValidator {

    fun isValid(value: String): Boolean =
        value.length >= 8 &&
                value.any { it in 'A'..'Z' } &&
                value.any { it in 'a'..'z' } &&
                value.any { it in '0'..'9' } &&
                value.any {
                    !it.isLetterOrDigit() && !it.isWhitespace()
                }
}