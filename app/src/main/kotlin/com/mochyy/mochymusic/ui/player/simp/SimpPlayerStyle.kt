package com.mochyy.mochymusic.ui.player.simp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.player.styles.LivePosition
import com.mochyy.mochymusic.ui.player.styles.LocalStyleInteractive
import com.mochyy.mochymusic.ui.player.styles.StyleActions
import com.mochyy.mochymusic.ui.player.styles.StyleCircle
import com.mochyy.mochymusic.ui.player.styles.StyleFrame
import com.mochyy.mochymusic.ui.player.styles.StyleMenu
import com.mochyy.mochymusic.ui.player.styles.StyleTones
import com.mochyy.mochymusic.ui.player.styles.StyleTrack
import com.mochyy.mochymusic.ui.player.styles.rememberDurationMs
import com.mochyy.mochymusic.ui.player.styles.rememberLivePosition
import com.mochyy.mochymusic.ui.player.styles.rememberPlayedSeconds
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTones
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTrack
import com.mochyy.mochymusic.ui.player.styles.styleInsets
import com.mochyy.mochymusic.ui.player.styles.whileStyleInteractive
import com.mochyy.mochymusic.utils.makeTimeString

private val TrackHeight = 4.dp
private val KnobSize = 12.dp

@Composable
internal fun SimpPlayerStyle(
    actions: StyleActions,
    modifier: Modifier,
    preview: Boolean,
) {
    val connection = LocalPlayerConnection.current ?: return
    val track = rememberStyleTrack()
    val trackInfo by connection.uiState.trackInfo.collectAsState()
    val transport by connection.uiState.transportState.collectAsState()
    val tones = rememberStyleTones()
    // the position moves with the frame only while the song plays; paused, it stays put
    val position = rememberLivePosition(animated = !preview)
    val duration = rememberDurationMs()
    val accent = MaterialTheme.colorScheme.primary
    val interactive = LocalStyleInteractive.current

    // a tap on a control while the player is collapsed does nothing, the same rule as the other styles
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }

    StyleFrame(preview, modifier, tones, background = {
        Box(Modifier.fillMaxSize().background(tones.remaining))
    }) { landscape, menuOpen, setMenuOpen ->
        val insets = styleInsets(landscape)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = insets.start + 24.dp,
                    top = insets.top + 8.dp,
                    end = insets.end + 24.dp,
                    bottom = insets.bottom + 8.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleCircle(R.drawable.close, tones, actions.onClose, size = 48.dp, enabled = !preview)
                StyleCircle(R.drawable.more_vert, tones, { setMenuOpen(true) }, size = 48.dp, enabled = !preview)
            }

            Spacer(Modifier.height(12.dp))

            BoxWithConstraints(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                // a square that fits the stage it's given, so a short window keeps the controls on screen
                val artSize = minOf(maxWidth, maxHeight)
                SimpArtwork(
                    track = track,
                    onOpenLyrics = actions.onLyrics?.let { tap(it) },
                    modifier = Modifier.size(artSize),
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = tones.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = tones.inkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                val liked = trackInfo.liked
                StyleCircle(
                    icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                    tones = tones,
                    onClick = tap(connection::toggleLike),
                    size = 48.dp,
                    enabled = !preview,
                    active = liked,
                )
            }

            Spacer(Modifier.height(20.dp))

            SimpSeekBar(
                position = position,
                duration = duration,
                preview = preview,
                accent = accent,
                tones = tones,
                onSeek = { if (interactive()) connection.seekTo(it) },
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleCircle(
                    icon = R.drawable.skip_previous,
                    tones = tones,
                    onClick = tap(connection::seekToPrevious),
                    size = 64.dp,
                    enabled = !preview && transport.canSkipPrevious,
                )
                StyleCircle(
                    icon = if (transport.isPlaying) R.drawable.pause else R.drawable.play,
                    tones = tones,
                    onClick = tap(connection::togglePlayPause),
                    size = 88.dp,
                    enabled = !preview,
                    active = true,
                )
                StyleCircle(
                    icon = R.drawable.skip_next,
                    tones = tones,
                    onClick = tap(connection::seekToNext),
                    size = 64.dp,
                    enabled = !preview && transport.canSkipNext,
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleCircle(
                    icon = R.drawable.lyrics,
                    tones = tones,
                    onClick = tap { actions.onLyrics?.invoke() },
                    size = 48.dp,
                    enabled = !preview && actions.onLyrics != null,
                )
                StyleCircle(
                    icon = R.drawable.queue_music,
                    tones = tones,
                    onClick = tap { actions.onQueue?.invoke() },
                    size = 48.dp,
                    enabled = !preview && actions.onQueue != null,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
        StyleMenu(
            open = menuOpen,
            onOpenChange = setMenuOpen,
            tones = tones,
            actions = actions,
            atBottom = true,
            end = insets.end + 24.dp,
            edge = insets.bottom + 12.dp,
            showButton = false,
        )
    }
}

@Composable
private fun SimpArtwork(track: StyleTrack, onOpenLyrics: (() -> Unit)?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = track.artwork,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .then(
                if (onOpenLyrics != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenLyrics,
                ) else Modifier
            ),
    )
}

/**
 * A slim bar with a round knob, and the elapsed and remaining times under it. [position] is read
 * in draw, so the knob moves without recomposing; a drag shows its own place and seeks once, when
 * the finger lifts.
 */
@Composable
private fun SimpSeekBar(
    position: LivePosition,
    duration: Long,
    preview: Boolean,
    accent: Color,
    tones: StyleTones,
    onSeek: (Long) -> Unit,
) {
    val interactive = LocalStyleInteractive.current
    val safeDuration = if (duration > 0L) duration else 1L
    var drag by remember { mutableStateOf<Float?>(null) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    val played = rememberPlayedSeconds(position)
    val shownMs = drag?.let { (it * safeDuration).toLong() } ?: (played * 1_000L).coerceIn(0L, safeDuration)

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(KnobSize * 2)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(safeDuration, preview, interactive) {
                    if (preview) return@pointerInput
                    whileStyleInteractive(interactive) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            fun fractionAt(x: Float): Float =
                                if (trackWidthPx <= 0f) 0f else (x / trackWidthPx).coerceIn(0f, 1f)
                            var lastX = down.position.x
                            drag = fractionAt(lastX)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                lastX = change.position.x
                                if (!change.pressed) break
                                change.consume()
                                drag = fractionAt(lastX)
                            }
                            onSeek((fractionAt(lastX) * safeDuration).toLong())
                            drag = null
                        }
                    }
                },
        ) {
            val fraction = drag ?: (position.positionMs().toFloat() / safeDuration).coerceIn(0f, 1f)
            val centerY = size.height / 2f
            val trackH = TrackHeight.toPx()
            val knobR = KnobSize.toPx() / 2f
            val thumbX = fraction * size.width
            val corner = CornerRadius(trackH / 2f, trackH / 2f)

            drawRoundRect(
                color = tones.ink.copy(alpha = 0.18f),
                topLeft = Offset(0f, centerY - trackH / 2f),
                size = Size(size.width, trackH),
                cornerRadius = corner,
            )
            if (thumbX > 0f) {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(0f, centerY - trackH / 2f),
                    size = Size(thumbX, trackH),
                    cornerRadius = corner,
                )
            }
            drawCircle(color = tones.ink, radius = knobR, center = Offset(thumbX.coerceIn(knobR, size.width - knobR), centerY))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(makeTimeString(shownMs), style = MaterialTheme.typography.labelMedium, color = tones.inkSoft)
            Text("-" + makeTimeString(safeDuration - shownMs), style = MaterialTheme.typography.labelMedium, color = tones.inkSoft)
        }
    }
}
