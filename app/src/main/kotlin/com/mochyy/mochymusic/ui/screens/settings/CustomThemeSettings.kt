// CustomThemeSettings.kt

package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.AccentColorKey
import com.mochyy.mochymusic.constants.AccentFromArtKey
import com.mochyy.mochymusic.constants.CustomTheme
import com.mochyy.mochymusic.constants.CustomThemesKey
import com.mochyy.mochymusic.constants.decodeCustomThemes
import com.mochyy.mochymusic.constants.encodeCustomThemes
import com.mochyy.mochymusic.shared.ui.theme.MochyAccentPresets
import com.mochyy.mochymusic.ui.component.DefaultDialog
import androidx.compose.material3.MaterialTheme
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.theme.InterFontFamily
import com.mochyy.mochymusic.utils.dataStore
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.launch

// a saved theme is a seed colour; applying one sets the accent, so the palette comes from the same path as the accent picker
@Composable
internal fun customThemeItems(): List<SettingsItem> {
    val title = stringResource(R.string.settings_custom_themes)
    return listOf(
        SettingsItem(
            title = { Text(title) },
            highlightKey = "Custom themes",
            searchText = title,
            description = {
                Column {
                    Text(stringResource(R.string.settings_custom_themes_hint), style = MaterialTheme.typography.bodySmall)
                    ThemeChoices()
                }
            },
            icon = painterResource(R.drawable.palette),
            iconShape = CircleShape,
        ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeChoices() {
    val (raw) = rememberPreference(CustomThemesKey, defaultValue = "")
    val (accentInt) = rememberPreference(AccentColorKey, defaultValue = 0)
    val (fromArt) = rememberPreference(AccentFromArtKey, defaultValue = false)
    val themes = remember(raw) { decodeCustomThemes(raw) }
    val store = LocalContext.current.applicationContext.dataStore
    val scope = rememberCoroutineScope()

    // editing is the saved theme being changed, null for a new one; the editor is closed while editorOpen is false
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CustomTheme?>(null) }

    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            themes.forEach { theme ->
                val on = !fromArt && accentInt == theme.seed
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
                        .combinedClickable(
                            onClick = {
                                scope.launch {
                                    store.edit {
                                        it[AccentColorKey] = theme.seed
                                        it[AccentFromArtKey] = false
                                    }
                                }
                            },
                            onLongClick = {
                                editing = theme
                                editorOpen = true
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(theme.seed)),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = theme.name,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.08f))
                    .combinedClickable(onClick = {
                        editing = null
                        editorOpen = true
                    })
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.theme_new),
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = Color.White,
                )
            }
        }
    }

    if (editorOpen) {
        ThemeEditorDialog(
            theme = editing,
            onDismiss = { editorOpen = false },
            onSave = { saved ->
                editorOpen = false
                scope.launch {
                    store.edit { prefs ->
                        val current = decodeCustomThemes(prefs[CustomThemesKey].orEmpty())
                        val old = editing
                        val next = if (old == null) current + saved else current.map { if (it == old) saved else it }
                        prefs[CustomThemesKey] = encodeCustomThemes(next)
                    }
                }
            },
            onDelete = editing?.let { old ->
                {
                    editorOpen = false
                    scope.launch {
                        store.edit { prefs ->
                            prefs[CustomThemesKey] = encodeCustomThemes(decodeCustomThemes(prefs[CustomThemesKey].orEmpty()).filter { it != old })
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun ThemeEditorDialog(
    theme: CustomTheme?,
    onDismiss: () -> Unit,
    onSave: (CustomTheme) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember(theme) { mutableStateOf(theme?.name.orEmpty()) }
    var seed by remember(theme) { mutableIntStateOf(theme?.seed ?: MochyAccentPresets.first()) }

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(stringResource(if (theme == null) R.string.theme_new else R.string.theme_edit)) },
        buttons = {
            if (onDelete != null) {
                TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = { onSave(CustomTheme(name.trim(), seed)) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text(stringResource(R.string.theme_name)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.theme_colour),
            fontFamily = InterFontFamily,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.70f),
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MochyAccentPresets.forEach { argb ->
                Swatch(color = Color(argb), selected = seed == argb) { seed = argb }
            }
        }
    }
}
