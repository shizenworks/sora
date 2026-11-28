package com.mochyy.mochymusic.shared.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.Preferences
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.settings_write_failed
import com.mochyy.mochymusic.shared.resources.show_format_badge
import com.mochyy.mochymusic.shared.resources.show_format_sample_rate
import com.mochyy.mochymusic.shared.settings.PlaybackPreferenceKeys
import com.mochyy.mochymusic.shared.settings.PlaybackPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.IOException
import org.jetbrains.compose.resources.stringResource

// The codec badge switches, shown in the Settings tab and in the Now Playing sheet. Both read their
// defaults from codecBadgePrefs and write through one CodecBadgeWriter, so they can't drift apart.
internal data class CodecBadgePrefs(val showBadge: Boolean, val showSampleRate: Boolean)

/** The only place the codec badge defaults live: badge on, sample rate off. */
internal fun codecBadgePrefs(preferences: Preferences): CodecBadgePrefs = CodecBadgePrefs(
    showBadge = preferences[PlaybackPreferenceKeys.showFormatBadge] ?: true,
    showSampleRate = preferences[PlaybackPreferenceKeys.showSampleRate] ?: false,
)

/**
 * Saves the codec switches. The app shell builds it with its own [scope], not the Settings tab's,
 * so a save in flight finishes even when the tab leaves composition.
 */
internal class CodecBadgeWriter(
    private val settings: PlaybackPreferences,
    private val scope: CoroutineScope,
) {
    var writeFailed by mutableStateOf(false)
        private set

    fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        scope.launch {
            writeFailed = try {
                settings.set(key, value)
                false
            } catch (_: IOException) {
                true
            }
        }
    }
}

/** The two switch rows and the write-error line. The caller supplies the section and card around them. */
@Composable
internal fun CodecBadgeRows(prefs: CodecBadgePrefs, writer: CodecBadgeWriter) {
    PlaybackSettingRow(stringResource(Res.string.show_format_badge), prefs.showBadge) {
        writer.set(PlaybackPreferenceKeys.showFormatBadge, it)
    }
    PlaybackSettingRow(
        stringResource(Res.string.show_format_sample_rate),
        prefs.showSampleRate,
        enabled = prefs.showBadge,
    ) {
        writer.set(PlaybackPreferenceKeys.showSampleRate, it)
    }
    if (writer.writeFailed) {
        Text(stringResource(Res.string.settings_write_failed), color = MaterialTheme.colorScheme.error)
    }
}
