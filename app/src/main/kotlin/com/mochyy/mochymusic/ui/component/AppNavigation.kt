// AppNavigation.kt

package com.mochyy.mochymusic.ui.component

import com.mochyy.mochymusic.ui.component.liquid.liquidGlassSurface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.mochyy.mochymusic.ui.screens.Screens
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import com.mochyy.mochymusic.ui.component.liquid.glassGlowOverlay
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import com.mochyy.mochymusic.LocalGlassState
import com.mochyy.mochymusic.constants.ShowNavTabLabelsKey
import com.mochyy.mochymusic.utils.rememberPreference

@Immutable
private data class NavItemState(
    val isSelected: Boolean,
    val iconRes: Int
)

/**
 * Navigates to a top-level tab exactly the way the navigation bar does.
 *
 * Anything that jumps to a tab from inside another tab MUST use this rather than a plain
 * `navigate(route)`, and the reason is subtle enough to be worth spelling out.
 *
 * A plain `navigate("settings")` from the profile menu pushes Settings on top of the *Home* tab's
 * stack, because that is where you were standing. Tapping Home in the bar then runs
 * `popUpTo(startDestination) { saveState = true }`, which sweeps that pushed Settings entry into
 * the state saved *for Home* — and `restoreState = true` on the very same call immediately puts it
 * back. The result is that tapping Home lands you on Settings, which is precisely the bug this
 * fixes, and it looks like the bar is ignoring you when in fact it is faithfully restoring a stack
 * that Settings should never have been part of.
 *
 * Going through here instead means a tab is always entered *as* a tab, so it never becomes a child
 * of whichever tab happened to be on screen when you left.
 */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * a tap on a tab. another tab: switch to it, back on the page you left it on. the tab you're in:
 * from one of its pages, back to the tab's own page (as every tab bar does); already there, the
 * page scrolls to its top.
 */
fun NavController.onTabTapped(screen: Screens, isSelectedTab: Boolean, scrollToTop: () -> Unit) {
    if (!isSelectedTab) {
        navigateToTab(screen.route)
        return
    }
    if (currentDestination?.route == screen.route) {
        currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
        scrollToTop()
        return
    }
    if (!popBackStack(screen.route, inclusive = false)) navigateToTab(screen.route)
}

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    return navigationItems.any { it.route == screenRoute } &&
           currentRoute.startsWith("$screenRoute/")
}

private val RouteOwners: List<Pair<String, String>> = listOf(
    "search/" to "search_input",
    "genre/" to "search_input",
    "youtube_browse/" to "search_input",
    "browse/" to "search_input",
    "library/" to "library",
    "cipher_settings" to "settings",
    "playback_diagnostics" to "settings",
    "mochy_settings" to "settings",
    "general_settings" to "settings",
    "playback_settings" to "settings",
    "now_playing_settings" to "settings",
    "experimental_settings" to "settings",
    "developer_space" to "settings",
    "developer_playground" to "settings",
    "storage_settings" to "settings",
    "import_sync" to "settings",
    "import_providers" to "settings",
    "import_tunemymusic" to "settings",
    "import_progress" to "settings",
    "youtube_sync" to "settings",
    "youtube_login" to "settings",
    "account_import/" to "settings",
    "equalizer" to "settings",
)

private fun owningTabRoute(currentRoute: String?, navigationItems: List<Screens>): String? {
    if (currentRoute == null) return null
    navigationItems.firstOrNull { isRouteSelected(currentRoute, it.route, navigationItems) }
        ?.let { return it.route }
    RouteOwners.firstOrNull { (prefix, _) ->
        currentRoute == prefix || currentRoute.startsWith(prefix)
    }?.let { return it.second }

    // Catch-all so RouteOwners does not have to stay exhaustive: every settings sub-screen is
    // owned by the settings tab without anyone having to list it. Only a fallback now; the back
    // stack decides the tab whenever it has a tab page in it (see owningTab).
    if (currentRoute.endsWith("_settings") || currentRoute.startsWith("settings")) return "settings"
    return null
}

/**
 * the tab the page on screen belongs to: the nearest tab page under it in the back stack.
 *
 * the bar used to remember the last tab it could name from the current route alone, and keep it
 * for routes no tab claims (albums, playlists, artists). that went wrong two ways: switching back
 * to a tab restores the page you left it on, so coming back to the Library's album kept Home lit;
 * and the remembered value was dropped whenever the bar left the screen (the player's full view,
 * the recap), so it came back as Home. the back stack always knows: a tab is entered with
 * [navigateToTab], so every page sits above the tab page it was opened from.
 */
fun owningTab(backStack: List<NavBackStackEntry>, navigationItems: List<Screens>): String {
    val tabRoutes = navigationItems.mapTo(HashSet()) { it.route }
    backStack.asReversed().firstOrNull { it.destination.route in tabRoutes }
        ?.destination?.route
        ?.let { return it }
    // a stack without a tab page under it (a deep link): go by the route, else the first tab
    return owningTabRoute(backStack.lastOrNull()?.destination?.route, navigationItems)
        ?: navigationItems.firstOrNull()?.route.orEmpty()
}

/**
 * which tab each page belongs to, kept by entry so a page that has already left the stack (the
 * one a tab switch saved away) can still be placed. the tab slide reads this: an album open in
 * the Library slides like the Library, not like a page no tab owns.
 */
class TabOwnership(private val navigationItems: List<Screens>) {
    private val tabRoutes = navigationItems.map { it.route }
    private val owners = HashMap<String, String>()

    fun update(backStack: List<NavBackStackEntry>) {
        var owner: String? = null
        backStack.forEach { entry ->
            val route = entry.destination.route
            if (route != null && route in tabRoutes) owner = route
            owner?.let { owners[entry.id] = it }
        }
        // saved tab stacks keep their entries' ids, so only trim when it has clearly grown stale
        if (owners.size > 400) {
            val live = backStack.mapTo(HashSet()) { it.id }
            owners.keys.retainAll(live)
        }
    }

    /** the tab's position in the bar, or -1 when nothing places it */
    fun indexOf(entry: NavBackStackEntry, backStack: List<NavBackStackEntry>): Int {
        update(backStack)
        val owner = owners[entry.id] ?: entry.destination.route?.takeIf { it in tabRoutes }
            ?: owningTabRoute(entry.destination.route, navigationItems)
        return tabRoutes.indexOf(owner)
    }
}

/**
 * how far the nav bar has slid away: 0 shown, 1 gone. only read inside graphicsLayer lambdas, so a
 * scroll moves the bar without recomposing the screen for every pixel.
 */
@Stable
class NavBarHide {
    var progress by mutableFloatStateOf(0f)
}

/**
 * slides the bar away while a list scrolls down and brings it back when it scrolls up. it only
 * watches the scroll and never takes any of it, so the list still scrolls exactly as before.
 */
@Composable
fun rememberNavBarHideConnection(state: NavBarHide, barHeightPx: Float, enabled: Boolean): NestedScrollConnection =
    remember(state, barHeightPx, enabled) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // scrolling down moves the content up (consumed.y < 0), which hides the bar
                if (enabled && barHeightPx > 0f) {
                    state.progress = (state.progress - consumed.y / barHeightPx).coerceIn(0f, 1f)
                }
                return Offset.Zero
            }

            // a fling settles the bar: past halfway it finishes hiding, otherwise it comes back
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (enabled) {
                    val target = if (state.progress > 0.5f) 1f else 0f
                    animate(state.progress, target) { value, _ -> state.progress = value }
                }
                return Velocity.Zero
            }
        }
    }

/** one tab in the bar's settings: where it sits and whether it is shown */
data class NavTabSlot(val screen: Screens, val shown: Boolean)

/**
 * the tabs in the saved order, shown or hidden. a tab missing from the save (a new one, or a blank
 * save) comes last and is shown. Settings is always in the bar so the app can never be left without it.
 */
// the stored format lives in shared (NavBarSettingsScreen.kt), so the bar and its settings page agree
fun navTabSlots(saved: String): List<NavTabSlot> =
    com.mochyy.mochymusic.ui.screens.settings.decodeNavTabOrder(saved, Screens.MainScreens) { it.route }
        .map { (screen, shown) -> NavTabSlot(screen, shown) }

fun encodeNavTabs(slots: List<NavTabSlot>): String =
    com.mochyy.mochymusic.ui.screens.settings.encodeNavTabOrder(slots.map { it.screen to it.shown }) { it.route }

/** the tabs the bar draws, in order */
fun navigationTabs(saved: String): List<Screens> =
    navTabSlots(saved).filter { it.shown || it.screen == Screens.Settings }.map { it.screen }

/** The bar itself, a stadium the mini player's 64dp matches. */
internal val NavBarHeight = 64.dp
internal val NavBarShape = RoundedCornerShape(50)

/** How bright an unlit tab's icon is. */
private const val UnlitIconAlpha = 0.7f

@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    /** from [owningTab]: the tab the page on screen belongs to */
    selectedTabRoute: String,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    /** a second quick tap on Search: straight into typing */
    onSearchDoubleTap: (() -> Unit)? = null,
    /** "module/page" while one of a module's pages is on screen, so a look's bar can light its tab */
    currentPage: String? = null,
    /** opens a look's own tab page */
    onOpenRoute: (String) -> Unit = {},
    /** the player is over the app, so a look's moving bar can rest */
    obscured: () -> Boolean = { false },
) {
    // a look with a nav bar of its own draws it in this one's place
    val moduleBar = com.mochyy.mochymusic.modules.skin.ActiveSkin.bar
    if (moduleBar != null) {
        ModuleNavBar(
            active = moduleBar,
            navigationItems = navigationItems,
            selectedTabRoute = selectedTabRoute,
            currentPage = currentPage,
            onItemClick = onItemClick,
            onOpenRoute = onOpenRoute,
            onSearchDoubleTap = onSearchDoubleTap,
            pureBlack = pureBlack,
            obscured = obscured,
            modifier = modifier,
        )
        return
    }
    val glassState = LocalGlassState.current ?: remember { GlassState() }
    val accentWash = rememberChromeAccentWash()
    val selectedIndex = navigationItems.indexOfFirst { it.route == selectedTabRoute }
    val tabCount = navigationItems.size
    val pill = rememberNavPill(selectedIndex)
    // a look's glass can draw its own edge and its own pill, and then mochy leaves its hairline, shadow or highlight out
    val moduleGlass = rememberChromeModuleGlass()
    val density = LocalDensity.current
    val glassPill = remember(pill, tabCount, density) {
        { bar: Size -> pill.forGlass(bar, with(density) { NavRowPadding.toPx() }, tabCount, density) }
    }

    val liquid = com.mochyy.mochymusic.ui.component.liquid.LocalLiquidGlass.current
    val liquidBar = liquid.surface(liquid.navBar)
    val style = rememberNavBarStyle()
    // the dimension dials shape whichever bar is drawing: the corner never runs past the
    // bar's own height, whatever the height dial stands at
    val barHeight = NavBarHeight * style.heightScale
    val cornerDp = style.cornerRadius.coerceAtMost(barHeight / 2f)
    val barShape = RoundedCornerShape(cornerDp)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .fillMaxWidth(style.widthFraction)
                .height(barHeight)
                .press3D(maxTilt = 4f, pressedScale = 0.985f)
                .then(if (moduleGlass?.shadow == false) Modifier else Modifier.dropShadow(barShape, ChromeShadow))
                .clip(barShape)
                .drawWithContent {
                    drawContent()
                    if (moduleGlass?.outline != false) {
                        drawChromeOutline(Offset.Zero, size, cornerRadius = minOf(cornerDp.toPx(), size.height / 2f))
                    }
                }
                .then(if (style.glow > 0f) Modifier.glassGlowOverlay(style.glow, barShape) else Modifier)
        ) {
            if (liquidBar) {
                // the glass layer takes the dials too: the shape from the corner dial, the
                // opacity and transparency dials as how much of the glass shows at all
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(if (style.surfaceAlpha < 1f) Modifier.graphicsLayer { alpha = style.surfaceAlpha } else Modifier)
                        .liquidGlassSurface(barShape),
                )
            } else {
                FrostedChromeGlass(
                    state = glassState,
                    pureBlack = pureBlack,
                    accentWash = accentWash,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (style.surfaceAlpha < 1f) Modifier.graphicsLayer { alpha = style.surfaceAlpha } else Modifier),
                    pill = glassPill,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = NavRowPadding)
                    .navHighlight(pill, tabCount) { moduleGlass?.drawsPill != true }
                    .navPillDrag(pill, tabCount, selectedIndex) { index ->
                        navigationItems.getOrNull(index)?.let { onItemClick(it, index == selectedIndex) }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                navigationItems.forEach { screen ->
                    // lit = the tab this page belongs to. what a tap on it does (scroll to the top
                    // on the tab's own page, back to that page from deeper in) is the caller's call
                    NavTab(
                        screen = screen,
                        isSelected = screen.route == selectedTabRoute,
                        onItemClick = onItemClick,
                        onDoubleTap = onSearchDoubleTap.takeIf { screen == Screens.Search },
                        label = if (style.showLabels) style.labelSpacing else null,
                    )
                }
            }
        }
    }
}

/**
 * A tab's tap, or with [onDoubleTap] a second tap close behind the first: the first still goes to
 * the tab at once, the second does [onDoubleTap] instead. [lastTap] holds the time of the one before.
 */
internal fun tabTapped(lastTap: LongArray, doubleTapTimeoutMillis: Long, onDoubleTap: (() -> Unit)?, onTap: () -> Unit) {
    val now = android.os.SystemClock.uptimeMillis()
    if (onDoubleTap != null && now - lastTap[0] < doubleTapTimeoutMillis) {
        lastTap[0] = 0L
        onDoubleTap()
    } else {
        lastTap[0] = now
        onTap()
    }
}

/**
 * One tab of the bar. Its icon swaps to the filled one and brightens as it lights up, and it
 * squishes under a press. [onDoubleTap] is for Search, where two taps open the search field.
 * The Library's icon turns into a download badge while songs download.
 */
@Composable
internal fun RowScope.NavTab(
    screen: Screens,
    isSelected: Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onDoubleTap: (() -> Unit)?,
    // the gap under the glyph when its name shows beneath it (Neiro's Show labels); null: icon only
    label: androidx.compose.ui.unit.Dp? = null,
) {
    val (showNames) = rememberPreference(ShowNavTabLabelsKey, defaultValue = false)
    val viewConfiguration = LocalViewConfiguration.current
    val interactionSource = remember { MutableInteractionSource() }
    val lastTap = remember { longArrayOf(0L) }
    val lit = animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "nav_tab_lit"
    )

    // Both icons stay loaded, so lighting a tab never shows an empty frame while its filled icon loads.
    val unlitIcon = coil3.compose.rememberAsyncImagePainter(model = screen.iconIdInactive)
    val litIcon = coil3.compose.rememberAsyncImagePainter(model = screen.iconIdActive)
    val iconSize = if (screen == Screens.Home || screen == Screens.Search) 22.dp else 24.dp

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .pressBounce(interactionSource, pressedScale = 0.85f)
            .clickable(
                interactionSource = interactionSource,
                indication = RemoteFocus,
                onClick = {
                    tabTapped(lastTap, viewConfiguration.doubleTapTimeoutMillis, onDoubleTap) {
                        onItemClick(screen, isSelected)
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // the name is read out from the icon unless it already shows as text
            val title = stringResource(screen.titleId)
            val named = label != null || showNames
            val iconDescription = title.takeUnless { named }
            if (screen == Screens.Library) {
                LibraryDownloadIcon { TabIcons(iconDescription, unlitIcon, litIcon, iconSize) { lit.value } }
            } else {
                TabIcons(iconDescription, unlitIcon, litIcon, iconSize) { lit.value }
            }
            if (named) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(label ?: 3.dp))
                androidx.compose.material3.Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = androidx.compose.material3.LocalContentColor.current.copy(alpha = UnlitIconAlpha + (1f - UnlitIconAlpha) * lit.value),
                    maxLines = 1,
                )
            }
        }
    }
}

/** a tab's two icons, the filled one fading in over the outline as the tab lights up */
@Composable
private fun TabIcons(
    description: String?,
    unlitIcon: androidx.compose.ui.graphics.painter.Painter,
    litIcon: androidx.compose.ui.graphics.painter.Painter,
    iconSize: androidx.compose.ui.unit.Dp,
    lit: () -> Float,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            painter = unlitIcon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = UnlitIconAlpha * (1f - lit())
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
        )
        Icon(
            painter = litIcon,
            contentDescription = null,
            tint = com.mochyy.mochymusic.ui.theme.LocalChromeAccent.current ?: Color.White,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    alpha = lit()
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
        )
    }
}
