// ProxySettings.kt
//
// What the proxy settings page and the platform's network layer both need: the protocol names the
// stored preference keeps, the address text as people type it, and the shape of the check button's
// answer. Applying the proxy to OkHttp and java.net stays in :app (AppProxy).

package com.mochyy.mochymusic.utils

/** The proxy protocols the app offers. [name] is what ProxyTypeKey stores, so these names are the saved values. */
enum class ProxyType { HTTP, SOCKS }

/** host:port as typed in settings, the host left unresolved until a connection looks it up. */
data class ProxyAddress(val host: String, val port: Int)

/** reads "1.2.3.4:8080", "proxy.example.com:1080" or "[2001:db8::1]:3128", skipping a pasted scheme or login in front */
fun parseProxyAddress(text: String): ProxyAddress? {
    val hostPort = text.trim().substringAfter("://").substringAfterLast('@').trimEnd('/')
    val port = hostPort.substringAfterLast(':', "").toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
    val host = if (hostPort.startsWith("[")) {
        hostPort.substringAfter('[').substringBefore(']')
    } else {
        hostPort.substringBeforeLast(':')
    }
    if (host.isBlank()) return null
    return ProxyAddress(host, port)
}

/** the address as people write it, an ipv6 host in brackets so its port stays readable */
fun proxyAddressLabel(host: String, port: Int): String =
    if (':' in host) "[$host]:$port" else "$host:$port"

/** answers the page's check button on this platform; :app connects through the proxy and says how it went. */
interface ProxyTester {
    suspend fun test(type: ProxyType, address: ProxyAddress, username: String, password: String): ProxyCheckResult
}

sealed interface ProxyCheckResult {
    data class Passed(val millis: Long) : ProxyCheckResult
    data class Failed(val reason: ProxyFailure) : ProxyCheckResult
}

/** why the check failed, each one a proxy_check_* sentence on the page. */
enum class ProxyFailure { HostNotFound, LoginRequired, NoAnswer, Failed }
