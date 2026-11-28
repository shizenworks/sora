// DeviceHttpClient.kt

package com.mochyy.mochymusic.link

import io.ktor.client.HttpClient

// one client per platform engine, with the lan-friendly waits the link has always used
expect fun createDeviceConnectHttpClient(): HttpClient
