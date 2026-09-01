import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val firebaseProperties = Properties().apply {
    val localFile = rootProject.file("firebase.local.properties")
    if (localFile.isFile) {
        localFile.inputStream().use(::load)
    }
}

fun firebaseBuildValue(name: String): String {
    val value = firebaseProperties.getProperty(name, "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$value\""
}

android {
    namespace = "com.thebifel.bubnilka"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thebifel.bubnilka"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "FIREBASE_API_KEY", firebaseBuildValue("firebase.apiKey"))
        buildConfigField("String", "FIREBASE_APP_ID", firebaseBuildValue("firebase.applicationId"))
        buildConfigField("String", "FIREBASE_PROJECT_ID", firebaseBuildValue("firebase.projectId"))
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", firebaseBuildValue("firebase.storageBucket"))
        buildConfigField("String", "GEMINI_MODEL", firebaseBuildValue("firebase.geminiModel"))
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        disable += "AndroidGradlePluginVersion"
    }
}

dependencies {
    implementation(project(":shared-core"))
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    testImplementation(libs.junit4)
}
