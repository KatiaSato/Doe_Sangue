package br.edu.fatec.doesangue.domain.model

data class BloodNeed(
    val donationCenterId: String,
    val bloodType: BloodType,
    val level: NeedLevel,
    val measurementUnit: String,
    val source: String,
    val lastUpdatedLabel: String,
    val isSynthetic: Boolean,
)

