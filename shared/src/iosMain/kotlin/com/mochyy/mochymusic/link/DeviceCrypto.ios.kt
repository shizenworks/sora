// DeviceCrypto.ios.kt

package com.mochyy.mochymusic.link

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

// Ported from Metrolist (github.com/MetrolistGroup/Metrolist), GPL-3.0: shared/src/iosMain/kotlin/com/metrolist/shared/deviceconnect/DeviceCrypto.ios.kt
// HMAC-SHA256, SHA-256 and the system random source are CommonCrypto's; the p-256 handshake needs
// Security.framework's SecKey exchange and stays off until that actual lands, the way Metrolist ships iOS.
@OptIn(ExperimentalForeignApi::class)
actual object DeviceCrypto {
    actual fun newKeyPair(): DeviceKeyPair = unavailable()

    actual fun publicBytes(pair: DeviceKeyPair): ByteArray = unavailable()

    actual fun sharedSecret(pair: DeviceKeyPair, otherPublic: ByteArray): ByteArray? = unavailable()

    actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val result = ByteArray(CC_SHA256_DIGEST_LENGTH)
        key.usePinned { pinnedKey ->
            data.usePinned { pinnedData ->
                result.usePinned { pinnedResult ->
                    CCHmac(
                        algorithm = kCCHmacAlgSHA256,
                        key = if (key.isEmpty()) null else pinnedKey.addressOf(0),
                        keyLength = key.size.toULong(),
                        data = if (data.isEmpty()) null else pinnedData.addressOf(0),
                        dataLength = data.size.toULong(),
                        macOut = pinnedResult.addressOf(0),
                    )
                }
            }
        }
        return result
    }

    actual fun sha256(data: ByteArray): ByteArray {
        val result = ByteArray(CC_SHA256_DIGEST_LENGTH)
        data.usePinned { pinnedData ->
            result.usePinned { pinnedResult ->
                check(
                    CC_SHA256(
                        data = if (data.isEmpty()) null else pinnedData.addressOf(0),
                        len = data.size.toUInt(),
                        md = pinnedResult.addressOf(0).reinterpret(),
                    ) != null,
                )
            }
        }
        return result
    }

    actual fun secureRandomBytes(size: Int): ByteArray {
        require(size >= 0)
        val bytes = ByteArray(size)
        if (bytes.isNotEmpty()) {
            bytes.usePinned { pinned ->
                check(SecRandomCopyBytes(kSecRandomDefault, size.toULong(), pinned.addressOf(0)) == errSecSuccess)
            }
        }
        return bytes
    }

    private fun unavailable(): Nothing =
        throw UnsupportedOperationException("device linking cryptography is not enabled on ios yet")
}
