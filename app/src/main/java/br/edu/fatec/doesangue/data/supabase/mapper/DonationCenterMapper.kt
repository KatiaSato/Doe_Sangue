package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.DonationCenterDto
import br.edu.fatec.doesangue.domain.model.DonationCenter

// Converte os dados recebidos do Supabase para o modelo do aplicativo.
fun DonationCenterDto.toDomain(): DonationCenter {
    return DonationCenter(
        id = id.toString(),
        name = name,
        city = city,
        address = address,
        openingHours = openingHours.orEmpty(),
        phone = phone.orEmpty(),
    )
}
