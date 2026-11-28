package com.mochyy.mochymusic.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.preferences.core.emptyPreferences
import com.mochyy.mochymusic.constants.AccentColorKey
import com.mochyy.mochymusic.constants.PureBlackKey
import com.mochyy.mochymusic.shared.settings.PlaybackPreferences
import com.mochyy.mochymusic.ui.theme.AppFont
import com.mochyy.mochymusic.ui.theme.InterFontFamily

/** the desktop and iOS theme: [inter] is this platform's Inter, handed to [AppFont] before anything reads it */
@Composable
internal fun StandaloneMochyTheme(
    inter: FontFamily,
    settings: PlaybackPreferences,
    content: @Composable () -> Unit,
) {
    // written before the first read below, so the first frame is already in inter
    remember(inter) { AppFont.inter = inter }
    val preferences by settings.values.collectAsState(initial = emptyPreferences())
    val accent = preferences[AccentColorKey]?.takeIf { it != 0 }?.let { Color(it) }
    // the same pick as android's: a look's font, a custom font, the system font, then inter
    MochySharedTheme(InterFontFamily, pureBlack = preferences[PureBlackKey] ?: false, accent = accent, content = content)
}
