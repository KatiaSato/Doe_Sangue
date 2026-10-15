package br.edu.fatec.doesangue.domain.model

import java.time.Instant

// Nível publicado manualmente; não representa quantidade nem cálculo clínico.
enum class StockLevel(val databaseValue: String, val label: String) {
    STABLE("estavel", "Estável"), ALERT("alerta", "Alerta"), CRITICAL("critico", "Crítico");

    companion object {
        fun fromDatabase(value: String): StockLevel =
            requireNotNull(entries.firstOrNull { it.databaseValue == value })
    }
}

data class BloodStock(
    val centerId: String,
    val centerName: String,
    val bloodType: BloodType,
    val level: StockLevel,
    val source: String,
    val updatedAt: Instant,
    val timeZoneId: String,
)
