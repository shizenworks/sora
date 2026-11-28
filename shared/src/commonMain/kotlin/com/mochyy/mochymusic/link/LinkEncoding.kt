// LinkEncoding.kt

package com.mochyy.mochymusic.link

// the small byte-to-text pieces the wire needs, written here so common code doesn't reach for a jvm class
private const val Base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

private const val HexChars = "0123456789abcdef"

internal fun hex(bytes: ByteArray): String = buildString(bytes.size * 2) {
    bytes.forEach { byte ->
        val value = byte.toInt() and 0xff
        append(HexChars[value ushr 4])
        append(HexChars[value and 0x0f])
    }
}

// java.util.Base64.NO_WRAP's alphabet and padding, since that is what android sends
internal fun base64Encode(bytes: ByteArray): String {
    val out = StringBuilder((bytes.size + 2) / 3 * 4)
    var index = 0
    while (index < bytes.size) {
        val first = bytes[index].toInt() and 0xff
        val second = if (index + 1 < bytes.size) bytes[index + 1].toInt() and 0xff else 0
        val third = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xff else 0
        out.append(Base64Chars[first ushr 2])
        out.append(Base64Chars[((first and 0x03) shl 4) or (second ushr 4)])
        out.append(if (index + 1 < bytes.size) Base64Chars[((second and 0x0f) shl 2) or (third ushr 6)] else '=')
        out.append(if (index + 2 < bytes.size) Base64Chars[third and 0x3f] else '=')
        index += 3
    }
    return out.toString()
}

internal fun base64Decode(text: String): ByteArray? {
    val clean = text.filterNot { it.isWhitespace() }
    if (clean.length % 4 == 1) return null
    val out = ArrayList<Byte>(clean.length * 3 / 4)
    var index = 0
    while (index < clean.length) {
        val c0 = base64Value(clean[index]) ?: return null
        val c1 = base64Value(clean.getOrNull(index + 1) ?: return null) ?: return null
        val pad2 = clean.getOrNull(index + 2) == '='
        val pad3 = clean.getOrNull(index + 3) == '='
        val c2 = if (pad2) 0 else base64Value(clean.getOrNull(index + 2) ?: return null) ?: return null
        val c3 = if (pad3) 0 else base64Value(clean.getOrNull(index + 3) ?: return null) ?: return null
        val triple = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3
        out += ((triple ushr 16) and 0xff).toByte()
        if (!pad2) out += ((triple ushr 8) and 0xff).toByte()
        if (!pad3) out += (triple and 0xff).toByte()
        index += 4
        if (pad2 || pad3) break
    }
    return out.toByteArray()
}

private fun base64Value(char: Char): Int? = when (char) {
    in 'A'..'Z' -> char - 'A'
    in 'a'..'z' -> char - 'a' + 26
    in '0'..'9' -> char - '0' + 52
    '+' -> 62
    '/' -> 63
    else -> null
}

// java.net.URLEncoder's rules: space is "+", [A-Za-z0-9._*-] stay, the rest is percent-encoded utf-8
internal fun urlEncode(text: String): String {
    val out = StringBuilder()
    text.encodeToByteArray().forEach { byte ->
        val value = byte.toInt() and 0xff
        val char = value.toChar()
        when {
            char in 'A'..'Z' || char in 'a'..'z' || char in '0'..'9' || char == '.' || char == '-' || char == '*' || char == '_' -> out.append(char)
            char == ' ' -> out.append('+')
            else -> out.append('%').append("%02X".formatHex(value))
        }
    }
    return out.toString()
}

internal fun urlDecode(text: String): String {
    val bytes = ArrayList<Byte>(text.length)
    var index = 0
    while (index < text.length) {
        val char = text[index]
        when {
            char == '+' -> {
                bytes += ' '.code.toByte()
                index++
            }
            char == '%' && index + 2 < text.length -> {
                val value = text.substring(index + 1, index + 3).toIntOrNull(16)
                if (value == null) {
                    bytes += char.code.toByte()
                    index++
                } else {
                    bytes += value.toByte()
                    index += 3
                }
            }
            else -> {
                char.toString().encodeToByteArray().forEach { bytes += it }
                index++
            }
        }
    }
    return bytes.toByteArray().decodeToString()
}

private fun String.formatHex(value: Int): String = buildString(2) {
    append("0123456789ABCDEF"[value ushr 4])
    append("0123456789ABCDEF"[value and 0x0f])
}

// compared in constant time, so the length of a near miss gives nothing away
internal fun constantTimeEquals(expected: String, given: String): Boolean {
    val a = expected.encodeToByteArray()
    val b = given.encodeToByteArray()
    var difference = a.size xor b.size
    val length = maxOf(a.size, b.size)
    for (index in 0 until length) {
        difference = difference or ((a.getOrNull(index)?.toInt() ?: 0) xor (b.getOrNull(index)?.toInt() ?: 0))
    }
    return difference == 0
}
