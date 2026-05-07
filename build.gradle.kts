plugins {
    id("com.android.application") version "9.2.1"
    // Compose Compiler is a standalone plugin in Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21"
}

android {
    namespace = "at.jschatteiner.enitemhdtranslator"
    compileSdk = 37

    defaultConfig {
        applicationId = "at.jschatteiner.enitemhdtranslator"
        minSdk = 23
        targetSdk = 37
        versionCode = 3
        versionName = "1.1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "enitemhdtranslator"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

val releaseArtifactName = "Enite-Mittelhochdeutsches-Woerterbuch"

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("$releaseArtifactName.apk")
        }
    }
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    doLast {
        val bundleDir = layout.buildDirectory.dir("outputs/bundle/release").get().asFile
        val target = File(bundleDir, "$releaseArtifactName.aab")
        bundleDir.listFiles()?.forEach { file ->
            if (file.extension == "aab" && file != target) {
                if (target.exists()) target.delete()
                file.renameTo(target)
            }
        }
    }
}

dependencies {
    // Jetpack Compose BOM – keeps all Compose versions aligned
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    // Core Android
    implementation("androidx.core:core-ktx:1.18.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
