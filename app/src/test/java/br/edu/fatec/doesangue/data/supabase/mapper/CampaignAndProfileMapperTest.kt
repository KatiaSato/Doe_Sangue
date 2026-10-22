package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.CampaignDto
import br.edu.fatec.doesangue.data.supabase.dto.DonorProfileDto
import br.edu.fatec.doesangue.domain.model.BloodType
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CampaignAndProfileMapperTest {
    private val json = Json { ignoreUnknownKeys = true }
    private fun campaign(types: String = "[]") = json.decodeFromString<CampaignDto>("""
        {"id":9000000000,"unidade_id":8000000000,"titulo":"Campanha sintética",
        "descricao":"Descrição","inicio":"2026-10-15T08:00:00-03:00",
        "fim":"2026-10-16T08:00:00-03:00","fonte":"Demonstração",
        "unidade_nome":"Unidade sintética","fuso_horario":"America/Sao_Paulo",
        "tipos_sanguineos":$types}
    """)

    @Test fun rpcJsonPreservesBigintIdentityOffsetsAndExactTypes() {
        val result = campaign("[\"AB-\",\"O+\"]").toDomain()
        assertEquals("9000000000", result.id)
        assertEquals("8000000000", result.centerId)
        assertEquals(Instant.parse("2026-10-15T11:00:00Z"), result.startsAt)
        assertEquals(listOf(BloodType.AB_NEGATIVE, BloodType.O_POSITIVE), result.bloodTypes)
    }

    @Test fun noTargetsMeansGeneralCampaign() { assertTrue(campaign().toDomain().bloodTypes.isEmpty()) }

    @Test fun invalidOrUnknownCampaignTargetsAreRejected() {
        for (value in listOf("NAO_SEI", "INVALIDO")) {
            assertThrows(IllegalArgumentException::class.java) { campaign("[\"$value\"]").toDomain() }
        }
    }

    @Test fun existingProfileWithoutTypeDefaultsToUnknownAndNewTypeIsRead() {
        val legacy = json.decodeFromString<DonorProfileDto>("""{"id":"synthetic","nome_exibicao":"Doador"}""")
        assertEquals(BloodType.UNKNOWN, legacy.toDomain().bloodType)
        val current = json.decodeFromString<DonorProfileDto>("""{"id":"synthetic","nome_exibicao":"Doador","tipo_sanguineo":"B-"}""")
        assertEquals(BloodType.B_NEGATIVE, current.toDomain().bloodType)
    }

    @Test fun unrecognizedProfileTypeIsNotSilentlyChangedToUnknown() {
        assertThrows(IllegalArgumentException::class.java) { DonorProfileDto("synthetic", "Doador", "INVALIDO").toDomain() }
    }

    @Test fun profileOptionalFieldsRemainNullForLegacyAndPreserveCalendarDate() {
        val legacy = DonorProfileDto("synthetic", "Doador").toDomain()
        assertNull(legacy.phone)
        assertNull(legacy.birthDate)
        val profile = json.decodeFromString<DonorProfileDto>("""
            {"id":"synthetic","nome_exibicao":"Doador","tipo_sanguineo":"O+",
            "telefone":"00900000000","data_nascimento":"2000-02-29"}
        """).toDomain()
        assertEquals("00900000000", profile.phone)
        assertEquals(java.time.LocalDate.of(2000, 2, 29), profile.birthDate)
    }
}
