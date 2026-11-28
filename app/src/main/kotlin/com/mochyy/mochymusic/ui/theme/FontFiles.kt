// FontFiles.kt
// fonts from files: a look's font and the user's own font file, both built the same way

package com.mochyy.mochymusic.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.datastore.preferences.core.edit
import com.mochyy.mochymusic.constants.CustomBoldFontKey
import com.mochyy.mochymusic.constants.CustomFontKey
import com.mochyy.mochymusic.utils.dataStore
import java.io.File
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** whether [file] opens as a font; a file that isn't one comes back as the default typeface on android 8+ instead of throwing */
fun loadsAsFont(file: File): Boolean = runCatching { Typeface.createFromFile(file) !== Typeface.DEFAULT }
    .onFailure { Timber.tag("FontFiles").w(it, "Couldn't open ${file.name} as a font") }
    .getOrDefault(false)

/**
 * [regular] (and [bold] when there is one) as a font family, or null when [regular] isn't a font.
 * call it off the main thread: the file is opened here, not the first time some text draws with it
 */
fun fontFamilyFromFiles(regular: File, bold: File?): FontFamily? = runCatching {
    val typeface = Typeface.createFromFile(regular)
    if (typeface === Typeface.DEFAULT) error("${regular.name} isn't a font")
    // fonts from files with their weights are android 8+, older phones get the regular one and a drawn bold
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        FontFamily(listOfNotNull(Font(regular, FontWeight.Normal), bold?.let { Font(it, FontWeight.Bold) }))
    } else {
        FontFamily(typeface)
    }
}.onFailure { Timber.tag("FontFiles").w(it, "Couldn't load the font ${regular.name}") }.getOrNull()

/** the font file the user picked in settings, copied into the app's own storage; no permission to the picked file is kept */
object CustomFont {
    enum class Part(internal val stem: String) { Regular("custom-regular"), Bold("custom-bold") }

    sealed interface Picked {
        data class Done(val family: FontFamily) : Picked
        data object TooBig : Picked
        data object NotAFont : Picked
        /** it is a font, but it couldn't be saved into the app; the font from before is still on */
        data object NotSaved : Picked
    }

    /** the mime types the picker offers; many pickers tag a .ttf as octet-stream */
    val PickerTypes = arrayOf(
        "font/ttf", "font/otf", "font/sfnt", "application/x-font-ttf", "application/x-font-otf",
        "application/font-sfnt", "application/vnd.ms-opentype", "application/octet-stream",
    )

    private fun dir(context: Context) = File(context.filesDir, "fonts")

    // which files count as installed, and how a new one is swapped in, lives in shared CustomFontFiles (tested there)
    private fun file(context: Context, part: Part): File? =
        CustomFontFiles.installed(FileSystem.SYSTEM, dir(context).toOkioPath(), part.stem)?.toFile()

    private fun drop(context: Context, part: Part) {
        CustomFontFiles.remove(FileSystem.SYSTEM, dir(context).toOkioPath(), part.stem)
    }

    /** the saved custom font, loaded; null when there is none or it no longer loads */
    suspend fun load(context: Context): FontFamily? = withContext(Dispatchers.IO) {
        val regular = file(context, Part.Regular) ?: return@withContext null
        fontFamilyFromFiles(regular, file(context, Part.Bold))
    }

    /** copies the picked file in as [part], checks it loads, saves its name and puts the font on */
    suspend fun pick(context: Context, uri: Uri, part: Part): Picked = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name: String? = null
        var size: Long? = null
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0)
                    size = if (cursor.isNull(1)) null else cursor.getLong(1)
                }
            }
        }
        if (!customFontSizeAllowed(size)) return@withContext Picked.TooBig
        val extension = name?.substringAfterLast('.', "")?.lowercase()?.takeIf { it in CustomFontFiles.Extensions } ?: "ttf"
        val folder = dir(context).apply { mkdirs() }
        val incoming = CustomFontFiles.staging(folder.toOkioPath(), part.stem).toFile()
        // the size a provider reports can be missing or wrong, so the copy itself stops past the limit
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                incoming.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (!customFontSizeAllowed(total)) return@runCatching false
                        output.write(buffer, 0, read)
                    }
                }
                true
            } ?: error("the picked file has nothing to read")
        }.onFailure { Timber.tag("FontFiles").w(it, "Couldn't copy the picked font") }.getOrNull()
        if (copied != true) {
            incoming.delete()
            // false: it ran past the limit; null: it couldn't be read at all
            return@withContext if (copied == false) Picked.TooBig else Picked.NotAFont
        }
        if (!loadsAsFont(incoming)) {
            incoming.delete()
            return@withContext Picked.NotAFont
        }
        // a bold needs the regular it goes with
        if (part == Part.Bold && file(context, Part.Regular) == null) {
            incoming.delete()
            return@withContext Picked.NotSaved
        }
        // the old file stays until the new one is in place, so a failed save keeps the font from before
        val saved = CustomFontFiles.install(FileSystem.SYSTEM, folder.toOkioPath(), part.stem, extension, incoming.toOkioPath())?.toFile()
            ?: return@withContext Picked.NotSaved
        // a new regular is a new font, so a bold kept from the last one would not match it
        if (part == Part.Regular) drop(context, Part.Bold)
        val regular = file(context, Part.Regular)
        val family = regular?.let { fontFamilyFromFiles(it, file(context, Part.Bold)) }
        if (family == null) {
            saved.delete()
            return@withContext Picked.NotAFont
        }
        val shown = name ?: saved.name
        context.dataStore.edit { preferences ->
            when (part) {
                Part.Regular -> {
                    preferences[CustomFontKey] = shown
                    preferences.remove(CustomBoldFontKey)
                }
                Part.Bold -> preferences[CustomBoldFontKey] = shown
            }
        }
        // the font state changes on the main thread, like a look's font
        withContext(Dispatchers.Main) { AppFont.custom = family }
        Picked.Done(family)
    }

    /** takes [part] away; without the regular file there is no custom font, so that takes the bold with it */
    suspend fun remove(context: Context, part: Part) = withContext(Dispatchers.IO) {
        drop(context, part)
        if (part == Part.Regular) drop(context, Part.Bold)
        context.dataStore.edit { preferences ->
            if (part == Part.Regular) preferences.remove(CustomFontKey)
            preferences.remove(CustomBoldFontKey)
        }
        val family = if (part == Part.Regular) null else load(context)
        withContext(Dispatchers.Main) { AppFont.custom = family }
    }

}
