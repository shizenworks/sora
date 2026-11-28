// Desktop launcher for Mochy (a fork of Mochy Music by realidkroo, github.com/realidkroo/mochy-music).
// It owns main() and packaging only; every screen lives in :shared (see runDesktopApp).
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    // the packaging plugin resolves its runtime classpath under the multiplatform naming, so one jvm() target
    kotlin("multiplatform")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        jvmMain {
            // Inter comes straight from the Android app's font folder, so the APK and the desktop build
            // share one copy of the files instead of :shared shipping a second 3 MB set inside the APK.
            resources.srcDir(rootProject.file("app/src/main/res")).apply {
                include("font/inter_*.ttf")
            }
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.mochyy.mochymusic.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "Mochy"
            packageVersion = "1.0.0"
            description = "Mochy music player for desktop, built on Mochy Music by realidkroo"
            copyright = "GPL-3.0; based on Mochy (github.com/realidkroo/mochy-music)"
            licenseFile.set(rootProject.file("LICENSE"))
            // jdk.security.auth: dbus-java's SASL EXTERNAL login reads the user id through it; without it the
            // Linux media session cannot connect in the packaged app (it then stays silently off).
            modules("java.net.http", "jdk.unsupported", "jdk.crypto.ec", "jdk.security.auth")
            linux {
                appCategory = "AudioVideo"
                menuGroup = "AudioVideo"
            }
        }
    }
}
