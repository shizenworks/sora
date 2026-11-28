package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.AccentColorKey
import com.mochyy.mochymusic.constants.AccentFromArtKey
import com.mochyy.mochymusic.constants.CustomBoldFontKey
import com.mochyy.mochymusic.constants.CustomFontKey
import com.mochyy.mochymusic.constants.DefaultOpenTabKey
import com.mochyy.mochymusic.constants.HideNavOnScrollKey
import com.mochyy.mochymusic.constants.SystemFontKey
import com.mochyy.mochymusic.constants.TabletLayoutKey
import com.mochyy.mochymusic.constants.UiScalePercentKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.arrow_downward
import com.mochyy.mochymusic.shared.resources.close
import com.mochyy.mochymusic.shared.resources.edit
import com.mochyy.mochymusic.shared.resources.fullscreen
import com.mochyy.mochymusic.shared.resources.home
import com.mochyy.mochymusic.shared.resources.nav_bar
import com.mochyy.mochymusic.shared.resources.palette
import com.mochyy.mochymusic.shared.resources.screen_rotation
import com.mochyy.mochymusic.shared.resources.settings_accent
import com.mochyy.mochymusic.shared.resources.settings_accent_art
import com.mochyy.mochymusic.shared.resources.settings_appearance
import com.mochyy.mochymusic.shared.resources.settings_custom_font
import com.mochyy.mochymusic.shared.resources.settings_custom_font_bold
import com.mochyy.mochymusic.shared.resources.settings_custom_font_bold_desc
import com.mochyy.mochymusic.shared.resources.settings_custom_font_desc
import com.mochyy.mochymusic.shared.resources.settings_custom_font_remove
import com.mochyy.mochymusic.shared.resources.settings_hide_nav_on_scroll
import com.mochyy.mochymusic.shared.resources.settings_hide_nav_on_scroll_desc
import com.mochyy.mochymusic.shared.resources.settings_nav_bar
import com.mochyy.mochymusic.shared.resources.settings_nav_bar_desc
import com.mochyy.mochymusic.shared.resources.settings_start_tab
import com.mochyy.mochymusic.shared.resources.settings_system_font
import com.mochyy.mochymusic.shared.resources.settings_system_font_desc
import com.mochyy.mochymusic.shared.resources.settings_system_font_overridden
import com.mochyy.mochymusic.shared.resources.settings_tablet_layout
import com.mochyy.mochymusic.shared.resources.settings_tablet_layout_desc
import com.mochyy.mochymusic.shared.resources.settings_ui_scale
import com.mochyy.mochymusic.shared.resources.translate
import com.mochyy.mochymusic.shared.ui.theme.MochyAccentPresets
import com.mochyy.mochymusic.ui.component.DefaultPlayerSeekBarSlider
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.component.SubSettingsSearchBar
import com.mochyy.mochymusic.ui.theme.InterFontFamily
import com.mochyy.mochymusic.utils.LocalAppPreferences
import com.mochyy.mochymusic.utils.rememberPreference
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The custom font rows' platform half. Picking a file is android's own document picker (an ActivityResult)
 * and the picked file is copied into the app's storage there, so :app hands these four actions in. Without
 * them (a platform with no font picker yet) the two custom font rows are left out.
 */
data class CustomFontActions(
    val pickRegular: () -> Unit,
    val pickBold: () -> Unit,
    val removeRegular: () -> Unit,
    val removeBold: () -> Unit,
)

/** one choice for the tab the app opens on: the saved value (:app's NavigationTab name) and what it reads as */
data class StartTabChoice(val value: String, val label: String)

// settings > appearance: the tabs, the layout and the colours the app opens with.
//
// startTabs is the app's NavigationTab list (the enum lives beside Screens, in :app), customFonts the file
// picker and customThemeRows the app's theme editor, which isn't shared yet; each comes from the host and a
// platform without one leaves those rows out.
@Composable
fun AppearanceSettingsScreen(
    navController: NavController,
    startTabs: List<StartTabChoice> = emptyList(),
    customFonts: CustomFontActions? = null,
    customThemeRows: List<SettingsItem> = emptyList(),
) {
    var query by remember { mutableStateOf("") }

    SubSettingsScaffold(
        title = stringResource(Res.string.settings_appearance),
        onBack = { navController.navigateUp() },
        searchBar = { SubSettingsSearchBar(query = query, onQueryChange = { query = it }) },
    ) {
        SettingsSection(
            title = null,
            items = (startItems(startTabs) + navBarItems(navController) + accentItems() + customThemeRows).matching(query),
        )
        SettingsSection(title = null, items = (scaleItems() + tabletItems() + fontItems(customFonts)).matching(query))
        Spacer(Modifier.height(140.dp))
    }
}

@Composable
private fun startItems(startTabs: List<StartTabChoice>): List<SettingsItem> {
    if (startTabs.isEmpty()) return emptyList()
    // the value is the enum name NavigationTab saves, read as text so the enum can stay in :app
    val (startTab, setStartTab) = rememberPreference(DefaultOpenTabKey, defaultValue = "HOME")
    // a save the enum no longer knows reads as the first tab, the way rememberEnumPreference's toEnum did
    val selectedTab = startTab.takeIf { value -> startTabs.any { it.value == value } } ?: startTabs.first().value
    val title = stringResource(Res.string.settings_start_tab)
    return listOf(
        // the choices sit in the row itself, so there is no separate sheet to open
        SettingsItem(
            title = { Text(title) },
            highlightKey = "Default start tab",
            searchText = title,
            description = { TabChoices(choices = startTabs, selected = selectedTab, onPick = { setStartTab(it.value) }) },
            icon = painterResource(Res.drawable.home),
            iconShape = CircleShape,
        ),
    )
}

@Composable
private fun navBarItems(navController: NavController): List<SettingsItem> {
    val (hideOnScroll, setHideOnScroll) = rememberPreference(HideNavOnScrollKey, defaultValue = false)
    return listOf(
        linkItem(
            key = "Navigation bar",
            title = stringResource(Res.string.settings_nav_bar),
            description = stringResource(Res.string.settings_nav_bar_desc),
            icon = painterResource(Res.drawable.nav_bar),
            onClick = { navController.navigate("nav_bar_settings") },
        ),
        switchItem(
            key = "Hide navigation bar on scroll",
            title = stringResource(Res.string.settings_hide_nav_on_scroll),
            description = stringResource(Res.string.settings_hide_nav_on_scroll_desc),
            icon = painterResource(Res.drawable.arrow_downward),
            checked = hideOnScroll,
            onCheckedChange = setHideOnScroll,
        ),
    )
}

// the steps the scale slider stops on, from smaller than a phone's own size to a little bigger
private val UiScaleSteps = listOf(85, 90, 95, 100, 105, 110, 115)

@Composable
private fun scaleItems(): List<SettingsItem> {
    val (scale, setScale) = rememberPreference(UiScalePercentKey, defaultValue = 100)
    val title = stringResource(Res.string.settings_ui_scale)
    return listOf(
        SettingsItem(
            title = { ValueTitle(title, "$scale%") },
            highlightKey = "UI scale",
            searchText = title,
            description = {
                DefaultPlayerSeekBarSlider(
                    value = scale.toFloat(),
                    onValueChange = { setScale(it.roundToInt()) },
                    valueRange = 85f..115f,
                    steps = UiScaleSteps.size - 2,
                    modifier = Modifier.padding(top = 8.dp),
                )
            },
            icon = painterResource(Res.drawable.fullscreen),
            iconShape = CircleShape,
        ),
    )
}

@Composable
private fun fontItems(customFonts: CustomFontActions?): List<SettingsItem> {
    val (systemFont, setSystemFont) = rememberPreference(SystemFontKey, defaultValue = false)
    val (customFont) = rememberPreference(CustomFontKey, defaultValue = "")
    val (customBoldFont) = rememberPreference(CustomBoldFontKey, defaultValue = "")
    val hasCustom = customFont.isNotEmpty()
    val systemFontRow = switchItem(
        key = "System font",
        title = stringResource(Res.string.settings_system_font),
        description = stringResource(
            if (hasCustom) Res.string.settings_system_font_overridden else Res.string.settings_system_font_desc,
        ),
        icon = painterResource(Res.drawable.translate),
        checked = systemFont,
        onCheckedChange = setSystemFont,
        // a custom font stands in for the system font too, so the switch waits, greyed out and as it was, until that's removed
        enabled = !hasCustom,
    )
    // a platform with no font picker leaves the two rows out; the system font switch works either way
    if (customFonts == null) return listOf(systemFontRow)
    val actions = customFonts
    val customTitle = stringResource(Res.string.settings_custom_font)
    val boldTitle = stringResource(Res.string.settings_custom_font_bold)
    val removeLabel = stringResource(Res.string.settings_custom_font_remove)
    @Composable
    fun RemoveButton(onClick: () -> Unit) {
        IconButton(onClick = onClick) {
            Icon(painterResource(Res.drawable.close), contentDescription = removeLabel, tint = Color.White.copy(alpha = 0.7f))
        }
    }
    val removeCustom: (@Composable () -> Unit)? = if (hasCustom) { { RemoveButton(actions.removeRegular) } } else null
    val removeBold: (@Composable () -> Unit)? = if (customBoldFont.isNotEmpty()) { { RemoveButton(actions.removeBold) } } else null
    val customDescription = stringResource(Res.string.settings_custom_font_desc)
    val boldDescription = stringResource(Res.string.settings_custom_font_bold_desc)
    return listOf(
        systemFontRow,
        // the picked file is copied into the app, so the font stays when the original is moved or deleted
        SettingsItem(
            title = { Text(customTitle) },
            highlightKey = "Custom font",
            searchText = customTitle,
            descriptionText = customFont.ifEmpty { customDescription },
            icon = painterResource(Res.drawable.edit),
            iconShape = CircleShape,
            onClick = actions.pickRegular,
            trailingContent = removeCustom,
        ),
        SettingsItem(
            title = { Text(boldTitle) },
            highlightKey = "Custom bold font",
            searchText = boldTitle,
            descriptionText = customBoldFont.ifEmpty { boldDescription },
            icon = painterResource(Res.drawable.edit),
            iconShape = CircleShape,
            isVisible = hasCustom,
            isSubOption = true,
            onClick = actions.pickBold,
            trailingContent = removeBold,
        ),
    )
}

@Composable
private fun tabletItems(): List<SettingsItem> {
    val (tablet, setTablet) = rememberPreference(TabletLayoutKey, defaultValue = false)
    return listOf(
        switchItem(
            key = "Tablet layout",
            title = stringResource(Res.string.settings_tablet_layout),
            description = stringResource(Res.string.settings_tablet_layout_desc),
            icon = painterResource(Res.drawable.screen_rotation),
            checked = tablet,
            onCheckedChange = setTablet,
        ),
    )
}

@Composable
private fun accentItems(): List<SettingsItem> {
    val (accentInt) = rememberPreference(AccentColorKey, defaultValue = 0)
    val (fromArt) = rememberPreference(AccentFromArtKey, defaultValue = false)
    // the app's own store (the SecretPreferences one on android), as the app has always written it
    val store = LocalAppPreferences.current
    val scope = rememberCoroutineScope()
    fun selectAccent(color: Int?) {
        scope.launch {
            store.edit { preferences ->
                if (color != null) preferences[AccentColorKey] = color
                preferences[AccentFromArtKey] = color == null
            }
        }
    }
    val title = stringResource(Res.string.settings_accent)
    return listOf(
        SettingsItem(
            title = { Text(title) },
            highlightKey = "Accent colour",
            searchText = title,
            description = {
                AccentChoices(
                    picked = accentInt,
                    fromArt = fromArt,
                    onPick = ::selectAccent,
                    onPickArt = { selectAccent(null) },
                )
            },
            icon = painterResource(Res.drawable.palette),
            iconShape = CircleShape,
        ),
    )
}

@Composable
private fun AccentChoices(picked: Int, fromArt: Boolean, onPick: (Int) -> Unit, onPickArt: () -> Unit) {
    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 0 is mochy's own grey, the first swatch puts it back
            Swatch(color = Color(0xFF8E8E8E), selected = !fromArt && picked == 0) { onPick(0) }
            MochyAccentPresets.forEach { argb ->
                Swatch(color = Color(argb), selected = !fromArt && picked == argb) { onPick(argb) }
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (fromArt) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                .clickable { onPickArt() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_accent_art),
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                fontWeight = if (fromArt) FontWeight.Bold else FontWeight.Normal,
                color = Color.White,
            )
        }
    }
}

// the colour pills the accent and theme pages pick from; the app's theme creator draws them too
@Composable
fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (selected) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun TabChoices(choices: List<StartTabChoice>, selected: String, onPick: (StartTabChoice) -> Unit) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choices.forEach { choice ->
            val on = choice.value == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (on) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                    .clickable { onPick(choice) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = choice.label,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White,
                )
            }
        }
    }
}
