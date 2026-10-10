package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.DonorProfileDto
import br.edu.fatec.doesangue.domain.model.DonorProfile

// Converte o perfil recebido do Supabase para o modelo do aplicativo
fun DonorProfileDto.toDomain(): DonorProfile {
    return DonorProfile(
        id = id,
        displayName = displayName,
    )
}