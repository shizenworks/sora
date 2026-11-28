// Modern and Vintage layouts adapted from Natune's Metrolist player (NatuneGroup/Natune,
// GPL-3.0), based on Metrolist by MetrolistGroup (GPL-3.0).
package com.mochyy.mochymusic.ui.player.styles

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.extensions.toggleRepeatMode
import com.mochyy.mochymusic.utils.makeTimeString

@Composable
internal fun MetrolistPlayerStyle(
    actions: StyleActions,
    modifier: Modifier,
    preview: Boolean,
    vintage: Boolean,
) {
    val connection = LocalPlayerConnection.current ?: return
    val track = rememberStyleTrack()
    val trackInfo by connection.uiState.trackInfo.collectAsState()
    val transport by connection.uiState.transportState.collectAsState()
    val tones = rememberStyleTones()
    StyleFrame(preview, modifier, tones, background = {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().shadedTone(tones.remaining))
    }) { landscape, menuOpen, setMenuOpen ->
        val insets = styleInsets(landscape)
        BoxWithConstraints(Modifier.fillMaxSize().padding(
            start = insets.start + 24.dp,
            top = insets.top,
            end = insets.end + 24.dp,
            bottom = insets.bottom,
        )) {
            val artworkSize = if (landscape) {
                minOf(maxWidth * 0.42f, (maxHeight - 112.dp).coerceAtLeast(96.dp))
            } else {
                minOf(maxWidth, (maxHeight * 0.52f).coerceAtLeast(144.dp), 440.dp)
            }
            val artwork: @Composable () -> Unit = {
                val shape = RoundedCornerShape(if (vintage) 0.dp else 12.dp)
                AsyncImage(
                    model = track.artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(artworkSize).aspectRatio(1f).clip(shape)
                        .background(tones.chip)
                        .then(if (vintage) Modifier.border(8.dp, tones.inkSoft, shape).padding(18.dp) else Modifier),
                )
            }
            val controls: @Composable (Modifier) -> Unit = { controlsModifier ->
                Column(
                    modifier = controlsModifier,
                    horizontalAlignment = if (vintage) Alignment.CenterHorizontally else Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        track.title,
                        color = tones.ink,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = if (vintage) TextAlign.Center else TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(track.artist, color = tones.inkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MetrolistSeek(tones, track, preview)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MetrolistButton(R.drawable.skip_previous, R.string.previous, tones.ink, connection::seekToPrevious,
                            enabled = !preview && transport.canSkipPrevious)
                        MetrolistButton(
                            if (transport.isPlaying) R.drawable.pause else R.drawable.play,
                            if (transport.isPlaying) R.string.pause else R.string.play,
                            tones.remaining,
                            connection::togglePlayPause,
                            modifier = Modifier.size(64.dp)
                                .clip(RoundedCornerShape(if (vintage) 32.dp else 20.dp))
                                .background(tones.ink),
                            enabled = !preview,
                        )
                        MetrolistButton(R.drawable.skip_next, R.string.next, tones.ink, connection::seekToNext,
                            enabled = !preview && transport.canSkipNext)
                    }
                }
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    MetrolistButton(R.drawable.expand_more, R.string.metrolist_collapse, tones.ink, actions.onClose, enabled = !preview)
                    Text(
                        stringResource(if (vintage) R.string.metrolist_vintage else R.string.metrolist_modern),
                        style = MaterialTheme.typography.labelLarge,
                        color = tones.inkSoft,
                    )
                    MetrolistButton(
                        if (trackInfo.liked) R.drawable.favorite else R.drawable.favorite_border,
                        if (trackInfo.liked) R.string.action_remove_like else R.string.action_like,
                        tones.ink, connection::toggleLike, enabled = !preview,
                    )
                }
                if (landscape) {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(24.dp), Alignment.CenterVertically) {
                        artwork()
                        controls(Modifier.weight(1f))
                    }
                } else {
                    artwork()
                    controls(Modifier.widthIn(max = 440.dp).fillMaxWidth())
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                    MetrolistButton(R.drawable.lyrics, R.string.lyrics, tones.ink, actions.onLyrics, enabled = !preview)
                    MetrolistButton(R.drawable.shuffle, R.string.shuffle,
                        if (transport.shuffleModeEnabled) tones.ink else tones.inkSoft,
                        { connection.player.shuffleModeEnabled = !transport.shuffleModeEnabled }, enabled = !preview)
                    MetrolistButton(R.drawable.queue_music, R.string.queue, tones.ink, actions.onQueue, enabled = !preview)
                    MetrolistButton(
                        if (transport.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
                        if (transport.repeatMode == Player.REPEAT_MODE_ONE) R.string.ps_style_repeat_one else R.string.repeat,
                        if (transport.repeatMode == Player.REPEAT_MODE_OFF) tones.inkSoft else tones.ink,
                        { connection.player.toggleRepeatMode() }, enabled = !preview,
                    )
                    MetrolistButton(R.drawable.sleep_timer, R.string.sleep_timer, tones.ink, actions.onSleepTimer, enabled = !preview)
                    MetrolistButton(R.drawable.more_horiz, R.string.more_options, tones.ink, { setMenuOpen(true) }, enabled = !preview)
                }
            }
        }
        StyleMenu(
            open = menuOpen, onOpenChange = setMenuOpen, tones = tones, actions = actions,
            atBottom = true, end = insets.end + 24.dp, edge = insets.bottom + 12.dp,
            showButton = false,
        )
    }
}

@Composable
private fun MetrolistSeek(tones: StyleTones, track: StyleTrack, preview: Boolean) {
    val connection = LocalPlayerConnection.current ?: return
    val progress by connection.progressState.collectAsState()
    val scrub = rememberStyleScrub(connection)
    val duration = progress.duration.coerceAtLeast(0L)
    val interactive = LocalStyleInteractive.current
    Slider(
        value = scrub.fraction ?: progress.percentage.coerceIn(0f, 1f),
        onValueChange = { if (interactive()) scrub.update(it) },
        onValueChangeFinished = {
            scrub.finish(duration, commit = interactive())
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !preview && duration > 0,
        colors = SliderDefaults.colors(thumbColor = tones.ink, activeTrackColor = tones.ink, inactiveTrackColor = tones.chip),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val position = scrub.fraction?.let { (duration * it).toLong() } ?: progress.position
        Text(makeTimeString(position), color = tones.inkSoft, style = MaterialTheme.typography.labelMedium)
        Text(makeTimeString(duration), color = tones.inkSoft, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MetrolistButton(
    @DrawableRes icon: Int,
    @StringRes description: Int,
    color: Color,
    action: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactive = LocalStyleInteractive.current
    IconButton(
        onClick = { if (interactive()) action?.invoke() },
        modifier = modifier,
        enabled = enabled && action != null,
    ) {
        Icon(painterResource(icon), stringResource(description), tint = if (enabled) color else color.copy(alpha = 0.45f))
    }
}
