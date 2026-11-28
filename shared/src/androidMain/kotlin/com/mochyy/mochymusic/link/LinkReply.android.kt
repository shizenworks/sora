// LinkReply.android.kt

package com.mochyy.mochymusic.link

import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.io.OutputStream
import java.util.zip.GZIPInputStream

// a zipper and other jvm streams write to an OutputStream, while a route's body is a LinkOutput
fun LinkOutput.asOutputStream(): OutputStream {
    val sink = this
    return object : OutputStream() {
        override fun write(byte: Int) {
            val one = byteArrayOf(byte.toByte())
            sink.write(one, 0, 1)
        }

        override fun write(buffer: ByteArray, offset: Int, count: Int) {
            sink.write(buffer, offset, count)
        }
    }
}

// the two shapes a big body comes in: a library snapshot squeezed on the way out, or a song file straight through
suspend fun LinkReply.gunzipTo(file: File) {
    try {
        GZIPInputStream(channel().toInputStream()).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    } finally {
        close()
    }
}

suspend fun LinkReply.copyTo(file: File) {
    try {
        channel().toInputStream().use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    } finally {
        close()
    }
}
