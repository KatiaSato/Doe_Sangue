package br.edu.fatec.doesangue.domain.model

import java.time.Instant

data class Campaign(
    val id: String,
    val centerId: String,
    val title: String,
    val description: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val source: String,
    val centerName: String,
    val timeZoneId: String,
    val bloodTypes: List<BloodType>,
)
