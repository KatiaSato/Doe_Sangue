package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.DonorProfileDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.DonorProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

// Consulta o perfil da conta autenticada no Supabase
class SupabaseDonorProfileDataSource(
    private val client: SupabaseClient,
) {
    suspend fun getCurrentProfile(): DonorProfile? {
        client.auth.awaitInitialization()

        val user = checkNotNull(client.auth.currentUserOrNull()) {
            "É necessário entrar para consultar o perfil."
        }

        val profiles = client.from("perfil_doador").select {
                filter {
                    eq("id", user.id)
                }
            }.decodeList<DonorProfileDto>()
        return profiles.singleOrNull()?.toDomain()
    }

    /*
    Essa função:
    1. Remove espaços nas extremidades do nome e rejeita um nome vazio.
    2. Obtém o ID da conta autenticada.
    3. Envia somente id e nome_exibicao; o banco preenche criado_em.
    4. Recebe o registro criado e o converte para o domínio.
    O select() dentro de insert pede que o Supabase devolva os dados inseridos, conforme a documentação oficial.
     */
    suspend fun createCurrentProfile(displayName: String): DonorProfile {
        val normalizedName = displayName.trim()
        require(normalizedName.isNotBlank()) {
            "Informe o nome de exibição."
        }

        client.auth.awaitInitialization()

        val user = checkNotNull(client.auth.currentUserOrNull()) {
            "É necessário entrar para criar o perfil."
        }

        val profileDto = DonorProfileDto(
            id = user.id,
            displayName = normalizedName,
        )

        val createdProfile = client.from("perfil_doador").insert(profileDto) {
                select()
            }.decodeSingle<DonorProfileDto>()

        return createdProfile.toDomain()
    }
}