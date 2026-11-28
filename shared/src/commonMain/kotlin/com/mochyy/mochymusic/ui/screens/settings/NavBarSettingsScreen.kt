package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.arrow_downward
import com.mochyy.mochymusic.shared.resources.arrow_upward
import com.mochyy.mochymusic.shared.resources.settings_nav_bar
import com.mochyy.mochymusic.shared.resources.settings_nav_bar_always
import com.mochyy.mochymusic.shared.resources.settings_nav_move_down
import com.mochyy.mochymusic.shared.resources.settings_nav_move_up
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.component.SubSettingsSearchBar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * One tab the bar can hold, as :app's Screens describes it: its route is what the saved preference keeps,
 * the title and icon are what the row draws, and [locked] is the tab settings can never hide.
 */
data class NavTab(
    val route: String,
    val title: String,
    val icon: Painter,
    val locked: Boolean,
)

// settings > appearance > navigation bar: which tabs the bar shows and in what order.
//
// Screens and navTabSlots() live in :app, so the app hands the tabs in and the saved value back out; the
// saved-order decode below is the same rule the bar itself reads (AppNavigation.navTabSlots): saved tabs in
// their order, a tab missing from the save last and shown, settings always in the bar.
@Composable
fun NavBarSettingsScreen(
    navController: NavController,
    saved: String,
    onSave: (String) -> Unit,
    tabs: List<NavTab>,
) {
    var query by remember { mutableStateOf("") }
    val slots = navTabRows(saved, tabs)

    // one save for every change: the new order and shown flags go back as one string
    val save = { changed: List<NavTabRow> -> onSave(encodeNavTabs(changed)) }
    val items = slots.mapIndexed { index, slot ->
        tabItem(
            slot = slot,
            canMoveUp = index > 0,
            canMoveDown = index < slots.lastIndex,
            onMoveUp = { save(slots.swapped(index, index - 1)) },
            onMoveDown = { save(slots.swapped(index, index + 1)) },
            onShownChange = { shown -> save(slots.toMutableList().also { it[index] = slot.copy(shown = shown) }) },
        )
    }

    SubSettingsScaffold(
        title = stringResource(Res.string.settings_nav_bar),
        onBack = { navController.navigateUp() },
        searchBar = { SubSettingsSearchBar(query = query, onQueryChange = { query = it }) },
    ) {
        SettingsSection(title = null, items = items.matching(query))
        Spacer(Modifier.height(140.dp))
    }
}

/** a saved tab and whether the bar shows it */
private data class NavTabRow(val tab: NavTab, val shown: Boolean)

/**
 * NavTabsKey's stored format, the one place it is read and written: routes in bar order, a hidden tab
 * prefixed with "-" ("home,-search,library"). Unknown routes are skipped, a route seen twice keeps its
 * first place, and a tab the saved value doesn't name yet goes at the end, shown. The app's bar
 * (AppNavigation.navTabSlots) and this page both go through these two.
 */
fun <T> decodeNavTabOrder(saved: String, tabs: List<T>, route: (T) -> String): List<Pair<T, Boolean>> {
    val rows = mutableListOf<Pair<T, Boolean>>()
    saved.split(',').filter { it.isNotBlank() }.forEach { token ->
        val tab = tabs.firstOrNull { route(it) == token.removePrefix("-") } ?: return@forEach
        if (rows.none { route(it.first) == route(tab) }) rows += tab to !token.startsWith("-")
    }
    tabs.forEach { tab ->
        if (rows.none { route(it.first) == route(tab) }) rows += tab to true
    }
    return rows
}

fun <T> encodeNavTabOrder(rows: List<Pair<T, Boolean>>, route: (T) -> String): String =
    rows.joinToString(",") { (tab, shown) -> if (shown) route(tab) else "-${route(tab)}" }

private fun navTabRows(saved: String, tabs: List<NavTab>): List<NavTabRow> =
    decodeNavTabOrder(saved, tabs) { it.route }.map { (tab, shown) -> NavTabRow(tab, shown) }

private fun encodeNavTabs(rows: List<NavTabRow>): String =
    encodeNavTabOrder(rows.map { it.tab to it.shown }) { it.route }

private fun List<NavTabRow>.swapped(from: Int, to: Int): List<NavTabRow> =
    toMutableList().also { it[from] = this[to]; it[to] = this[from] }

@Composable
private fun tabItem(
    slot: NavTabRow,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onShownChange: (Boolean) -> Unit,
): SettingsItem {
    val title = slot.tab.title
    // settings is always in the bar, so its switch is locked on
    val locked = slot.tab.locked
    return SettingsItem(
        title = { Text(title) },
        highlightKey = slot.tab.route,
        searchText = title,
        descriptionText = if (locked) stringResource(Res.string.settings_nav_bar_always) else null,
        icon = slot.tab.icon,
        iconShape = CircleShape,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoveButton(Res.drawable.arrow_upward, stringResource(Res.string.settings_nav_move_up), canMoveUp, onMoveUp)
                MoveButton(Res.drawable.arrow_downward, stringResource(Res.string.settings_nav_move_down), canMoveDown, onMoveDown)
                AppSwitch(checked = slot.shown, onCheckedChange = onShownChange, enabled = !locked)
            }
        },
    )
}

@Composable
private fun MoveButton(icon: DrawableResource, description: String, enabled: Boolean, onClick: () -> Unit) {
    Icon(
        painter = painterResource(icon),
        contentDescription = description,
        tint = Color.White.copy(alpha = if (enabled) 0.8f else 0.25f),
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp),
    )
}
