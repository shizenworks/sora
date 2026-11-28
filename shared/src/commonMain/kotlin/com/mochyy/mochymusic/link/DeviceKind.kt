// DeviceKind.kt

package com.mochyy.mochymusic.link

// what kind of thing a linked device is, so the list can draw it and only offer what it can really do
enum class DeviceKind(val wire: String) {
    Phone("phone"),
    Tablet("tablet"),
    Tv("tv"),
    Car("car"),
    // mochy on a mac, windows or linux; it pairs and steers like a phone but never runs this app, so of() never says it
    Desktop("desktop"),
    ;

    // a tv mirrors a phone's library and is steered by it; the others are all phones of some size
    val isTv: Boolean get() = this == Tv

    companion object {
        // a peer from before kinds were sent says nothing, the caller knows what it must have been
        fun fromWire(value: String?, fallback: DeviceKind): DeviceKind = entries.firstOrNull { it.wire == value } ?: fallback

        // each platform owns the one place that knows what it is: an android tv, a desktop app, an iphone
        fun of(platformContext: Any): DeviceKind = deviceKindOf(platformContext)
    }
}

expect fun deviceKindOf(platformContext: Any): DeviceKind

// the name a paired device shows for this one when the person hasn't set their own
expect fun getDefaultDeviceName(): String

// this device's addresses on the wi-fi, for a network that hides devices from each other
expect fun getLocalIpV4Addresses(): List<String>

fun getLocalIpV4Address(): String? = getLocalIpV4Addresses().firstOrNull()

// small enough for both the shared code and the platform actuals; a lost log line never breaks a link
internal object LinkLog {
    fun i(message: String) = println("Link: $message")

    fun w(message: String, throwable: Throwable? = null) {
        println("Link: $message" + (throwable?.message?.let { ": $it" } ?: ""))
    }
}
