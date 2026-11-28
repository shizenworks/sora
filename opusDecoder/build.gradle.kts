plugins {
    id("com.android.library")
}

android {
    namespace = "androidx.media3.decoder.opus"
    compileSdk = 37
    ndkVersion = "27.0.12077973"

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        externalNativeBuild {
            cmake {
                // libopus is vendored, nobody steps through it: a debug APK decoding at -O0 would measure the wrong thing
                arguments += listOf("-DCMAKE_BUILD_TYPE=Release", "-DOPUS_BUILD_TESTING=OFF", "-DOPUS_BUILD_PROGRAMS=OFF")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.media3)
    compileOnly(libs.androidx.annotation)
}
