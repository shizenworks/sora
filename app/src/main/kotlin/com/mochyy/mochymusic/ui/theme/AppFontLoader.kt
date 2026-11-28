// Neiro's font choice (data/settings AppFont + ui/theme/CustomFontLoader), on sora's DataStore: the picked
// font is read at launch and whenever it changes, and handed to the shared font state, which the app's typography reads.

package com.mochyy.mochymusic.ui.theme

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.preferences.core.edit
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.AppFont
import com.mochyy.mochymusic.constants.AppFontKey
import com.mochyy.mochymusic.constants.CustomFontNameKey
import com.mochyy.mochymusic.constants.CustomFontPathKey
import com.mochyy.mochymusic.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import android.graphics.Typeface as AndroidTypeface

object AppFontLoader {
    val supportedMimeTypes = arrayOf(
        "font/ttf",
        "application/x-font-ttf",
        "application/x-font-truetype",
        "font/otf",
        "application/x-font-opentype",
        "application/vnd.ms-opentype",
        "application/octet-stream",
    )

    private fun fontDir(context: Context) = File(context.filesDir, "fonts").apply { if (!isDirectory) mkdirs() }

    private fun safeName(name: String) = name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "font.ttf" }

    // the family for a file, or null when android can't read it as a font; read here so a bad file is refused at
    // the pick rather than at the next launch, in place of the app's type
    fun familyOf(file: File): FontFamily? = runCatching {
        if (!file.isFile || file.length() < 1024) return null
        AndroidTypeface.createFromFile(file)
        FontFamily(Font(file))
    }.getOrNull()

    fun displayName(context: Context, uri: Uri): String =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "font.ttf"

    /** copies a picked document in and chooses it; false (with the error toast) when it isn't a readable font */
    suspend fun applyPicked(context: Context, uri: Uri): Boolean {
        val name = displayName(context, uri)
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val target = File(fontDir(context), safeName(name))
                val partial = File(target.parentFile, "${target.name}.part")
                context.contentResolver.openInputStream(uri)!!.use { input -> partial.outputStream().use { input.copyTo(it) } }
                if (familyOf(partial) == null) {
                    partial.delete()
                    null
                } else {
                    partial.renameTo(target)
                    target
                }
            }.getOrNull()
        }
        return choose(context, file, name.substringBeforeLast('.'))
    }

    /** a downloaded catalog font, copied out of the cache and chosen, the same as a picked file */
    suspend fun applyFile(context: Context, source: File, name: String): Boolean {
        val file = withContext(Dispatchers.IO) {
            runCatching {
                val target = File(fontDir(context), safeName(source.name))
                source.copyTo(target, overwrite = true)
                target.takeIf { familyOf(it) != null }
            }.getOrNull()
        }
        return choose(context, file, name)
    }

    private suspend fun choose(context: Context, file: File?, name: String): Boolean {
        if (file == null) {
            withContext(Dispatchers.Main) { Toast.makeText(context, R.string.custom_font_invalid, Toast.LENGTH_SHORT).show() }
            return false
        }
        context.dataStore.edit {
            it[AppFontKey] = AppFont.CUSTOM.name
            it[CustomFontNameKey] = name
            it[CustomFontPathKey] = file.absolutePath
        }
        return true
    }
}

/** called once from MainActivity: keeps the shared font on the choice saved in settings */
@Composable
fun ApplyAppFont() {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(Unit) {
        context.dataStore.data
            .map { (it[AppFontKey] ?: AppFont.DEFAULT.name) to it[CustomFontPathKey] }
            .distinctUntilChanged()
            .collect { (choice, path) ->
                com.mochyy.mochymusic.ui.theme.AppFont.custom = when (runCatching { AppFont.valueOf(choice) }.getOrDefault(AppFont.DEFAULT)) {
                    AppFont.DEFAULT -> null
                    AppFont.SF_PRO -> SfProFamily
                    AppFont.SYSTEM -> FontFamily.Default
                    AppFont.CUSTOM -> {
                        val family = path?.let { withContext(Dispatchers.IO) { AppFontLoader.familyOf(File(it)) } }
                        if (family == null) {
                            Toast.makeText(context, R.string.custom_font_invalid, Toast.LENGTH_SHORT).show()
                            context.dataStore.edit { it[AppFontKey] = AppFont.DEFAULT.name }
                        }
                        family
                    }
                }
            }
    }
}
