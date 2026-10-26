package br.edu.fatec.doesangue.domain.model

data class DonationCenter(
    val id: String,
    val name: String,
    val city: String,
    val address: String,
    val openingHours: String,
    val phone: String,
    val timeZoneId : String,
    val coordinates: GeoCoordinates? = null,
)

// ? permite que a unidade ainda não tenha coordenadas.
// = null mantém os usos atuais funcionando sem exigir esse novo dado.
// Assim, uma posição não cadastrada será representada por null.
