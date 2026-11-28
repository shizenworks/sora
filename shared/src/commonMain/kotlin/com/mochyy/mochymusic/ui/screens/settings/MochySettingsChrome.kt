// MochySettingsChrome.kt

package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.ui.component.SubSettingsSearchBar
import com.mochyy.mochymusic.ui.utils.stableStatusBars
import androidx.compose.foundation.layout.WindowInsets

/**
 * The settings list's header as each platform draws it. Android provides GlassKit's frosted backdrop
 * and the real search bar with the profile avatar (app: AppMochySettingsChrome); desktop and iOS get
 * the plain header below. Same shape as LocalSubSettingsChrome, so a platform never loses the glass
 * on Android just because the list moved to shared.
 */
@Stable
interface MochySettingsChrome {
    /** how much room the header takes at rest; the list starts under it */
    @Composable
    fun topBarHeight(titleBlockHeight: Dp): Dp

    /** one per list composition, shared by the backdrop and the header so the glass is the same capture */
    @Composable
    fun rememberGlass(): SettingsListGlass

    /** the scrolling list, captured as the header's backdrop where the platform has glass */
    fun backdrop(glass: SettingsListGlass, isActive: () -> Boolean): Modifier

    @Composable
    fun TopBar(
        glass: SettingsListGlass,
        collapseProgress: () -> Float,
        title: String,
        titleBlockHeight: Dp,
        titleContent: @Composable () -> Unit,
        profilePicUri: String?,
        onAvatarClick: () -> Unit,
        query: TextFieldValue,
        onQueryChange: (TextFieldValue) -> Unit,
        isSearchFocused: Boolean,
        onFocusChanged: (Boolean) -> Unit,
    )
}

/** An opaque handle to whatever the platform's header draws its glass from; commonMain never reads it. */
interface SettingsListGlass

/** Desktop and iOS: a plain header with the title and the shared search pill, no glass. */
object PlainMochySettingsChrome : MochySettingsChrome {
    @Composable
    override fun topBarHeight(titleBlockHeight: Dp): Dp {
        val statusBar = WindowInsets.stableStatusBars.asPaddingValues().calculateTopPadding()
        return statusBar + 16.dp + titleBlockHeight + 14.dp + 52.dp + 16.dp
    }

    @Composable
    override fun rememberGlass(): SettingsListGlass = PlainSettingsListGlass

    override fun backdrop(glass: SettingsListGlass, isActive: () -> Boolean): Modifier = Modifier

    @Composable
    override fun TopBar(
        glass: SettingsListGlass,
        collapseProgress: () -> Float,
        title: String,
        titleBlockHeight: Dp,
        titleContent: @Composable () -> Unit,
        profilePicUri: String?,
        onAvatarClick: () -> Unit,
        query: TextFieldValue,
        onQueryChange: (TextFieldValue) -> Unit,
        isSearchFocused: Boolean,
        onFocusChanged: (Boolean) -> Unit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(top = WindowInsets.stableStatusBars.asPaddingValues().calculateTopPadding())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.height(titleBlockHeight)) { titleContent() }
            Spacer(Modifier.height(14.dp))
            SubSettingsSearchBar(
                query = query.text,
                onQueryChange = { onQueryChange(TextFieldValue(it)) },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

private object PlainSettingsListGlass : SettingsListGlass

/** Rows that only Android can open. Desktop and iOS pass their own (all off) so nothing points nowhere. */
@Immutable
data class SettingsListPlatform(
    val audioLab: Boolean = false,
    val customisation: Boolean = false,
    val modules: Boolean = false,
    val devices: Boolean = false,
)
