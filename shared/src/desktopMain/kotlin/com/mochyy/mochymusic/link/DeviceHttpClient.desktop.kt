// DeviceHttpClient.desktop.kt

package com.mochyy.mochymusic.link

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

// OkHttp on the desktop jvm too, so the request bytes match what android sends
actual fun createDeviceConnectHttpClient(): HttpClient = HttpClient(OkHttp) {
    install(HttpTimeout) {
        connectTimeoutMillis = 4_000L
        requestTimeoutMillis = 180_000L
        socketTimeoutMillis = 180_000L
    }
}
