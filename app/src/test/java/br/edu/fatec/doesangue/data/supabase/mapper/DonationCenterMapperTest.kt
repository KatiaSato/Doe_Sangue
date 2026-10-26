package br.edu.fatec.doesangue.data.supabase.mapper

import br.edu.fatec.doesangue.data.supabase.dto.DonationCenterDto
import br.edu.fatec.doesangue.domain.model.GeoCoordinates
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class DonationCenterMapperTest {
    private val json = Json { ignoreUnknownKeys = true }
    private fun center(extra: String = "") = json.decodeFromString<DonationCenterDto>("""
        {"id":9000000000,"nome":"Unidade sintética","cidade":"Cidade fictícia",
        "endereco":"Endereço fictício","fuso_horario":"America/Sao_Paulo",
        "publicada":true$extra}
    """)

    @Test fun legacyPayloadWithoutCoordinatesPreservesExistingFields() {
        val result = center().toDomain()
        assertEquals("9000000000", result.id)
        assertEquals("Unidade sintética", result.name)
        assertEquals("Cidade fictícia", result.city)
        assertEquals("Endereço fictício", result.address)
        assertEquals("America/Sao_Paulo", result.timeZoneId)
        assertEquals("", result.openingHours)
        assertEquals("", result.phone)
        assertNull(result.coordinates)
    }

    @Test fun explicitNullPairRepresentsAnUnknownLocation() {
        assertNull(center(""", "latitude":null, "longitude":null""").toDomain().coordinates)
    }

    @Test fun jsonCoordinatePairIsTransportedWithoutChangingSignsOrAxes() {
        val result = center(""", "latitude":-22.123456, "longitude":47.654321""").toDomain()
        assertEquals(GeoCoordinates(-22.123456, 47.654321), result.coordinates)
    }

    @Test fun zeroCoordinatesAreAValidPositionRatherThanMissingData() {
        assertEquals(GeoCoordinates(0.0, 0.0),
            center(""", "latitude":0, "longitude":0""").toDomain().coordinates)
    }

    @Test fun partialPairsAreRejectedInsteadOfSilentlyHidingInvalidData() {
        for (extra in listOf(
            """, "latitude":10""", """, "longitude":20""",
            """, "latitude":10, "longitude":null""",
            """, "latitude":null, "longitude":20""",
        )) {
            assertThrows(IllegalArgumentException::class.java) { center(extra).toDomain() }
        }
    }

    @Test fun inclusiveGeographicBoundariesAreAccepted() {
        for (lat in listOf(-90.0, 90.0)) for (lon in listOf(-180.0, 180.0)) {
            assertEquals(GeoCoordinates(lat, lon),
                center().copy(latitude = lat, longitude = lon).toDomain().coordinates)
        }
    }

    @Test fun outOfRangeAndNonFiniteValuesAreRejectedByTheDomainAndMapper() {
        val pairs = listOf(
            90.000001 to 0.0, -90.000001 to 0.0,
            0.0 to 180.000001, 0.0 to -180.000001,
            Double.NaN to 0.0, 0.0 to Double.NaN,
            Double.POSITIVE_INFINITY to 0.0, Double.NEGATIVE_INFINITY to 0.0,
            0.0 to Double.POSITIVE_INFINITY, 0.0 to Double.NEGATIVE_INFINITY,
        )
        for ((lat, lon) in pairs) {
            assertThrows(IllegalArgumentException::class.java) { GeoCoordinates(lat, lon) }
            assertThrows(IllegalArgumentException::class.java) {
                center().copy(latitude = lat, longitude = lon).toDomain()
            }
        }
    }
}
