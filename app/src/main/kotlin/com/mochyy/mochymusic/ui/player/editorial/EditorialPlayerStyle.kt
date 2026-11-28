package com.mochyy.mochymusic.ui.player.editorial

// The editorial player, adapted from Natune's V10 player style (NatuneGroup/Natune, GPL-3.0,
// shared-ui/.../playerstyles/V10Player.kt). The design is V10's: a field colour mixed from the
// cover, the artwork die-cut into a Material shape that morphs with the song, a serif italic
// title, a wide PLAY/PAUSE pill and the icon row under the wavy seek bar. Drawn here in Mochy's
// own style spine, and its inline queue/lyrics panels become the player's own pages.

import com.mochyy.mochymusic.ui.player.styles.rememberStyleScrub
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.LocalArtworkColor
import com.mochyy.mochymusic.LocalPlayerConnection
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.extensions.toggleRepeatMode
import com.mochyy.mochymusic.ui.component.SeekBarStyle
import com.mochyy.mochymusic.ui.component.SeekBarTrack
import com.mochyy.mochymusic.ui.player.styles.LocalStyleInteractive
import com.mochyy.mochymusic.ui.player.styles.StyleActions
import com.mochyy.mochymusic.ui.player.styles.StyleFrame
import com.mochyy.mochymusic.ui.player.styles.StyleMenu
import com.mochyy.mochymusic.ui.player.styles.StyleTones
import com.mochyy.mochymusic.ui.player.styles.StyleTrack
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTones
import com.mochyy.mochymusic.ui.player.styles.rememberStyleTrack
import com.mochyy.mochymusic.ui.player.styles.styleInsets
import com.mochyy.mochymusic.ui.player.styles.whileStyleInteractive
import com.mochyy.mochymusic.utils.makeTimeString

// the die-cut shapes the artwork cycles through, the same set V10 picks from
private val EditorialShapes: List<RoundedPolygon>
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    get() = listOf(
        MaterialShapes.Flower,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Puffy,
        MaterialShapes.Cookie12Sided,
        MaterialShapes.SoftBurst,
    )

private fun RoundedPolygon.toPath(): Path = Path().apply {
    cubics.forEachIndexed { index, cubic ->
        if (index == 0) moveTo(cubic.anchor0X, cubic.anchor0Y)
        cubicTo(
            cubic.control0X, cubic.control0Y,
            cubic.control1X, cubic.control1Y,
            cubic.anchor1X, cubic.anchor1Y,
        )
    }
    close()
}

private fun List<Cubic>.toPath(): Path = Path().apply {
    forEachIndexed { index, cubic ->
        if (index == 0) moveTo(cubic.anchor0X, cubic.anchor0Y)
        cubicTo(
            cubic.control0X, cubic.control0Y,
            cubic.control1X, cubic.control1Y,
            cubic.anchor1X, cubic.anchor1Y,
        )
    }
    close()
}

/** Clips to [path] scaled to fill the layout, the way the die-cut artwork is cut from its cover. */
private class EditorialShape(private val path: Path) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val scaled = Path().also { it.addPath(path) }
        val bounds = scaled.getBounds()
        if (bounds.width <= 0f || bounds.height <= 0f) return Outline.Rectangle(Rect(Offset.Zero, size))
        val s = minOf(size.width / bounds.width, size.height / bounds.height)
        scaled.transform(
            Matrix().apply {
                translate(
                    (size.width - bounds.width * s) / 2f - bounds.left * s,
                    (size.height - bounds.height * s) / 2f - bounds.top * s,
                )
                scale(s, s, 1f)
            },
        )
        return Outline.Generic(scaled)
    }
}

@Composable
internal fun EditorialPlayerStyle(
    actions: StyleActions,
    modifier: Modifier,
    preview: Boolean,
) {
    val connection = LocalPlayerConnection.current ?: return
    val track = rememberStyleTrack()
    val trackInfo by connection.uiState.trackInfo.collectAsState()
    val transport by connection.uiState.transportState.collectAsState()
    val dark = isSystemInDarkTheme()
    val dominant = LocalArtworkColor.current
    // V10's two colours: the field is the cover held almost to black (or white in daylight), and
    // the accent is the other end of the same mix, so type and field always pull apart
    val field by animateColorAsState(
        if (dark) lerp(dominant, Color.Black, 0.7f) else lerp(dominant, Color.White, 0.9f),
        tween(800),
        label = "editorialField",
    )
    val accent by animateColorAsState(
        if (dark) lerp(dominant, Color.White, 0.9f) else lerp(dominant, Color.Black, 0.9f),
        tween(800),
        label = "editorialAccent",
    )
    val tones = rememberStyleTones()
    // the styles' menu is dressed in the editorial colours, not the shared tones
    val editorialTones = StyleTones(
        played = accent.copy(alpha = 0.4f),
        remaining = field,
        ink = accent,
        inkSoft = accent.copy(alpha = 0.6f),
        chip = accent.copy(alpha = 0.12f),
    )
    val interactive = LocalStyleInteractive.current
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }

    StyleFrame(preview, modifier, tones, background = {
        Box(Modifier.fillMaxSize().background(field))
    }) { landscape, menuOpen, setMenuOpen ->
        val insets = styleInsets(landscape)
        val playing = transport.isPlaying
        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(
                            start = insets.start + 20.dp,
                            top = insets.top + 8.dp,
                            end = 12.dp,
                            bottom = insets.bottom + 20.dp,
                        ),
                ) {
                    EditorialHeader(actions, accent, editorialTones, preview, onMore = { setMenuOpen(true) })
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EditorialArtwork(track, playing, accent, Modifier.fillMaxWidth(0.72f))
                    }
                    EditorialMetadata(track, accent)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(
                            start = 12.dp,
                            top = insets.top + 8.dp,
                            end = insets.end + 20.dp,
                            bottom = insets.bottom + 20.dp,
                        )
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    EditorialControls(
                        connection = connection,
                        transport = transport,
                        trackInfo = trackInfo,
                        accent = accent,
                        field = field,
                        tones = editorialTones,
                        preview = preview,
                        onMore = { setMenuOpen(true) },
                        onQueue = actions.onQueue?.let { open -> tap(open) },
                    )
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = insets.start + 20.dp,
                        top = insets.top + 4.dp,
                        end = insets.end + 20.dp,
                        bottom = insets.bottom + 8.dp,
                    ),
            ) {
                EditorialHeader(actions, accent, editorialTones, preview, onMore = { setMenuOpen(true) })
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // a scroll, not a weight: the artwork keeps its square and a short window
                    // simply scrolls to the controls under it
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        EditorialArtwork(track, playing, accent, Modifier.fillMaxWidth(0.68f))
                    }
                    EditorialMetadata(track, accent)
                    EditorialControls(
                        connection = connection,
                        transport = transport,
                        trackInfo = trackInfo,
                        accent = accent,
                        field = field,
                        tones = editorialTones,
                        preview = preview,
                        onMore = { setMenuOpen(true) },
                        onQueue = actions.onQueue?.let { open -> tap(open) },
                    )
                }
            }
        }
        StyleMenu(
            open = menuOpen,
            onOpenChange = setMenuOpen,
            tones = editorialTones,
            actions = actions,
            atBottom = true,
            end = insets.end + 20.dp,
            edge = insets.bottom + 12.dp,
            showButton = false,
        )
    }
}

@Composable
private fun EditorialHeader(
    actions: StyleActions,
    accent: Color,
    tones: StyleTones,
    preview: Boolean,
    onMore: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        EditorialIcon(
            icon = R.drawable.expand_more,
            description = R.string.close,
            tint = accent,
            size = 44.dp,
            enabled = !preview,
            onClick = actions.onClose,
        )
        Spacer(Modifier.weight(1f))
        actions.onSleepTimer?.let { sleep ->
            EditorialIcon(
                icon = R.drawable.sleep_timer,
                description = R.string.sleep_timer,
                tint = accent,
                size = 44.dp,
                enabled = !preview,
                onClick = sleep,
            )
            Spacer(Modifier.width(6.dp))
        }
        actions.onLyrics?.let { lyrics ->
            EditorialIcon(
                icon = R.drawable.lyrics,
                description = R.string.lyrics,
                tint = accent,
                size = 44.dp,
                enabled = !preview,
                onClick = lyrics,
            )
            Spacer(Modifier.width(6.dp))
        }
        EditorialIcon(
            icon = R.drawable.more_horiz,
            description = R.string.more_options,
            tint = accent,
            size = 44.dp,
            enabled = !preview,
            onClick = onMore,
        )
    }
}

/** The cover cut into its song's shape, morphing when the next song takes a different one. */
@Composable
private fun EditorialArtwork(
    track: StyleTrack,
    playing: Boolean,
    accent: Color,
    modifier: Modifier,
) {
    val shapes = EditorialShapes
    // floor-mod, not abs: a title whose hash is Int.MIN_VALUE would otherwise index backwards
    val index = ((track.title.hashCode() % shapes.size) + shapes.size) % shapes.size
    val target = shapes[index]
    val progress = remember { Animatable(1f) }
    // the shape the artwork is morphing away from, held until the morph lands
    var from by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        if (from !== target) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(700))
            from = target
        }
    }
    val scale by animateFloatAsState(if (playing) 1f else 0.94f, tween(400), label = "editorialPauseScale")
    val targetShape = remember(target) { EditorialShape(target.toPath()) }
    // built once per song change, not once per frame; a pair of shapes with different curve counts
    // can't be morphed, and the artwork then simply takes the next shape instead of crashing
    val morph = remember(from, target) { if (from === target) null else runCatching { Morph(from, target) }.getOrNull() }
    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // the morph's progress is read here, while drawing, so its 700 ms re-draw the cut-out
                // without recomposing the artwork and its image on every frame
                val p = progress.value
                shape = if (morph == null || p >= 0.999f) targetShape
                else runCatching { EditorialShape(morph.asCubics(p).toPath()) }.getOrElse { targetShape }
                clip = true
            },
    ) {
        // a song without a cover keeps the cut-out, holding the field's own tint
        Box(Modifier.fillMaxSize().background(accent.copy(alpha = 0.12f)))
        AsyncImage(
            model = track.artwork,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun EditorialMetadata(track: StyleTrack, accent: Color) {
    val titleStyle = when {
        track.title.length > 40 -> MaterialTheme.typography.displaySmall
        track.title.length > 20 -> MaterialTheme.typography.displayMedium
        else -> MaterialTheme.typography.displayLarge
    }
    Text(
        text = track.title,
        color = accent,
        style = titleStyle.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = track.artist.uppercase(),
        color = accent.copy(alpha = 0.8f),
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun EditorialControls(
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    transport: com.mochyy.mochymusic.ui.player.models.TransportState,
    trackInfo: com.mochyy.mochymusic.ui.player.models.TrackInfo,
    accent: Color,
    field: Color,
    tones: StyleTones,
    preview: Boolean,
    onMore: () -> Unit,
    onQueue: (() -> Unit)?,
) {
    val interactive = LocalStyleInteractive.current
    fun tap(action: () -> Unit): () -> Unit = { if (interactive()) action() }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .then(
                // a swipe up anywhere on the controls opens the queue, as V10's gesture does
                if (onQueue == null) Modifier else Modifier.pointerInput(onQueue) {
                    whileStyleInteractive(interactive) {
                        var pulled = 0f
                        detectVerticalDragGestures(
                            onDragStart = { pulled = 0f },
                            onVerticalDrag = { change, amount ->
                                pulled += amount
                                if (pulled < -40f) {
                                    pulled = 0f
                                    change.consume()
                                    onQueue()
                                }
                            },
                        )
                    }
                },
            ),
    ) {
        Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(accent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !preview,
                        onClick = tap(connection::togglePlayPause),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (transport.playbackState == Player.STATE_BUFFERING) {
                    CircularProgressIndicator(Modifier.size(34.dp), color = field)
                } else {
                    Text(
                        text = stringResource(if (transport.isPlaying) R.string.pause else R.string.play),
                        color = field,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 3.sp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            EditorialIcon(
                icon = R.drawable.skip_next,
                description = R.string.next,
                tint = accent.copy(alpha = if (transport.canSkipNext) 1f else 0.4f),
                size = 80.dp,
                iconSize = 34.dp,
                enabled = !preview && transport.canSkipNext,
                onClick = connection::seekToNext,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EditorialIcon(
                icon = R.drawable.skip_previous,
                description = R.string.previous,
                tint = accent.copy(alpha = if (transport.canSkipPrevious) 1f else 0.4f),
                size = 80.dp,
                iconSize = 34.dp,
                enabled = !preview && transport.canSkipPrevious,
                onClick = connection::seekToPrevious,
            )
            EditorialSeek(connection, accent, transport.isPlaying, preview, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            val liked = trackInfo.liked
            EditorialIcon(
                icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                description = if (liked) R.string.action_remove_like else R.string.action_like,
                tint = accent.copy(alpha = if (liked) 1f else 0.5f),
                size = 44.dp,
                enabled = !preview,
                onClick = connection::toggleLike,
            )
            EditorialIcon(
                icon = R.drawable.shuffle,
                description = R.string.shuffle,
                tint = accent.copy(alpha = if (transport.shuffleModeEnabled) 1f else 0.5f),
                size = 44.dp,
                enabled = !preview,
                onClick = { connection.player.shuffleModeEnabled = !transport.shuffleModeEnabled },
            )
            EditorialIcon(
                icon = if (transport.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
                description = if (transport.repeatMode == Player.REPEAT_MODE_ONE) R.string.ps_style_repeat_one else R.string.repeat,
                tint = accent.copy(alpha = if (transport.repeatMode != Player.REPEAT_MODE_OFF) 1f else 0.5f),
                size = 44.dp,
                enabled = !preview,
                onClick = { connection.player.toggleRepeatMode() },
            )
            EditorialIcon(
                icon = R.drawable.add,
                description = R.string.more_options,
                tint = accent.copy(alpha = 0.8f),
                size = 44.dp,
                enabled = !preview,
                onClick = onMore,
            )
        }
    }
}

/** V10's squiggly bar: the played part a moving wave, the rest a plain line. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorialSeek(
    connection: com.mochyy.mochymusic.playback.PlayerConnection,
    accent: Color,
    playing: Boolean,
    preview: Boolean,
    modifier: Modifier = Modifier,
) {
    val progress by connection.progressState.collectAsState()
    val scrub = rememberStyleScrub(connection)
    val duration = progress.duration.coerceAtLeast(0L)
    val shown = scrub.fraction ?: progress.percentage.coerceIn(0f, 1f)
    val interactive = LocalStyleInteractive.current
    Column(modifier) {
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
                    inactiveColor = accent.copy(alpha = 0.3f),
                    animateWave = playing,
                )
            },
            modifier = Modifier.fillMaxWidth().height(36.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val position = scrub.fraction?.let { (duration * it).toLong() } ?: progress.position
            Text(makeTimeString(position), color = accent.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
            Text("-" + makeTimeString((duration - position).coerceAtLeast(0L)), color = accent.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** One bare glyph of V10's chrome: no fill, the tap target still a finger wide. */
@Composable
private fun EditorialIcon(
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
            .clip(CircleShape)
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
