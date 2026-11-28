package com.mochyy.mochymusic.ui.player.bitchord

// The BitChord player, adapted from Natune's port of BitChord's NowPlayingScreen
// (NatuneGroup/Natune, GPL-3.0; original design: BitChord by kushagrasinghx, GPL-3.0).
// The full-bleed artwork hero dissolving into the cover's mesh, the credits row with like and
// more, the thin scrub line, the oversized play glyph and the lyrics / shuffle-repeat pill /
// queue row are BitChord's. Two pieces of its original are Natune-only and are left out rather
// than imitated: the device volume slider (Mochy's styles have no volume controller) and the
// inline lyric and queue panels (this style opens the player's own pages instead).

import com.mochyy.mochymusic.ui.player.styles.rememberStyleScrub
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.LocalArtworkColor
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.extensions.toggleRepeatMode
import com.mochyy.mochymusic.ui.component.SeekBarStyle
import com.mochyy.mochymusic.ui.component.SeekBarTrack
import com.mochyy.mochymusic.ui.player.models.TransportState
import com.mochyy.mochymusic.ui.player.styles.LocalStyleInteractive
import com.mochyy.mochymusic.ui.player.styles.StyleActions
import com.mochyy.mochymusic.ui.player.styles.StyleFrame
import com.mochyy.mochymusic.ui.player.styles.StyleMenu
import com.mochyy.mochymusic.ui.player.styles.StyleTrack
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTones
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTrack
import com.mochyy.mochymusic.ui.player.styles.styleInsets
import com.mochyy.mochymusic.utils.makeTimeString

@Composable
internal fun BitChordPlayerStyle(
    actions: StyleActions,
    modifier: Modifier,
    preview: Boolean,
) {
    val connection = LocalPlayerConnection.current ?: return
    val track = rememberStyleTrack()
    val trackInfo by connection.uiState.trackInfo.collectAsState()
    val transport by connection.uiState.transportState.collectAsState()
    val tones = rememberStyleTones()
    val dominant = LocalArtworkColor.current
    val backdrop = lerp(dominant, Color.Black, 0.8f)
    val interactive = LocalStyleInteractive.current
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }

    StyleFrame(preview, modifier, tones, background = {
        // the mesh: the sleeve's hue lit from the top-left and sinking to near-black, the colour
        // the hero fades into
        Box(
            Modifier
                .fillMaxSize()
                .background(backdrop)
                .background(
                    Brush.radialGradient(
                        colors = listOf(dominant.copy(alpha = 0.30f), Color.Transparent),
                        center = Offset(0.28f, 0.10f),
                        radius = 1400f,
                    ),
                ),
        )
    }) { landscape, menuOpen, setMenuOpen ->
        val insets = styleInsets(landscape)
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    // no side padding here: the hero is full-bleed, and the rows under it keep
                    // their own inset
                    .padding(
                        start = insets.start,
                        top = insets.top + 4.dp,
                        end = insets.end,
                        bottom = insets.bottom + 10.dp,
                    ),
            ) {
                BitChordHeader(actions, preview, onMore = { setMenuOpen(true) })
                // the hero owns what the credits, scrub line and transport leave, so the sleeve
                // is as big as the window allows rather than a fixed share of it
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BitChordHero(track, transport.isPlaying, backdrop, Modifier.fillMaxSize())
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(14.dp))
                    BitChordCredits(track, trackInfo.liked, connection, preview)
                    Spacer(Modifier.height(14.dp))
                    BitChordSeek(connection, transport.isPlaying, preview)
                    Spacer(Modifier.height(8.dp))
                    BitChordTransport(connection, transport, preview)
                    Spacer(Modifier.height(12.dp))
                    BitChordBottomRow(actions, transport, connection, preview)
                }
            }
        }
        StyleMenu(
            open = menuOpen,
            onOpenChange = setMenuOpen,
            tones = tones,
            actions = actions,
            atBottom = true,
            end = insets.end + 20.dp,
            edge = insets.bottom + 12.dp,
            showButton = false,
        )
    }
}

@Composable
private fun BitChordHeader(actions: StyleActions, preview: Boolean, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        BitChordIcon(R.drawable.expand_more, R.string.close, Color.White.copy(alpha = 0.85f), 40.dp, !preview, actions.onClose)
        Spacer(Modifier.weight(1f))
        BitChordIcon(R.drawable.more_vert, R.string.more_options, Color.White.copy(alpha = 0.75f), 40.dp, !preview, onMore)
    }
}

/** The sleeve as a full-bleed banner, fading into the mesh at its foot. */
@Composable
private fun BitChordHero(track: StyleTrack, playing: Boolean, backdrop: Color, modifier: Modifier) {
    // BitChord's Apple Music touch: the sleeve shrinks back while the song is paused
    val scale by animateFloatAsState(
        targetValue = if (playing) 1f else 0.86f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 250f),
        label = "bitchordArtScale",
    )
    Box(modifier) {
        AsyncImage(
            model = track.artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // a small pull-back while paused reads on the artwork, not on the background
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
        // the artwork's own edge is what dissolves it into the player's colour
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.68f to Color.Transparent,
                        1f to backdrop,
                    ),
                ),
        )
    }
}

@Composable
private fun BitChordCredits(
    track: StyleTrack,
    liked: Boolean,
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    preview: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        BitChordIcon(
            icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
            description = if (liked) R.string.action_remove_like else R.string.action_like,
            tint = if (liked) Color.White else Color.White.copy(alpha = 0.55f),
            size = 44.dp,
            enabled = !preview,
            onClick = connection::toggleLike,
        )
    }
}

@Composable
private fun BitChordSeek(
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
                    inactiveColor = Color.White.copy(alpha = 0.22f),
                    animateWave = playing,
                )
            },
            modifier = Modifier.fillMaxWidth().height(24.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val position = scrub.fraction?.let { (duration * it).toLong() } ?: progress.position
            Text(makeTimeString(position), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
            Text("-" + makeTimeString((duration - position).coerceAtLeast(0L)), color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun BitChordTransport(
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    transport: TransportState,
    preview: Boolean,
) {
    val interactive = LocalStyleInteractive.current
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BitChordIcon(
            icon = R.drawable.skip_previous,
            description = R.string.previous,
            tint = Color.White.copy(alpha = if (transport.canSkipPrevious) 0.9f else 0.35f),
            size = 48.dp,
            iconSize = 40.dp,
            enabled = !preview && transport.canSkipPrevious,
            onClick = tap(connection::seekToPrevious),
        )
        // BitChord keeps the play glyph's 72dp footprint through a buffer, so nothing below it moves
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            if (transport.playbackState == Player.STATE_BUFFERING) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(38.dp))
            } else {
                BitChordIcon(
                    icon = if (transport.isPlaying) R.drawable.pause else R.drawable.play,
                    description = if (transport.isPlaying) R.string.pause else R.string.play,
                    tint = Color.White,
                    size = 72.dp,
                    iconSize = 48.dp,
                    enabled = !preview,
                    onClick = tap {
                        if (transport.playbackState == Player.STATE_ENDED) {
                            connection.player.seekTo(0, 0)
                            connection.player.playWhenReady = true
                        } else {
                            connection.togglePlayPause()
                        }
                    },
                )
            }
        }
        BitChordIcon(
            icon = R.drawable.skip_next,
            description = R.string.next,
            tint = Color.White.copy(alpha = if (transport.canSkipNext) 0.9f else 0.35f),
            size = 48.dp,
            iconSize = 40.dp,
            enabled = !preview && transport.canSkipNext,
            onClick = tap(connection::seekToNext),
        )
    }
}

/** The lyrics glyph, the shuffle/repeat capsule and the queue glyph, as one row. */
@Composable
private fun BitChordBottomRow(
    actions: StyleActions,
    transport: TransportState,
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    preview: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.onLyrics?.let { lyrics ->
            BitChordIcon(R.drawable.lyrics, R.string.lyrics, Color.White.copy(alpha = 0.85f), 48.dp, !preview, lyrics)
        }
        Row(
            Modifier
                .height(42.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.10f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BitChordIcon(
                icon = R.drawable.shuffle,
                description = R.string.shuffle,
                tint = Color.White.copy(alpha = if (transport.shuffleModeEnabled) 1f else 0.5f),
                size = 52.dp,
                iconSize = 20.dp,
                enabled = !preview,
                onClick = { connection.player.shuffleModeEnabled = !transport.shuffleModeEnabled },
            )
            Box(Modifier.width(1.dp).height(18.dp).background(Color.White.copy(alpha = 0.18f)))
            BitChordIcon(
                icon = if (transport.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
                description = if (transport.repeatMode == Player.REPEAT_MODE_ONE) R.string.ps_style_repeat_one else R.string.repeat,
                tint = Color.White.copy(alpha = if (transport.repeatMode != Player.REPEAT_MODE_OFF) 1f else 0.5f),
                size = 52.dp,
                iconSize = 20.dp,
                enabled = !preview,
                onClick = { connection.player.toggleRepeatMode() },
            )
        }
        actions.onQueue?.let { queue ->
            BitChordIcon(R.drawable.queue_music, R.string.queue, Color.White.copy(alpha = 0.85f), 48.dp, !preview, queue)
        }
    }
}

@Composable
private fun BitChordIcon(
    icon: Int,
    description: Int,
    tint: Color,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = size * 0.55f,
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
