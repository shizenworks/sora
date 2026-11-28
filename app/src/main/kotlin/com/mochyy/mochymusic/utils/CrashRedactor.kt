// CrashRedactor.kt
//
// What a crash report looks like once it leaves the phone. The saved report stays as written (it is in
// no-backup storage and only this app reads it); the copy handed to the share sheet goes through here
// first, and the preview shows that copy, so what the user reads is exactly what they send. No android.*
// so it runs in JVM tests.

package com.mochyy.mochymusic.utils

import java.io.File
import java.net.URLEncoder

object CrashRedactor {
    const val MASK = "[redacted]"

    // values shorter than this are too likely to be ordinary words in a stack trace
    private const val MIN_KNOWN_LENGTH = 4

    // a link keeps its scheme and host (which server failed is the useful part); its path and query, where
    // stream signatures, playlist ids and account ids sit, go. content:// and file:// keep only the scheme
    private val webUrl = Regex("""\b(https?://)([A-Za-z0-9.-]+(?::\d+)?)([/?#][^\s"'<>)\]]*)?""")
    private val localUri = Regex("""\b(content|file)://[^\s"'<>)\]]+""")

    private val email = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+""")

    private val bearer = Regex("""(?i)\b(bearer\s+)[A-Za-z0-9._~+/=-]+""")

    // "cookie: …", "Authorization=…" and the like: the whole header value to the end of the line
    private val headerLine = Regex("""(?i)\b((?:set-)?cookie|authorization|x-goog-[a-z-]+)(\s*[:=]\s*)[^\n]+""")

    // name=value / name: value where the name says it is a secret
    private val secretPair = Regex(
        """(?i)\b([A-Za-z0-9_-]*(?:token|secret|password|passwd|apikey|api_key|session|sapisid|visitor_?data|datasync(?:id)?|sig|signature|auth))(\s*[:=]\s*"?)[^\s,;&"'<>]+""",
    )

    // what an imported file was called and where it sat on the phone; names have spaces and quotes in
    // them, so the rest of the line goes with it
    private val storagePath = Regex("""(?:/storage/emulated/\d+|/storage/[A-F0-9]{4}-[A-F0-9]{4}|/sdcard)/[^\n"<>]*""")

    // long opaque runs with a digit in them: cookies, sealed values, OAuth tokens. A stack frame's names are
    // split by dots, and a long method name has no digits, so frames stay readable
    private val opaque = Regex("""(?<![A-Za-z0-9_-])(?=[A-Za-z_-]*\d)[A-Za-z0-9_-]{40,}(?![A-Za-z0-9_-])""")

    /** [text] with every value in [known] (the user's account names, tokens, keys) and every pattern above masked */
    fun redact(text: String, known: Collection<String> = emptyList()): String {
        var out = text
        // the user's own values first and longest first, so a name inside a longer token can't leave half of it
        known.asSequence()
            .map { it.trim() }
            .filter { it.length >= MIN_KNOWN_LENGTH }
            .flatMap { sequenceOf(it, URLEncoder.encode(it, "UTF-8")) }
            .distinct()
            .sortedByDescending { it.length }
            .forEach { out = out.replace(it, MASK, ignoreCase = true) }
        out = headerLine.replace(out) { "${it.groupValues[1]}${it.groupValues[2]}$MASK" }
        out = bearer.replace(out) { "${it.groupValues[1]}$MASK" }
        out = localUri.replace(out) { "${it.groupValues[1]}://$MASK" }
        out = webUrl.replace(out) { m -> if (m.groupValues[3].isEmpty()) m.value else "${m.groupValues[1]}${m.groupValues[2]}/$MASK" }
        out = email.replace(out, MASK)
        out = secretPair.replace(out) { "${it.groupValues[1]}${it.groupValues[2]}$MASK" }
        out = storagePath.replace(out, MASK)
        out = opaque.replace(out, MASK)
        return out
    }

    /** writes the redacted copy of [report] into [folder] (replacing an earlier copy) and returns it */
    fun exportCopy(report: File, folder: File, known: Collection<String>): File {
        folder.mkdirs()
        folder.listFiles()?.forEach { it.delete() }
        val copy = File(folder, report.name)
        copy.writeText(redact(report.readText(), known))
        return copy
    }
}
