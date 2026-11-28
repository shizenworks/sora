package com.mochyy.mochymusic.ui.player.spatialflow

// The SpatialFlow full player, adapted from Natune's port of SpatialFlow's FullPlayer
// (NatuneGroup/Natune, GPL-3.0; original design: SpatialFlow by MythicalSHUB, GPL-3.0). The
// "NOW PLAYING" header, the big rounded artwork, the marquee metadata row, the scrolling pill
// chips, the wavy seek bar and the M3 Expressive button-group transport are SpatialFlow's. Its
// inline lyrics overlay and queue drawer map onto the player's own pages; the haptics, share and
// download chips have no Mochy equivalent and are left out rather than faked.

import com.mochyy.mochymusic.ui.player.styles.rememberStyleScrub
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
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
import com.mochyy.mochymusic.ui.component.SeekBarStyle
import com.mochyy.mochymusic.ui.component.SeekBarTrack
import com.mochyy.mochymusic.ui.player.styles.LocalStyleInteractive
import com.mochyy.mochymusic.ui.player.styles.StyleActions
import com.mochyy.mochymusic.ui.player.styles.StyleFrame
import com.mochyy.mochymusic.ui.player.styles.StyleMenu
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTones
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTrack
import com.mochyy.mochymusic.ui.player.styles.styleInsets
import com.mochyy.mochymusic.ui.player.styles.whileStyleInteractive
import com.mochyy.mochymusic.utils.makeTimeString

@Composable
internal fun SpatialFlowPlayerStyle(
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
    // a hushed copy of the sleeve's hue for the screen behind it, and the accent the transport
    // carries: the same colour lifted enough to read on it
    val backdrop = lerp(dominant, Color.Black, 0.82f)
    val accent = lerp(dominant, Color.White, 0.20f)
    val onAccent = if (accent.luminance() > 0.5f) Color(0xFF1C1B1F) else Color.White
    val contentColor = Color(0xFFF6F3F8)
    val contentSecondary = contentColor.copy(alpha = 0.6f)
    val interactive = LocalStyleInteractive.current
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }

    StyleFrame(preview, modifier, tones, background = {
        Box(Modifier.fillMaxSize().background(backdrop))
        // the sleeve's own light, soft behind the artwork
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(dominant.copy(alpha = 0.22f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(0.5f, 0.22f),
                        radius = 1200f,
                    ),
                ),
        )
    }) { _, menuOpen, setMenuOpen ->
        val insets = styleInsets(false)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val artSize = minOf(maxWidth - 24.dp, maxHeight * 0.42f)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = insets.start + 20.dp,
                        top = insets.top + 4.dp,
                        end = insets.end + 20.dp,
                        bottom = insets.bottom + 6.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // the header: collapse, the style's own label, the player's menu
                Row(
                    Modifier.fillMaxWidth().height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpatialFlowIcon(R.drawable.expand_more, R.string.close, contentColor.copy(alpha = 0.8f), 44.dp, 28.dp, !preview, actions.onClose)
                    Text(
                        text = stringResource(R.string.ps_spatialflow_now_playing),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentSecondary,
                    )
                    SpatialFlowIcon(R.drawable.more_vert, R.string.more_options, contentColor.copy(alpha = 0.8f), 44.dp, 28.dp, !preview) { setMenuOpen(true) }
                }
                Spacer(Modifier.weight(1f))
                AsyncImage(
                    model = track.artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(artSize)
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f)),
                )
                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentSecondary,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val liked = trackInfo.liked
                    SpatialFlowChip(
                        icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                        label = stringResource(R.string.action_like),
                        selected = liked,
                        accent = accent,
                        contentColor = contentColor,
                        enabled = !preview,
                        onClick = tap(connection::toggleLike),
                    )
                    actions.onLyrics?.let { lyrics ->
                        SpatialFlowChip(
                            icon = R.drawable.lyrics,
                            label = stringResource(R.string.lyrics),
                            selected = false,
                            accent = accent,
                            contentColor = contentColor,
                            enabled = !preview,
                            onClick = tap(lyrics),
                        )
                    }
                    actions.onQueue?.let { queue ->
                        SpatialFlowChip(
                            icon = R.drawable.queue_music,
                            label = stringResource(R.string.queue),
                            selected = false,
                            accent = accent,
                            contentColor = contentColor,
                            enabled = !preview,
                            onClick = tap(queue),
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                SpatialFlowSeek(connection, accent, contentSecondary, transport.isPlaying, preview)
                Spacer(Modifier.height(16.dp))
                SpatialFlowTransport(connection, transport, accent, onAccent, contentColor, preview)
                Spacer(Modifier.weight(0.6f))
                SpatialFlowQueueHandle(actions, contentSecondary, preview)
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

/** SpatialFlow's wavy seek bar and its labels: elapsed on the left, what is left on the right. */
@Composable
private fun SpatialFlowSeek(
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    accent: Color,
    secondary: Color,
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
                    style = SeekBarStyle.M3_EXPRESSIVE,
                    fraction = shown,
                    activeColor = accent,
                    inactiveColor = Color.White.copy(alpha = 0.22f),
                    animateWave = playing,
                )
            },
            modifier = Modifier.fillMaxWidth().height(36.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val position = scrub.fraction?.let { (duration * it).toLong() } ?: progress.position
            Text(makeTimeString(position), color = secondary, style = MaterialTheme.typography.labelMedium)
            Text("-" + makeTimeString((duration - position).coerceAtLeast(0L)), color = secondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** The M3 Expressive button group: three connected capsules, the play one in the accent. */
@Composable
private fun SpatialFlowTransport(
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    transport: com.mochyy.mochymusic.ui.player.models.TransportState,
    accent: Color,
    onAccent: Color,
    contentColor: Color,
    preview: Boolean,
) {
    val interactive = LocalStyleInteractive.current
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }
    Row(
        Modifier.fillMaxWidth().height(76.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpatialFlowGroupButton(
            icon = R.drawable.skip_previous,
            description = R.string.previous,
            weight = 1f,
            container = contentColor.copy(alpha = 0.08f),
            content = contentColor,
            iconSize = 34.dp,
            enabled = !preview && transport.canSkipPrevious,
            onClick = tap(connection::seekToPrevious),
        )
        SpatialFlowGroupButton(
            icon = if (transport.isPlaying) R.drawable.pause else R.drawable.play,
            description = if (transport.isPlaying) R.string.pause else R.string.play,
            weight = 1.2f,
            container = accent,
            content = onAccent,
            iconSize = 40.dp,
            enabled = !preview,
            loading = transport.playbackState == Player.STATE_BUFFERING,
            onClick = tap {
                if (transport.playbackState == Player.STATE_ENDED) {
                    connection.player.seekTo(0, 0)
                    connection.player.playWhenReady = true
                } else {
                    connection.togglePlayPause()
                }
            },
        )
        SpatialFlowGroupButton(
            icon = R.drawable.skip_next,
            description = R.string.next,
            weight = 1f,
            container = contentColor.copy(alpha = 0.08f),
            content = contentColor,
            iconSize = 34.dp,
            enabled = !preview && transport.canSkipNext,
            onClick = tap(connection::seekToNext),
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SpatialFlowGroupButton(
    icon: Int,
    description: Int,
    weight: Float,
    container: Color,
    content: Color,
    iconSize: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    loading: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // SpatialFlow rounds each capsule off while it is held
    val corner by animateDpAsState(
        targetValue = if (pressed) 12.dp else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "spatialflowCorner",
    )
    Button(
        onClick = onClick,
        modifier = Modifier
            .weight(weight, fill = true)
            .height(76.dp),
        enabled = enabled,
        shape = RoundedCornerShape(corner),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = container.copy(alpha = 0.5f),
            disabledContentColor = content.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interaction,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(iconSize), color = content, strokeWidth = 3.dp)
        } else {
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(description),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun SpatialFlowChip(
    icon: Int,
    label: String,
    selected: Boolean,
    accent: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (selected) accent else contentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) accent else contentColor.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The swipe-up queue handle: a pill that opens the queue, or a drag up on it. */
@Composable
private fun SpatialFlowQueueHandle(
    actions: StyleActions,
    secondary: Color,
    preview: Boolean,
) {
    val open = actions.onQueue
    val interactive = LocalStyleInteractive.current
    var pulled by remember { mutableFloatStateOf(0f) }
    Column(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (open == null || preview) Modifier else Modifier
                    .pointerInput(open) {
                        whileStyleInteractive(interactive) {
                            detectVerticalDragGestures(
                                onDragStart = { pulled = 0f },
                                onVerticalDrag = { change, amount ->
                                    pulled += amount
                                    if (pulled < -12f) {
                                        pulled = 0f
                                        change.consume()
                                        open()
                                    }
                                },
                            )
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (interactive()) open() },
                    ),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(50)).background(secondary.copy(alpha = 0.5f)))
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.queue),
            style = MaterialTheme.typography.labelSmall,
            color = secondary,
        )
    }
}

@Composable
private fun SpatialFlowIcon(
    icon: Int,
    description: Int,
    tint: Color,
    size: Dp,
    iconSize: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
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
