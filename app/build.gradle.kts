import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val signingProps = Properties().also { props ->
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}
fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: signingProps.getProperty(prop)

android {
    namespace = "com.retroplayer.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.retroplayer.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.1.0"
    }

    // Release signing. CI passes the key through environment variables (GitHub secrets);
    // locally it comes from keystore.properties in the project root, which is never committed.
    // Without either, the release build is left unsigned.
    val storePath = signingValue("RETRO_KEYSTORE_FILE", "storeFile")

    signingConfigs {
        if (storePath != null) {
            create("release") {
                storeFile = file(storePath)
                storePassword = signingValue("RETRO_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("RETRO_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("RETRO_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Lets a debug build sit next to the released app on the same phone.
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:theme"))
    implementation(project(":core:media"))
    implementation(project(":feature:player"))
    implementation(project(":core:settings"))
    implementation(project(":widget"))
    implementation(project(":feature:share"))
    implementation(project(":feature:crate"))
    implementation(project(":core:history"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.window)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
