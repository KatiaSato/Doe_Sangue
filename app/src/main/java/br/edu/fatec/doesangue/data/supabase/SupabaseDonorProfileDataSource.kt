package br.edu.fatec.doesangue.data.supabase

import br.edu.fatec.doesangue.data.supabase.dto.DonorProfileDto
import br.edu.fatec.doesangue.data.supabase.mapper.toDomain
import br.edu.fatec.doesangue.domain.model.DonorProfile
import br.edu.fatec.doesangue.domain.model.BloodType
import br.edu.fatec.doesangue.domain.model.ProfileError
import br.edu.fatec.doesangue.domain.model.ProfileFailure
import br.edu.fatec.doesangue.domain.validation.ProfileValidator
import java.time.LocalDate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

// Consulta o perfil da conta autenticada no Supabase
class SupabaseDonorProfileDataSource(
    private val client: SupabaseClient,
) {
    suspend fun updateCurrentProfile(
        displayName: String,
        bloodType: BloodType,
        phone: String?,
        birthDate: LocalDate?,
    ): DonorProfile {
        val name = displayName.trim()
        if (name.isBlank() || !ProfileValidator.isValidPhone(phone.orEmpty()) ||
            (birthDate != null && (birthDate.year < 1 || birthDate.isAfter(LocalDate.now())))) {
            throw ProfileFailure(ProfileError.INVALID_DATA)
        }
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull()
            ?: throw ProfileFailure(ProfileError.SESSION_UNAVAILABLE)
        val rows = client.from("perfil_doador").update(buildJsonObject {
            put("nome_exibicao", name)
            put("tipo_sanguineo", bloodType.databaseValue)
            put("telefone", ProfileValidator.normalizePhone(phone.orEmpty())?.let {
                JsonPrimitive(it)
            } ?: JsonNull)
            put("data_nascimento", birthDate?.let {
                JsonPrimitive(it.toString())
            } ?: JsonNull)
        }) {
            filter { eq("id", user.id) }
            select()
        }.decodeList<DonorProfileDto>()
        return rows.singleOrNull()?.toDomain()
            ?: throw ProfileFailure(ProfileError.MISSING)
    }

    suspend fun updateCurrentBloodType(bloodType: BloodType): DonorProfile {
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull()
            ?: throw ProfileFailure(ProfileError.SESSION_UNAVAILABLE)
        return client.from("perfil_doador").update(buildJsonObject {
            put("tipo_sanguineo", bloodType.databaseValue)
        }) {
            filter { eq("id", user.id) }
            select()
        }.decodeSingle<DonorProfileDto>().toDomain()
    }

    suspend fun getCurrentProfile(): DonorProfile? {
        client.auth.awaitInitialization()

        val user = client.auth.currentUserOrNull()
            ?: throw ProfileFailure(ProfileError.SESSION_UNAVAILABLE)

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
        if (normalizedName.isBlank()) throw ProfileFailure(ProfileError.INVALID_DATA)

        client.auth.awaitInitialization()

        val user = client.auth.currentUserOrNull()
            ?: throw ProfileFailure(ProfileError.SESSION_UNAVAILABLE)

        // Envia somente as colunas permitidas na criação, sem os novos campos do DTO.
        val createdProfile = client.from("perfil_doador").insert(buildJsonObject {
            put("id", user.id)
            put("nome_exibicao", normalizedName)
        }) {
                select()
            }.decodeSingle<DonorProfileDto>()

        return createdProfile.toDomain()
    }
}
