package com.mochyy.mochymusic.ui.theme

import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomFontFilesTest {
    private val fs = FileSystem.SYSTEM
    private val folder: Path = Files.createTempDirectory("mochy-fonts").toFile().apply { deleteOnExit() }.toOkioPath()
    private val stem = "custom-regular"

    private fun stage(text: String): Path =
        CustomFontFiles.staging(folder, stem).also { fs.write(it) { writeUtf8(text) } }

    @Test
    fun aFirstImportIsInstalledAndNotDeletedWithItsStagingCopy() {
        val staged = stage("font one")
        // the staged copy isn't an installed font, so nothing cleans it away before it's moved
        assertNull(CustomFontFiles.installed(fs, folder, stem))
        val installed = CustomFontFiles.install(fs, folder, stem, "ttf", staged)
        assertEquals(folder / "custom-regular.ttf", installed)
        assertEquals("font one", fs.read(installed!!) { readUtf8() })
        assertFalse(fs.exists(staged))
    }

    @Test
    fun aReplacementTakesTheOldFontsPlaceOnlyOnceItIsIn() {
        CustomFontFiles.install(fs, folder, stem, "ttf", stage("old"))
        val installed = CustomFontFiles.install(fs, folder, stem, "otf", stage("new"))
        assertEquals(folder / "custom-regular.otf", installed)
        assertEquals("new", fs.read(installed!!) { readUtf8() })
        assertFalse(fs.exists(folder / "custom-regular.ttf"))
        assertEquals(installed, CustomFontFiles.installed(fs, folder, stem))
    }

    @Test
    fun aFailedSaveKeepsTheOldFontAndDropsTheStagedCopy() {
        CustomFontFiles.install(fs, folder, stem, "ttf", stage("old"))
        val failing = object : ForwardingFileSystem(fs) {
            override fun atomicMove(source: Path, target: Path) = throw IOException("disk full")
        }
        val staged = stage("new")
        assertNull(CustomFontFiles.install(failing, folder, stem, "otf", staged))
        assertEquals("old", fs.read(folder / "custom-regular.ttf") { readUtf8() })
        assertFalse(fs.exists(staged))
    }

    @Test
    fun removingLeavesOtherPartsAlone() {
        CustomFontFiles.install(fs, folder, stem, "ttf", stage("regular"))
        val bold = CustomFontFiles.staging(folder, "custom-bold").also { fs.write(it) { writeUtf8("bold") } }
        CustomFontFiles.install(fs, folder, "custom-bold", "ttf", bold)
        CustomFontFiles.remove(fs, folder, "custom-bold")
        assertNull(CustomFontFiles.installed(fs, folder, "custom-bold"))
        assertTrue(fs.exists(folder / "custom-regular.ttf"))
    }
}
