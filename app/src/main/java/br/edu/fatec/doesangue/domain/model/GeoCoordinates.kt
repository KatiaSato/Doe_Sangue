package br.edu.fatec.doesangue.domain.model

data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0) {
            "Latitude inválida."
        }

        require(longitude.isFinite() && longitude in -180.0..180.0) {
            "Longitude inválida."
        }
    }
}