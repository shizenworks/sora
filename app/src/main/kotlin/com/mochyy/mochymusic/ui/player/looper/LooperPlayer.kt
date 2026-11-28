// The Looper player style: a flat near-black stage, the square artwork with a close button and a
// more button above it, the title and like beside it, a wave seek bar, a tall pair of transport
// pills around a play button, and shuffle, repeat, queue and lyrics as pills at the bottom. Adapted
// from Natune's Looper player (NatuneGroup/Natune, GPL-3.0), based on ArchiveTune's Looper by
// Rukamori (GPL-3.0). Its own Jost type is not ported yet, so it reads in the app's Inter, and its
// round buttons are the shared StyleCircle.

package com.mochyy.mochymusic.ui.player.looper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.extensions.toggleRepeatMode
import com.mochyy.mochymusic.ui.player.styles.LocalStyleInteractive
import com.mochyy.mochymusic.ui.player.styles.StyleActions
import com.mochyy.mochymusic.ui.player.styles.StyleCircle
import com.mochyy.mochymusic.ui.player.styles.StyleFrame
import com.mochyy.mochymusic.ui.player.styles.StyleMenu
import com.mochyy.mochymusic.ui.player.styles.StyleTrack
import com.mochyy.mochymusic.ui.player.styles.rememberLivePosition
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTones
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTrack
import com.mochyy.mochymusic.ui.player.styles.rememberDurationMs
import com.mochyy.mochymusic.ui.player.styles.styleInsets

private val LooperStage = Color(0xFF141414)
private val LooperPillFill = Color.White.copy(alpha = 0.04f)

@Composable
internal fun LooperPlayerStyle(
    actions: StyleActions,
    modifier: Modifier,
    preview: Boolean,
) {
    val connection = LocalPlayerConnection.current ?: return
    val track = rememberStyleTrack()
    val trackInfo by connection.uiState.trackInfo.collectAsState()
    val transport by connection.uiState.transportState.collectAsState()
    val tones = rememberStyleTones()
    val position = rememberLivePosition(animated = !preview)
    val duration = rememberDurationMs()
    val accent = MaterialTheme.colorScheme.primary
    val interactive = LocalStyleInteractive.current

    // a tap that lands on a control while the player is collapsed does nothing, the same rule as the other styles
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }

    StyleFrame(preview, modifier, tones, background = {
        Box(Modifier.fillMaxSize().background(LooperStage))
    }) { landscape, menuOpen, setMenuOpen ->
        val insets = styleInsets(landscape)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = insets.start + 16.dp,
                    top = insets.top + 8.dp,
                    end = insets.end + 16.dp,
                    bottom = insets.bottom + 8.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // the close and more buttons; the style has its own chrome, so no grab handle over it
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleCircle(R.drawable.close, tones, actions.onClose, size = 48.dp, enabled = !preview)
                StyleCircle(R.drawable.more_vert, tones, { setMenuOpen(true) }, size = 48.dp, enabled = !preview)
            }

            Spacer(Modifier.height(16.dp))

            BoxWithConstraints(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                // a square that fits the stage it's given, so a short window doesn't push the controls off
                val artSize = minOf(maxWidth, maxHeight)
                LooperArtwork(
                    track = track,
                    onOpenLyrics = actions.onLyrics?.let { tap(it) },
                    modifier = Modifier.size(artSize),
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val liked = trackInfo.liked
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                StyleCircle(
                    icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                    tones = tones,
                    onClick = connection::toggleLike,
                    size = 56.dp,
                    enabled = !preview,
                    active = liked,
                )
            }

            Spacer(Modifier.height(32.dp))

            LooperSeekBar(
                position = position,
                duration = duration,
                playing = transport.isPlaying,
                preview = preview,
                accent = accent,
                onSeek = { if (interactive()) connection.seekTo(it) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LooperTransportPill(
                    innerRadius = 12.dp,
                    outerRadius = 40.dp,
                    enabled = !preview && transport.canSkipPrevious,
                    onClick = tap(connection::seekToPrevious),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = stringResource(R.string.looper_previous),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (transport.isPlaying) LooperPillFill else accent)
                        .clickable(
                            enabled = !preview,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = tap(connection::togglePlayPause),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (transport.isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = stringResource(if (transport.isPlaying) R.string.pause else R.string.play),
                        tint = if (transport.isPlaying) Color.White else Color.Black,
                        modifier = Modifier.size(28.dp),
                    )
                }
                LooperTransportPill(
                    innerRadius = 40.dp,
                    outerRadius = 12.dp,
                    enabled = !preview && transport.canSkipNext,
                    onClick = tap(connection::seekToNext),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = stringResource(R.string.looper_next),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(LooperPillFill)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LooperUtilityPill(
                        iconRes = R.drawable.shuffle,
                        label = stringResource(R.string.shuffle),
                        active = transport.shuffleModeEnabled,
                        horizontalPadding = 20.dp,
                        iconSize = 16.dp,
                        preview = preview,
                        onClick = tap { connection.player.shuffleModeEnabled = !transport.shuffleModeEnabled },
                    )
                    LooperUtilityPill(
                        iconRes = if (transport.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
                        label = stringResource(R.string.looper_repeat),
                        active = transport.repeatMode != Player.REPEAT_MODE_OFF,
                        horizontalPadding = 18.dp,
                        iconSize = 20.dp,
                        preview = preview,
                        onClick = tap { connection.player.toggleRepeatMode() },
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(LooperPillFill)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LooperLabeledPill(R.drawable.queue_music, stringResource(R.string.queue), preview, actions.onQueue?.let { tap(it) })
                    LooperLabeledPill(R.drawable.lyrics, stringResource(R.string.looper_lyrics), preview, actions.onLyrics?.let { tap(it) })
                }
            }

            Spacer(Modifier.height(32.dp))
        }
        StyleMenu(
            open = menuOpen,
            onOpenChange = setMenuOpen,
            tones = tones,
            actions = actions,
            atBottom = true,
            end = insets.end + 16.dp,
            edge = insets.bottom + 12.dp,
            showButton = false,
        )
    }
}

@Composable
private fun LooperArtwork(track: StyleTrack, onOpenLyrics: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    AsyncImage(
        model = track.artwork,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.04f), shape = shape)
            .then(if (onOpenLyrics != null) Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenLyrics,
            ) else Modifier),
    )
}

@Composable
private fun LooperUtilityPill(
    iconRes: Int,
    label: String,
    active: Boolean,
    horizontalPadding: Dp,
    iconSize: Dp,
    preview: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) Color.White.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(enabled = !preview, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = if (active) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun LooperLabeledPill(iconRes: Int, label: String, preview: Boolean, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(
                enabled = !preview && onClick != null,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick?.invoke() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
private fun LooperTransportPill(
    innerRadius: Dp,
    outerRadius: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // the pill's two ends are rounded differently, so the pair reads as one split button
    val shape = RoundedCornerShape(topStart = innerRadius, bottomStart = innerRadius, topEnd = outerRadius, bottomEnd = outerRadius)
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(shape)
            .background(LooperPillFill, shape = shape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
