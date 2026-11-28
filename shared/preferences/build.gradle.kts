import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(21)

    extensions.configure<KotlinMultiplatformAndroidLibraryExtension>("androidLibrary") {
        namespace = "com.mochyy.mochymusic.shared.preferences"
        compileSdk = 37
        minSdk = 24
    }

    jvm("desktop") {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets.commonMain.dependencies {
        api(libs.datastore)
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
    }
}
