package br.edu.fatec.doesangue.ui.navigation

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

/** Somente telas montadas com fontes fictícias nestes testes. */
internal fun captureSyntheticProfileLogin(compose: ComposeContentTestRule, name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
    val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "profile-login-test").apply { mkdirs() }
    File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
}
