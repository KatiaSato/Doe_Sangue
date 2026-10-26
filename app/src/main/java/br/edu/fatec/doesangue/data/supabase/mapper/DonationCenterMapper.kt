package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.DonationCenterDto
import br.edu.fatec.doesangue.domain.model.DonationCenter
import br.edu.fatec.doesangue.domain.model.GeoCoordinates

// Converte os dados recebidos do Supabase para o modelo do aplicativo.
fun DonationCenterDto.toDomain(): DonationCenter {
    return DonationCenter(
        id = id.toString(),
        name = name,
        city = city,
        address = address,
        openingHours = openingHours.orEmpty(),
        phone = phone.orEmpty(),
        timeZoneId = timeZoneId,
        coordinates = toCoordinates(),
    )
}

private fun DonationCenterDto.toCoordinates(): GeoCoordinates? {
    if (latitude == null && longitude == null) return null

    return GeoCoordinates(
        latitude = requireNotNull(latitude) { "Latitude ausente." },
        longitude = requireNotNull(longitude) { "Longitude ausente." },
    )
}
