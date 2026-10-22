package br.edu.fatec.doesangue.domain.validation

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

object ProfileValidator {
    val birthDateFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun isValidPhone(value: String): Boolean = value.isBlank() ||
        (value.all { it in '0'..'9' || it in " ()-." } &&
            value.count { it in '0'..'9' } in 10..11)

    fun normalizePhone(value: String): String? {
        require(isValidPhone(value))
        return value.filter { it in '0'..'9' }.ifEmpty { null }
    }

    fun parseBirthDate(value: String): LocalDate? = try {
        LocalDate.parse(value.trim(), birthDateFormat).takeIf { it.year >= 1 }
    } catch (_: DateTimeParseException) {
        null
    }
}
