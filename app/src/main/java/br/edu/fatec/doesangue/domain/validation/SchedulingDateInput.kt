package br.edu.fatec.doesangue.domain.validation

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Barras explicitam os limites: mês 8 e 08 representam a mesma data. */
object SchedulingDateInput {
    private val separated = Regex("[0-9]{1,2}/[0-9]{1,2}/[0-9]{4}")
    private val digits = Regex("[0-9]{8}")
    private val separatedFormat = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)
    private val digitsFormat = DateTimeFormatter.ofPattern("ddMMuuuu").withResolverStyle(ResolverStyle.STRICT)

    fun parse(value: String): LocalDate? {
        val text = value.trim()
        val format = when {
            separated.matches(text) -> separatedFormat
            digits.matches(text) -> digitsFormat
            else -> return null
        }
        return try {
            LocalDate.parse(text, format).takeIf { it.year in 1..9999 }
        } catch (_: DateTimeParseException) { null }
    }
}
