// DeviceCrypto.android.kt

package com.mochyy.mochymusic.link

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual object DeviceCrypto {
    private const val Curve = "secp256r1"
    private val random = SecureRandom()

    private fun keyPair(pair: DeviceKeyPair): KeyPair =
        pair.handle as? KeyPair ?: throw IllegalStateException("not a jvm key pair")

    actual fun newKeyPair(): DeviceKeyPair =
        DeviceKeyPair(KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(Curve)) }.generateKeyPair())

    // X.509 SubjectPublicKeyInfo, just like android has always sent
    actual fun publicBytes(pair: DeviceKeyPair): ByteArray = keyPair(pair).public.encoded

    actual fun sharedSecret(pair: DeviceKeyPair, otherPublic: ByteArray): ByteArray? = runCatching {
        val other = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(otherPublic))
        KeyAgreement.getInstance("ECDH").apply {
            init(keyPair(pair).private)
            doPhase(other, true)
        }.generateSecret()
    }.getOrNull()

    actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)

    actual fun sha256(data: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(data)

    actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)
}
