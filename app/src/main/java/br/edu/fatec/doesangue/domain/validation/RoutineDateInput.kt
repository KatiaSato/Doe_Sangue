package br.edu.fatec.doesangue.domain.validation

import java.time.LocalDate

/** Sem barras, uma entrada curta pode ter duas leituras; a escolha pertence ao usuário. */
object RoutineDateInput {
    private val compact = Regex("[0-9]{6,8}")

    fun candidates(value: String): List<LocalDate> {
        val text = value.trim()
        if (!compact.matches(text)) return listOfNotNull(SchedulingDateInput.parse(text))
        val year = text.takeLast(4)
        val dayAndMonth = text.dropLast(4)
        return (1..2).mapNotNull { dayLength ->
            val monthLength = dayAndMonth.length - dayLength
            if (monthLength !in 1..2) null
            else SchedulingDateInput.parse("${dayAndMonth.take(dayLength)}/${dayAndMonth.drop(dayLength)}/$year")
        }.distinct()
    }
}
