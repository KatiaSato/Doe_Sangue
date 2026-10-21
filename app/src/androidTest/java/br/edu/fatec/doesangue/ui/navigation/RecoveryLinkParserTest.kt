package br.edu.fatec.doesangue.ui.navigation

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Endereços sintéticos: somente parsing Android, sem Activity/SDK/rede. */
@RunWith(AndroidJUnit4::class)
class RecoveryLinkParserTest {
    private val base = "br.edu.fatec.doesangue://auth/recovery"

    @Test fun acceptsExpectedAddressAndDecodesOpaqueCodeWithoutAssumingUuid() {
        assertEquals("synthetic-code", recoveryCodeFromUri(Uri.parse("$base?code=synthetic-code")))
        assertEquals("synthetic+code", recoveryCodeFromUri(Uri.parse("$base?code=synthetic%2Bcode")))
        assertEquals("synthetic-code", recoveryCodeFromUri(Uri.parse("$base?type=recovery&code=synthetic-code")))
    }

    @Test fun rejectsEmptyRelativeAndOpaqueAddresses() {
        listOf(Uri.EMPTY, Uri.parse("/recovery?code=synthetic-code"),
            Uri.parse("br.edu.fatec.doesangue:recovery?code=synthetic-code"))
            .forEach { assertNull(recoveryCodeFromUri(it)) }
    }

    @Test fun rejectsOtherSchemesHostsPortsAndUserInfo() {
        listOf(
            "https://auth/recovery?code=synthetic-code",
            "outro.app://auth/recovery?code=synthetic-code",
            "br.edu.fatec.doesangue://outro/recovery?code=synthetic-code",
            "br.edu.fatec.doesangue://auth.evil.test/recovery?code=synthetic-code",
            "br.edu.fatec.doesangue://auth:443/recovery?code=synthetic-code",
            "br.edu.fatec.doesangue://user@auth/recovery?code=synthetic-code",
            "br.edu.fatec.doesangue://AUTH/recovery?code=synthetic-code",
        ).forEach { assertNull(recoveryCodeFromUri(Uri.parse(it))) }
    }

    @Test fun rejectsOtherPathsTrailingSlashAndEncodedPathAliases() {
        listOf("/callback", "/recovery/", "/%72ecovery", "/recovery/../recovery", "")
            .forEach {
                assertNull(recoveryCodeFromUri(Uri.parse("br.edu.fatec.doesangue://auth$it?code=synthetic-code")))
            }
    }

    @Test fun rejectsMissingEmptyAndWhitespaceCodes() {
        listOf("", "?type=recovery", "?code", "?code=", "?code=%20",
            "?code=synthetic%20code", "?code=%09synthetic-code", "?code=synthetic-code%0A")
            .forEach { assertNull(recoveryCodeFromUri(Uri.parse(base + it))) }
    }

    @Test fun rejectsRepeatedCodeParametersEvenWhenOneIsEmpty() {
        listOf("?code=first&code=second", "?code=first&code=", "?code=&code=second")
            .forEach { assertNull(recoveryCodeFromUri(Uri.parse(base + it))) }
    }

    @Test fun rejectsFragmentsIncludingEmptyFragment() {
        listOf("#", "#synthetic", "#access_token=synthetic")
            .forEach { assertNull(recoveryCodeFromUri(Uri.parse("$base?code=synthetic-code$it"))) }
    }

    @Test fun rejectsErrorIndicatorsEvenWithCodeAndWithoutDisplayingDescription() {
        listOf("error", "error_code", "error_description", "%65rror")
            .forEach {
                assertNull(recoveryCodeFromUri(Uri.parse("$base?code=synthetic-code&$it=synthetic")))
                assertNull(recoveryCodeFromUri(Uri.parse("$base?$it=")))
            }
    }
}
