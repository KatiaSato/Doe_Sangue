import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Lê as configurações locais da raiz do projeto.
val localProperties = Properties().apply {
    val configFile = rootProject.file("local.properties")

    if (configFile.exists()) {
        configFile.inputStream().use { stream ->
            load(stream)
        }
    }
}

android {
    namespace = "br.edu.fatec.doesangue"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.edu.fatec.doesangue"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Disponibiliza a URL do Supabase ao código do aplicativo.
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperties.getProperty("SUPABASE_URL", "")}\""
        )

        // Disponibiliza somente a chave publicável ao aplicativo.
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY", "")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    androidTestImplementation(libs.androidx.navigation.testing)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Define as versões dos módulos Supabase.
    implementation(platform(libs.supabase.bom))

    // Acesso às tabelas pela API.
    implementation(libs.supabase.postgrest)

    // Autenticação e gerenciamento da sessão.
    implementation(libs.supabase.auth)

    // Comunicação pela rede.
    implementation(libs.ktor.client.okhttp)

    // Compatibilidade com versões antigas do Android.
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)
}
