// DeviceHttpClient.ios.kt

package com.mochyy.mochymusic.link

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout

// NSURLSession through Ktor's Darwin engine
actual fun createDeviceConnectHttpClient(): HttpClient = HttpClient(Darwin) {
    install(HttpTimeout) {
        connectTimeoutMillis = 4_000L
        requestTimeoutMillis = 180_000L
        socketTimeoutMillis = 180_000L
    }
}
