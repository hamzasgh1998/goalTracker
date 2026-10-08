import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Whether debug builds seed the sample dataset. See the debug build type below.
val seedSampleData: Boolean =
    (project.findProperty("milestone.seedSampleData") as String?)?.toBooleanStrict() ?: true

/**
 * Release signing credentials, read from an untracked `keystore.properties`.
 *
 * Absent on a fresh clone, and that is deliberate: the build still works, it just
 * produces an unsigned release APK. Hard-coding these, or failing the build
 * without them, would both be worse.
 */
val keystoreProperties: Properties? =
    rootProject.file("keystore.properties")
        .takeIf { it.exists() }
        ?.let { file ->
            // Qualified `java.util.Properties` does not resolve here: Gradle's
            // Kotlin DSL binds `java` to the JavaPluginExtension accessor, which
            // shadows the package. Hence the import at the top of this file.
            Properties().apply { file.inputStream().use { stream -> load(stream) } }
        }

android {
    namespace = "com.milestone.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.milestone.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Sample data makes the UI look alive while developing. Override with
            //   ./gradlew installDebug -Pmilestone.seedSampleData=false
            // to get an empty app and walk through onboarding.
            buildConfigField("boolean", "SEED_SAMPLE_DATA", seedSampleData.toString())
        }
        release {
            // Production ships empty: the user's first goal is their own.
            buildConfigField("boolean", "SEED_SAMPLE_DATA", "false")
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true

                // `packageScope` is deliberately not set. Declaring the library
                // packages here let R8 repackage them, which moved
                // `kotlin.sequences` classes apart from the package-private
                // superclasses they extend — the app then died on first frame
                // with `IllegalAccessError: Class kotlin.sequences.d extended by
                // class tt1 is inaccessible`. The build succeeded every time; only
                // running the release APK showed it.
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // Lets us use java.time everywhere while keeping minSdk 24.
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    // Room needs a schema output location for migration diffing.
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // The repository tests drive suspend writes against a real in-memory database.
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.work.testing)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
