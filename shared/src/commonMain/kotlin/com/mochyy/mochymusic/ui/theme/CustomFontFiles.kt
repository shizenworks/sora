// CustomFontFiles.kt
// where the user's own font files live in the app's storage, and how a new one is swapped in: the old file
// stays until the new one is in place, so a failed save never leaves the app with no font, or half of one

package com.mochyy.mochymusic.ui.theme

import okio.FileSystem
import okio.IOException
import okio.Path

object CustomFontFiles {
    /** the file types a custom font is kept as */
    val Extensions = setOf("ttf", "otf")

    /** a picked file is copied to [staging] first and checked there; it never counts as an installed font */
    fun staging(folder: Path, stem: String): Path = folder / "$stem.incoming"

    /** the installed font file for [stem] (e.g. custom-regular.ttf), or null when there is none */
    fun installed(fs: FileSystem, folder: Path, stem: String): Path? = fontsOf(fs, folder, stem).firstOrNull()

    /**
     * Moves the checked [staged] file into place as [stem].[extension]. Only once it's there does an older
     * file for [stem] under another extension go. Returns the installed path, or null when the move failed:
     * then the staged copy is removed and the old font is untouched.
     */
    fun install(fs: FileSystem, folder: Path, stem: String, extension: String, staged: Path): Path? {
        val target = folder / "$stem.$extension"
        try {
            fs.atomicMove(staged, target)
        } catch (_: IOException) {
            runCatching { fs.delete(staged, mustExist = false) }
            return null
        }
        fontsOf(fs, folder, stem).filter { it != target }.forEach { runCatching { fs.delete(it, mustExist = false) } }
        return target
    }

    /** removes every installed file for [stem], leaving any staged copy alone */
    fun remove(fs: FileSystem, folder: Path, stem: String) {
        fontsOf(fs, folder, stem).forEach { runCatching { fs.delete(it, mustExist = false) } }
    }

    // only real font files count: "custom-regular.ttf" yes, "custom-regular.incoming" no
    private fun fontsOf(fs: FileSystem, folder: Path, stem: String): List<Path> =
        fs.listOrNull(folder).orEmpty().filter { path ->
            val name = path.name
            name.substringBeforeLast('.') == stem && name.substringAfterLast('.', "") in Extensions
        }
}
