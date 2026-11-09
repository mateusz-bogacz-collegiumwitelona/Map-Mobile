import java.io.StringReader
import java.net.URI
import java.net.URISyntaxException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localPropertiesText = providers.fileContents(
    rootProject.layout.projectDirectory.file("local.properties")
).asText.get()

val localProperties = Properties().apply {
    StringReader(localPropertiesText).use { load(it) }
}

fun readApiBaseUrl(propertyName: String): String {
    val value = providers.gradleProperty(propertyName).orNull
        ?: providers.environmentVariable(propertyName).orNull
        ?: localProperties.getProperty(propertyName)
        ?: throw org.gradle.api.GradleException(
            "Brak $propertyName. Ustaw tę wartość w local.properties, zmiennej środowiskowej albo przez -P."
        )

    val parsedUrl = try {
        URI(value)
    } catch (error: URISyntaxException) {
        throw org.gradle.api.GradleException(
            "$propertyName nie jest poprawnym adresem URL: $value",
            error
        )
    }

    check(parsedUrl.scheme == "http" || parsedUrl.scheme == "https") {
        "$propertyName musi zaczynać się od http:// albo https://."
    }
    check(!parsedUrl.host.isNullOrBlank()) {
        "$propertyName musi zawierać nazwę hosta."
    }
    check(value.endsWith("/")) {
        "$propertyName musi kończyć się znakiem /, np. https://api.example.com/."
    }

    if (propertyName == "MAP_API_BASE_URL_PRODUCTION") {
        check(parsedUrl.scheme == "https") {
            "Produkcyjny adres API musi używać HTTPS."
        }
    }

    return value
}

fun asBuildConfigString(value: String): String {
    val escapedValue = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    return "\"$escapedValue\""
}

val developmentApiBaseUrl =
    readApiBaseUrl("MAP_API_BASE_URL_DEVELOPMENT")
val productionApiBaseUrl =
    readApiBaseUrl("MAP_API_BASE_URL_PRODUCTION")

android {
    namespace = "com.example.map"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.map"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "environment"

    productFlavors {
        create("development") {
            dimension = "environment"
            buildConfigField(
                "String",
                "API_BASE_URL",
                asBuildConfigString(developmentApiBaseUrl)
            )
        }

        create("production") {
            dimension = "environment"
            buildConfigField(
                "String",
                "API_BASE_URL",
                asBuildConfigString(productionApiBaseUrl)
            )
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofitKotlinxSerialization)
    implementation(libs.okhttp)
    implementation(libs.okhttpLoggingInterceptor)
    implementation(libs.kotlinxSerializationJson)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.security.crypto)
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation(libs.junit)
    testImplementation(libs.okhttpMockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.hiltAndroidTesting)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    kspAndroidTest(libs.hilt.compiler)
}