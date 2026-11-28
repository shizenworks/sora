// the proxy from settings for everything that talks to youtube, read on each request so a change needs no restart

package com.mochyy.mochymusic.utils

import androidx.datastore.preferences.core.Preferences
import com.mochyy.mochymusic.constants.ProxyEnabledKey
import com.mochyy.mochymusic.constants.ProxyPasswordKey
import com.mochyy.mochymusic.constants.ProxyTypeKey
import com.mochyy.mochymusic.constants.ProxyUrlKey
import com.mochyy.mochymusic.constants.ProxyUsernameKey
import com.mochyy.mochymusic.extensions.toEnum
import com.music.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.Authenticator
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

data class ProxyConfig(
    val type: ProxyType,
    val address: InetSocketAddress,
    val username: String,
    val password: String,
) {
    val hasLogin: Boolean get() = username.isNotEmpty() || password.isNotEmpty()
}

object AppProxy {
    @Volatile
    var current: ProxyConfig? = null
        private set

    // one pool and dispatcher for every client in the app, so dozens of helpers don't each spin up their own thread pools
    internal val connectionPool = ConnectionPool()
    internal val dispatcher = Dispatcher().apply {
        // covers stream in from a handful of picture hosts while the download queue runs beside
        // them, so both need room: a per-host cap of 10 left a fast-scrolling grid queued behind
        // ten thumbnails, and a big download batch behind ten streams
        maxRequests = 96
        maxRequestsPerHost = 32
    }

    // shared base client so callers can use newBuilder() instead of allocating fresh pools and executors
    val sharedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(connectionPool)
            .followAppProxy()
            .build()
    }

    // with no proxy of ours the phone's own proxy setting still counts, as it did before
    internal val selector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> =
            YouTube.proxy?.let { listOf(it) }
                ?: ProxySelector.getDefault()?.select(uri)
                ?: listOf(Proxy.NO_PROXY)

        override fun connectFailed(uri: URI?, address: SocketAddress?, error: IOException?) = Unit
    }

    /** what the settings say, with or without the switch on, null while the address isn't host:port */
    fun configFrom(settings: Preferences, requireEnabled: Boolean = true): ProxyConfig? {
        if (requireEnabled && settings[ProxyEnabledKey] != true) return null
        val address = parseAddress(settings[ProxyUrlKey].orEmpty()) ?: return null
        return ProxyConfig(
            type = settings[ProxyTypeKey].toEnum(defaultValue = ProxyType.HTTP),
            address = address,
            username = settings[ProxyUsernameKey].orEmpty(),
            password = settings[ProxyPasswordKey].orEmpty(),
        )
    }

    /** sends youtube through config, or straight out when it's null, false when nothing changed */
    fun apply(config: ProxyConfig?): Boolean {
        if (config == current) return false
        YouTube.proxyAuth = config?.takeIf { it.type == ProxyType.HTTP && it.hasLogin }
            ?.let { Credentials.basic(it.username, it.password) }
        useSocksLogin(config)
        // after the login: the youtube client rebuilds itself here and takes the login with it
        YouTube.proxy = config?.let { Proxy(it.type.toJavaProxyType(), it.address) }
        current = config
        connectionPool.evictAll()
        return true
    }

    /** reads "1.2.3.4:8080", "proxy.example.com:1080" or "[2001:db8::1]:3128", skipping a pasted scheme or login in front */
    fun parseAddress(text: String): InetSocketAddress? =
        // unresolved: the proxy's name is looked up when connecting, never here on the main thread
        parseProxyAddress(text)?.let { InetSocketAddress.createUnresolved(it.host, it.port) }

    /** the address as people write it, an ipv6 host in brackets so its port stays readable */
    fun addressLabel(address: InetSocketAddress): String = proxyAddressLabel(address.hostString, address.port)

    /** the settings page's check button: a real request through the proxy, and the reason when it fails */
    val tester = object : ProxyTester {
        override suspend fun test(type: ProxyType, address: ProxyAddress, username: String, password: String): ProxyCheckResult =
            check(ProxyConfig(type, InetSocketAddress.createUnresolved(address.host, address.port), username, password)).fold(
                onSuccess = { ProxyCheckResult.Passed(it) },
                onFailure = { ProxyCheckResult.Failed(it.checkFailure()) },
            )
    }

    /** asks youtube for an empty page through config, how many ms it took or why it failed */
    suspend fun check(config: ProxyConfig): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            useSocksLogin(config)
            val client = sharedClient.newBuilder()
                .proxy(Proxy(config.type.toJavaProxyType(), config.address))
                .proxyAuthenticator { _, response ->
                    if (config.type != ProxyType.HTTP || !config.hasLogin || response.request.header(ProxyAuthHeader) != null) {
                        null
                    } else {
                        response.request.newBuilder().header(ProxyAuthHeader, Credentials.basic(config.username, config.password)).build()
                    }
                }
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val started = System.nanoTime()
            client.newCall(Request.Builder().url("https://www.youtube.com/generate_204").build()).execute().use { response ->
                if (!response.isSuccessful) throw ProxyRefusedException(response.code)
            }
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
        }
    }

    // socks has no header for a login, java asks this one global authenticator instead
    private fun useSocksLogin(config: ProxyConfig?) {
        val login = config?.takeIf { it.type == ProxyType.SOCKS && it.hasLogin }
        Authenticator.setDefault(
            login?.let {
                object : Authenticator() {
                    override fun getPasswordAuthentication() = PasswordAuthentication(it.username, it.password.toCharArray())
                }
            },
        )
    }
}

private const val ProxyAuthHeader = "Proxy-Authorization"

// the proxy or youtube answered the check with an error code, 407 means the proxy wants a sign-in
class ProxyRefusedException(val code: Int) : IOException("HTTP $code")

// java.net's two protocols, from the names the settings store keeps
private fun ProxyType.toJavaProxyType(): Proxy.Type = when (this) {
    ProxyType.HTTP -> Proxy.Type.HTTP
    ProxyType.SOCKS -> Proxy.Type.SOCKS
}

// the line the settings page shows, from what the check ran into
private fun Throwable.checkFailure(): ProxyFailure = when (this) {
    is UnknownHostException -> ProxyFailure.HostNotFound
    is ProxyRefusedException -> if (code == 407) ProxyFailure.LoginRequired else ProxyFailure.Failed
    is ConnectException, is SocketTimeoutException, is NoRouteToHostException -> ProxyFailure.NoAnswer
    else -> ProxyFailure.Failed
}

/** sends this client through the proxy and the DNS over HTTPS choice from settings, read on every request and lookup */
fun OkHttpClient.Builder.followAppProxy(): OkHttpClient.Builder =
    followAppDns()
        .proxySelector(AppProxy.selector)
        .proxyAuthenticator { _, response ->
            val auth = YouTube.proxyAuth
            // already sent once and still refused, asking again would only loop
            if (auth == null || response.request.header(ProxyAuthHeader) != null) {
                null
            } else {
                response.request.newBuilder().header(ProxyAuthHeader, auth).build()
            }
        }
        .dispatcher(AppProxy.dispatcher)
        .connectionPool(AppProxy.connectionPool)
