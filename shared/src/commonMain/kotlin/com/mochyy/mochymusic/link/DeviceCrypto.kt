// DeviceCrypto.kt

package com.mochyy.mochymusic.link

// a p-256 key pair whose halves never leave the platform's own crypto library; the handle is
// that library's own key object, opaque to common code
class DeviceKeyPair internal constructor(internal val handle: Any?)

// the cryptography the link protocol needs, behind one shared contract so android, desktop and ios
// can each use their own library while agreeing on the bytes. secp256r1 + ECDH + SHA-256 salts are
// Mochy's v2 handshake and can't change without breaking pairing with older devices.
expect object DeviceCrypto {
    fun newKeyPair(): DeviceKeyPair

    // X.509 SubjectPublicKeyInfo, exactly what java's PublicKey.encoded returns and what old phones send
    fun publicBytes(pair: DeviceKeyPair): ByteArray

    // the raw ECDH secret; null when the other half doesn't parse
    fun sharedSecret(pair: DeviceKeyPair, otherPublic: ByteArray): ByteArray?

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray

    fun sha256(data: ByteArray): ByteArray

    fun secureRandomBytes(size: Int): ByteArray
}

// one lock per object that needs it; common code can't use jvm's synchronized
expect class PlatformLock() {
    fun <T> withLock(block: () -> T): T
}
