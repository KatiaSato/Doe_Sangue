package br.edu.fatec.doesangue.ui.screens.profile

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** A máscara pertence à entrada da tela; o domínio continua validando a data completa. */
internal fun birthDateDigits(value: String): String = value.filter { it in '0'..'9' }.take(8)

internal fun formatBirthDateInput(value: String): String = birthDateDigits(value).let { digits ->
    buildString {
        digits.forEachIndexed { index, digit ->
            if (index == 2 || index == 4) append('/')
            append(digit)
        }
    }
}

internal object BirthDateMask : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val length = text.length
        val formatted = formatBirthDateInput(text.text)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                (offset + (if (length > 2 && offset >= 2) 1 else 0) +
                    (if (length > 4 && offset >= 4) 1 else 0)).coerceAtMost(formatted.length)

            override fun transformedToOriginal(offset: Int): Int =
                (offset - (if (offset > 2) 1 else 0) -
                    (if (offset > 5) 1 else 0)).coerceIn(0, length)
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
