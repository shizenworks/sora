// YouTube Music client (models, page parsers and the InnerTube/YouTube facade). Not Mochy's own work:
// it comes from the projects credited in the README (Metrolist and those before it).
//
// Multiplatform so the desktop and iOS apps can browse and search with the code Android uses:
// - commonMain: the response models and page parsers, pure Kotlin
// - jvmSharedMain: the HTTP client, the YouTube facade and the NewPipe fallback, shared by the
//   Android and desktop targets because both run on a JVM
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)

    extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("androidLibrary") {
        namespace = "com.music.innertube"
        compileSdk = 37
        minSdk = 24
    }

    jvm("desktop") {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }

    // models and parsers compile for iOS; the facade gets an iOS client when the iOS app needs it
    iosArm64()
    iosSimulatorArm64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.encoding)
        }
        val jvmSharedMain = create("jvmSharedMain") {
            dependsOn(getByName("commonMain"))
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.brotli)
                implementation(libs.newpipeextractor)
            }
        }
        getByName("androidMain").dependsOn(jvmSharedMain)
        getByName("desktopMain").dependsOn(jvmSharedMain)
    }
}
