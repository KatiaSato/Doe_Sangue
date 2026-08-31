package br.edu.fatec.doesangue.domain.model

data class DonationCenter(
    val id: String,
    val name: String,
    val city: String,
    val address: String,
    val openingHours: String,
    val phone: String,
)

