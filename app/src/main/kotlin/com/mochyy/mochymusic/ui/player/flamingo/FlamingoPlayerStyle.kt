package com.mochyy.mochymusic.ui.player.flamingo

// The Flamingo album page, adapted from Natune's port of Flamingo's NowPlaying (NatuneGroup/Natune,
// GPL-3.0; original design: Flamingo by shouryadixitisverycool, GPL-3.0). The floating-light
// backdrop, the big artwork on its stage, the 19.5sp title row, the flat seek line and the bare
// transport glyphs are Flamingo's; its lyric and playing-list pages become the player's own lyrics
// page and queue, opened from this style's header and bottom row.

import com.mochyy.mochymusic.ui.player.styles.rememberStyleScrub
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.transformations
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.extensions.toggleRepeatMode
import com.mochyy.mochymusic.ui.component.SeekBarStyle
import com.mochyy.mochymusic.ui.component.SeekBarTrack
import com.mochyy.mochymusic.ui.player.BackdropBlurTransformation
import com.mochyy.mochymusic.ui.player.models.TransportState
import com.mochyy.mochymusic.ui.player.styles.LocalStyleInteractive
import com.mochyy.mochymusic.ui.player.styles.StyleActions
import com.mochyy.mochymusic.ui.player.styles.StyleFrame
import com.mochyy.mochymusic.ui.player.styles.StyleMenu
import com.mochyy.mochymusic.ui.player.styles.StyleTrack
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTones
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTrack
import com.mochyy.mochymusic.ui.player.styles.styleInsets
import com.mochyy.mochymusic.ui.utils.resize
import com.mochyy.mochymusic.utils.makeTimeString

@Composable
internal fun FlamingoPlayerStyle(
    actions: StyleActions,
    modifier: Modifier,
    preview: Boolean,
) {
    val connection = LocalPlayerConnection.current ?: return
    val track = rememberStyleTrack()
    val trackInfo by connection.uiState.trackInfo.collectAsState()
    val transport by connection.uiState.transportState.collectAsState()
    val tones = rememberStyleTones()
    val interactive = LocalStyleInteractive.current
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }

    StyleFrame(preview, modifier, tones, background = {
        FlamingoBackdrop(track.artwork)
    }) { landscape, menuOpen, setMenuOpen ->
        val insets = styleInsets(landscape)
        val header: @Composable () -> Unit = {
            FlamingoHeader(actions, preview, onMore = { setMenuOpen(true) })
        }
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(
                            start = insets.start + 22.dp,
                            top = insets.top + 16.dp,
                            end = 10.dp,
                            bottom = insets.bottom + 24.dp,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        FlamingoArtwork(
                            track = track,
                            playing = transport.isPlaying,
                            modifier = Modifier.squareWithin(0.86f),
                        )
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(
                            start = 10.dp,
                            top = insets.top + 4.dp,
                            end = insets.end + 22.dp,
                            bottom = insets.bottom + 18.dp,
                        ),
                    verticalArrangement = Arrangement.Center,
                ) {
                    header()
                    Spacer(Modifier.height(18.dp))
                    FlamingoTitle(track, trackInfo.liked, connection, preview)
                    Spacer(Modifier.height(18.dp))
                    FlamingoSeek(connection, transport.isPlaying, preview)
                    Spacer(Modifier.height(12.dp))
                    FlamingoTransport(connection, transport, preview)
                    Spacer(Modifier.height(18.dp))
                    FlamingoBottomRow(actions, transport, connection, preview)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = insets.start + 22.dp,
                        top = insets.top + 4.dp,
                        end = insets.end + 22.dp,
                        bottom = insets.bottom + 10.dp,
                    ),
            ) {
                header()
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        // the artwork stage takes what the header and the controls leave, never
                        // more than a square, so a short window still shows the transport
                        FlamingoArtwork(
                            track = track,
                            playing = transport.isPlaying,
                            modifier = Modifier.squareWithin(0.94f),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                FlamingoTitle(track, trackInfo.liked, connection, preview)
                Spacer(Modifier.height(16.dp))
                FlamingoSeek(connection, transport.isPlaying, preview)
                Spacer(Modifier.height(10.dp))
                FlamingoTransport(connection, transport, preview)
                Spacer(Modifier.height(14.dp))
                FlamingoBottomRow(actions, transport, connection, preview)
            }
        }
        StyleMenu(
            open = menuOpen,
            onOpenChange = setMenuOpen,
            tones = tones,
            actions = actions,
            atBottom = true,
            end = insets.end + 22.dp,
            edge = insets.bottom + 12.dp,
            showButton = false,
        )
    }
}

/** The floating light: the cover downscaled, blurred and held under two dark washes. */
@Composable
private fun FlamingoBackdrop(artwork: String?) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (artwork != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    // decoded small before it is blurred: a full-size cover blurred at this
                    // radius is the same picture for many times the work
                    .data(artwork.resize(160, 160))
                    .allowHardware(false)
                    .transformations(BackdropBlurTransformation(radiusPx = 16))
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // an oversized footprint, so the blur never shows the stage's edge
                        scaleX = 1.3f
                        scaleY = 1.3f
                    },
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.42f),
                        0.45f to Color.Black.copy(alpha = 0.16f),
                        1f to Color.Black.copy(alpha = 0.62f),
                    ),
                ),
        )
    }
}

@Composable
private fun FlamingoHeader(actions: StyleActions, preview: Boolean, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        FlamingoIcon(R.drawable.expand_more, R.string.close, Color.White.copy(alpha = 0.9f), 44.dp, !preview, actions.onClose)
        Spacer(Modifier.weight(1f))
        actions.onLyrics?.let { lyrics ->
            FlamingoIcon(R.drawable.lyrics, R.string.lyrics, Color.White.copy(alpha = 0.72f), 44.dp, !preview, lyrics)
        }
        actions.onQueue?.let { queue ->
            FlamingoIcon(R.drawable.queue_music, R.string.queue, Color.White.copy(alpha = 0.72f), 44.dp, !preview, queue)
        }
        FlamingoIcon(R.drawable.more_horiz, R.string.more_options, Color.White.copy(alpha = 0.72f), 44.dp, !preview, onMore)
    }
}

@Composable
private fun FlamingoArtwork(track: StyleTrack, playing: Boolean, modifier: Modifier) {
    // Flamingo's sleeve pulls back a little while the song is paused
    val scale by animateFloatAsState(if (playing) 1f else 0.96f, tween(400), label = "flamingoArtScale")
    AsyncImage(
        model = track.artwork,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.06f)),
    )
}

@Composable
private fun FlamingoTitle(
    track: StyleTrack,
    liked: Boolean,
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    preview: Boolean,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 19.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        FlamingoIcon(
            icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
            description = if (liked) R.string.action_remove_like else R.string.action_like,
            tint = if (liked) Color.White else Color.White.copy(alpha = 0.6f),
            size = 44.dp,
            enabled = !preview,
            onClick = connection::toggleLike,
        )
    }
}

@Composable
private fun FlamingoSeek(
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    playing: Boolean,
    preview: Boolean,
) {
    val progress by connection.progressState.collectAsState()
    val scrub = rememberStyleScrub(connection)
    val duration = progress.duration.coerceAtLeast(0L)
    val shown = scrub.fraction ?: progress.percentage.coerceIn(0f, 1f)
    val interactive = LocalStyleInteractive.current
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = shown,
            onValueChange = { if (interactive() && !preview) scrub.update(it) },
            onValueChangeFinished = {
                scrub.finish(duration, commit = interactive())
            },
            enabled = !preview && duration > 0,
            thumb = { Spacer(Modifier.size(0.dp)) },
            track = {
                SeekBarTrack(
                    style = SeekBarStyle.PLAIN,
                    fraction = shown,
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.28f),
                    animateWave = playing,
                )
            },
            modifier = Modifier.fillMaxWidth().height(28.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val position = scrub.fraction?.let { (duration * it).toLong() } ?: progress.position
            Text(makeTimeString(position), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            Text("-" + makeTimeString((duration - position).coerceAtLeast(0L)), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun FlamingoTransport(
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    transport: TransportState,
    preview: Boolean,
) {
    val interactive = LocalStyleInteractive.current
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FlamingoIcon(
            icon = R.drawable.skip_previous,
            description = R.string.previous,
            tint = Color.White.copy(alpha = if (transport.canSkipPrevious) 0.92f else 0.35f),
            size = 61.dp,
            iconSize = 40.dp,
            enabled = !preview && transport.canSkipPrevious,
            onClick = tap(connection::seekToPrevious),
        )
        Spacer(Modifier.width(43.dp))
        Box(
            Modifier
                .size(58.5.dp)
                .clip(RoundedCornerShape(50))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !preview,
                    onClick = tap {
                        if (transport.playbackState == Player.STATE_ENDED) {
                            connection.player.seekTo(0, 0)
                            connection.player.playWhenReady = true
                        } else {
                            connection.togglePlayPause()
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (transport.isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = stringResource(if (transport.isPlaying) R.string.pause else R.string.play),
                tint = Color.White,
                modifier = Modifier.size(38.dp),
            )
        }
        Spacer(Modifier.width(43.dp))
        FlamingoIcon(
            icon = R.drawable.skip_next,
            description = R.string.next,
            tint = Color.White.copy(alpha = if (transport.canSkipNext) 0.92f else 0.35f),
            size = 61.dp,
            iconSize = 40.dp,
            enabled = !preview && transport.canSkipNext,
            onClick = tap(connection::seekToNext),
        )
    }
}

/** Flamingo's bottom row: lyrics, the airplay substitute (the queue), and the playing list. */
@Composable
private fun FlamingoBottomRow(
    actions: StyleActions,
    transport: TransportState,
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    preview: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().height(36.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.onLyrics?.let { lyrics ->
            FlamingoIcon(R.drawable.lyrics, R.string.lyrics, Color.White.copy(alpha = 0.5f), 40.dp, !preview, lyrics)
        }
        FlamingoIcon(
            icon = R.drawable.shuffle,
            description = R.string.shuffle,
            tint = Color.White.copy(alpha = if (transport.shuffleModeEnabled) 0.9f else 0.45f),
            size = 40.dp,
            enabled = !preview,
            onClick = { connection.player.shuffleModeEnabled = !transport.shuffleModeEnabled },
        )
        FlamingoIcon(
            icon = if (transport.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
            description = if (transport.repeatMode == Player.REPEAT_MODE_ONE) R.string.ps_style_repeat_one else R.string.repeat,
            tint = Color.White.copy(alpha = if (transport.repeatMode != Player.REPEAT_MODE_OFF) 0.9f else 0.45f),
            size = 40.dp,
            enabled = !preview,
            onClick = { connection.player.toggleRepeatMode() },
        )
        actions.onQueue?.let { queue ->
            FlamingoIcon(R.drawable.queue_music, R.string.queue, Color.White.copy(alpha = 0.5f), 40.dp, !preview, queue)
        }
    }
}

@Composable
private fun FlamingoIcon(
    icon: Int,
    description: Int,
    tint: Color,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = size * 0.56f,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(description),
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * A square artwork [fraction] of the stage's width, shrunk to the stage's height when the window
 * is short. One measurement, so the square never breaks the width constraint it was given.
 */
private fun Modifier.squareWithin(fraction: Float): Modifier = this.layout { measurable, constraints ->
    val side = minOf((constraints.maxWidth * fraction).toInt(), constraints.maxHeight)
    val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(side, side))
    layout(side, side) { placeable.place(0, 0) }
}
