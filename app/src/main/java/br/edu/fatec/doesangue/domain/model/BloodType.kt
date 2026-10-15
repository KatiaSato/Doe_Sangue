package br.edu.fatec.doesangue.domain.model

// Informação autodeclarada; não determina compatibilidade ou aptidão.
enum class BloodType(val databaseValue: String, val label: String) {
    A_POSITIVE("A+", "A+"),
    A_NEGATIVE("A-", "A-"),
    B_POSITIVE("B+", "B+"),
    B_NEGATIVE("B-", "B-"),
    AB_POSITIVE("AB+", "AB+"),
    AB_NEGATIVE("AB-", "AB-"),
    O_POSITIVE("O+", "O+"),
    O_NEGATIVE("O-", "O-"),
    UNKNOWN("NAO_SEI", "Não sei");

    companion object {
        val knownTypes: List<BloodType> = entries.filter { it != UNKNOWN }

        fun fromDatabase(value: String): BloodType =
            requireNotNull(entries.firstOrNull { it.databaseValue == value }) {
                "Tipo sanguíneo recebido é inválido."
            }
    }
}
