// MochyShell.kt
// The app frame shared by desktop and iOS: Mochy's tabs on the side (wide windows) or along the bottom
// (narrow ones), each tab's screen beside them. Screens join one at a time as they move from :app.

package com.mochyy.mochymusic.shared.ui.shell

import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.movableContentOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.foundation.Image
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.home
import com.mochyy.mochymusic.shared.resources.nav_home
import com.mochyy.mochymusic.shared.resources.nav_home_filled
import com.mochyy.mochymusic.shared.resources.nav_lib
import com.mochyy.mochymusic.shared.resources.nav_lib_fill
import com.mochyy.mochymusic.shared.resources.nav_search
import com.mochyy.mochymusic.shared.resources.nav_search_filled
import com.mochyy.mochymusic.shared.resources.nav_settings
import com.mochyy.mochymusic.shared.resources.nav_settings_filled
import com.mochyy.mochymusic.shared.resources.search
import com.mochyy.mochymusic.shared.resources.settings
import com.mochyy.mochymusic.shared.resources.shell_coming_body
import com.mochyy.mochymusic.shared.resources.shell_coming_title
import com.mochyy.mochymusic.shared.resources.shell_credit
import com.mochyy.mochymusic.shared.resources.your_library
import com.mochyy.mochymusic.shared.ui.theme.LocalMochyAccent
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Mochy's shared destinations, with the Android app's own icons. */
enum class ShellTab(
    val title: StringResource,
    val icon: DrawableResource,
    val iconActive: DrawableResource,
) {
    Home(Res.string.home, Res.drawable.nav_home, Res.drawable.nav_home_filled),
    Search(Res.string.search, Res.drawable.nav_search, Res.drawable.nav_search_filled),
    Library(Res.string.your_library, Res.drawable.nav_lib, Res.drawable.nav_lib_fill),
    Settings(Res.string.settings, Res.drawable.nav_settings, Res.drawable.nav_settings_filled),
}

// at or above this width the tabs stand in a rail on the left, the way a desktop window reads them
private val RailBreakpoint = 600.dp

/**
 * The frame every non-Android platform opens into. [content] draws a tab's screen; a tab it doesn't
 * know yet falls back to [ComingSoon]. The width, not the platform, picks rail or bar, so a narrow
 * desktop window and an iPad in split view both get the layout that fits them.
 */
@Composable
fun MochyShell(
    modifier: Modifier = Modifier,
    initialTab: ShellTab = ShellTab.Home,
    /** the mini player, under every tab */
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (ShellTab) -> Boolean = { false },
) {
    var selected by rememberSaveable { mutableStateOf(initialTab) }
    val currentContent by rememberUpdatedState(content)
    // the rail and the bar layouts each place the screen somewhere else; movable content carries the
    // screen, with its own state and back stack (Settings' NavHost), from one to the other when the
    // window crosses RailBreakpoint, instead of dropping it and starting the tab over
    val screen = remember {
        movableContentOf { m: Modifier ->
            Box(m) { if (!currentContent(selected)) ComingSoon(selected) }
        }
    }
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val wide = maxWidth >= RailBreakpoint
        if (wide) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxHeight().width(88.dp).padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ShellTab.entries.forEach { tab ->
                            TabButton(tab, tab == selected, onClick = { selected = tab })
                        }
                    }
                    screen(Modifier.weight(1f).fillMaxHeight())
                }
                bottomBar()
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                screen(Modifier.weight(1f).fillMaxWidth())
                bottomBar()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ShellTab.entries.forEach { tab ->
                        TabButton(tab, tab == selected, onClick = { selected = tab })
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(tab: ShellTab, active: Boolean, onClick: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val tint = if (active) LocalMochyAccent.current ?: onSurface else onSurface.copy(alpha = 0.5f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Image(
            painter = painterResource(if (active) tab.iconActive else tab.icon),
            contentDescription = stringResource(tab.title),
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(stringResource(tab.title), color = tint, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun ComingSoon(tab: ShellTab) {
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text(stringResource(tab.title), style = MaterialTheme.typography.displaySmall, color = Color.White)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(Res.string.shell_coming_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.shell_coming_body),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(Res.string.shell_credit),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.35f),
        )
    }
}
