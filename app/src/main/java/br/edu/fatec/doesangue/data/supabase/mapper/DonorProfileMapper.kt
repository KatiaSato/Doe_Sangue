package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.DonorProfileDto
import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.domain.model.BloodType
import java.time.LocalDate

// Converte o perfil recebido do Supabase para o modelo do aplicativo
fun DonorProfileDto.toDomain(): DonorProfile {
    return DonorProfile(
        id = id,
        displayName = displayName,
        bloodType = BloodType.fromDatabase(bloodType),
        phone = phone,
        birthDate = birthDate?.let(LocalDate::parse),
    )
}
