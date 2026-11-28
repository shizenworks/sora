package com.mochyy.mochymusic.shared.resources

import org.jetbrains.compose.resources.decodeToImageVector
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

// Compose resources only read vector sizes in dp or px and throw at runtime on anything else, which
// crashed the app the first time a shared screen drew an icon sized "24.0dip". Every icon the
// ResourceBridge copies in from app/src/main/res is checked here, in the folder the build reads.
class BridgedDrawablesTest {
    private val drawables = File("build/generated/mochyComposeResources/commonMain/drawable")
    private val size = Regex("""android:(?:width|height)="([^"]*)"""")

    @Test
    fun iconsAreThere() {
        // the bridge runs before the shared code compiles; if this folder moves, the check below must follow it
        assertTrue(drawables.listFiles().orEmpty().any { it.extension == "xml" }, "no bridged icons in ${drawables.absolutePath}")
    }

    @Test
    fun everyIconIsSizedInDpOrPx() {
        val bad = drawables.listFiles().orEmpty().filter { it.extension == "xml" }.flatMap { file ->
            size.findAll(file.readText()).map { it.groupValues[1] }
                .filterNot { it.matches(Regex("""[0-9.]+(dp|px)""")) }
                .map { "${file.name}: $it" }
        }
        assertTrue(bad.isEmpty(), "icons Compose resources can't read: $bad")
    }

    // the strongest check: Compose's own vector reader, the one that threw at runtime, reads every bridged icon
    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    @Test
    fun composeReadsEveryBridgedIcon() {
        val density = androidx.compose.ui.unit.Density(1f)
        val failed = drawables.listFiles().orEmpty().filter { it.extension == "xml" }.mapNotNull { file ->
            runCatching { file.readBytes().decodeToImageVector(density) }
                .exceptionOrNull()?.let { "${file.name}: ${it.message}" }
        }
        assertTrue(failed.isEmpty(), "icons Compose can't read: $failed")
    }

    @Test
    fun androidsWhiteIsBridgedAsItsValue() {
        // arrow_back is written with @android:color/white, which Compose can't resolve; the bridge writes its value
        val text = File(drawables, "arrow_back.xml").readText()
        assertTrue("#FFFFFFFF" in text && "@android:" !in text, text)
    }

    @Test
    fun controlTintedIconsAreBridged() {
        // sleep_timer is tinted ?attr/colorControlNormal in app/res; the bridged copy has no theme reference
        val text = File(drawables, "sleep_timer.xml").readText()
        assertTrue("?attr" !in text && "@android:" !in text, text)
    }

    @Test
    fun theSettingsArrowIsBridged() {
        // the icon that crashed Settings → Now Playing
        val text = File(drawables, "navigate_next.xml").readText()
        assertTrue("24.0dp\"" in text && "dip\"" !in text, text)
    }
}
