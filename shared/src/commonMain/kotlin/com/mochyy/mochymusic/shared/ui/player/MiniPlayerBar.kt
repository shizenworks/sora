// MiniPlayerBar.kt
// The bar along the bottom of the desktop and iOS windows while something plays: artwork, title and
// artists, Mochy's codec badge, a thin seek line and the play button with the Android app's icons.

package com.mochyy.mochymusic.shared.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.shared.playback.PlayerState
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.pause
import com.mochyy.mochymusic.shared.resources.play
import com.mochyy.mochymusic.shared.resources.player_pause
import com.mochyy.mochymusic.shared.resources.player_play
import com.mochyy.mochymusic.shared.ui.search.formatDuration
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MiniPlayerBar(
    state: PlayerState,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    showFormatBadge: Boolean = true,
    showSampleRate: Boolean = true,
) {
    val song = state.song ?: return
    Column(
        modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141414)),
    ) {
        SeekLine(state, onSeek)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            AsyncImage(
                model = song.thumbnail,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.06f)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        state.error ?: song.artists.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.error != null) Color(0xFFFF8A80) else Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (showFormatBadge) {
                        streamBadgeText(state.format, showSampleRate)?.let { StreamFormatBadge(it, height = 16.dp) }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "${formatDuration(state.positionMs)} / ${formatDuration(state.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
            )
            Spacer(Modifier.width(16.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onTogglePlay),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Image(
                        painter = painterResource(if (state.isPlaying) Res.drawable.player_pause else Res.drawable.player_play),
                        contentDescription = stringResource(if (state.isPlaying) Res.string.pause else Res.string.play),
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/** A thin progress line along the top of the bar; a click jumps there, the way the Android slider seeks. */
@Composable
private fun SeekLine(state: PlayerState, onSeek: (Long) -> Unit) {
    val fraction = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color.White.copy(alpha = 0.12f))
            .pointerInput(state.durationMs) {
                detectTapGestures { offset ->
                    if (state.durationMs > 0 && size.width > 0) {
                        onSeek((offset.x / size.width * state.durationMs).toLong())
                    }
                }
            },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(Color.White))
    }
}
