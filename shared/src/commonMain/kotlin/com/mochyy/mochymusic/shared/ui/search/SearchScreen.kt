// SearchScreen.kt
// Song search for the desktop and iOS apps, in the Android app's look: a big title, a rounded dark
// field, then rows of artwork, title and artists. Tapping a row plays it.

package com.mochyy.mochymusic.shared.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.add_to_queue
import com.mochyy.mochymusic.shared.resources.search
import com.mochyy.mochymusic.shared.resources.search_failed
import com.mochyy.mochymusic.shared.resources.search_hint
import com.mochyy.mochymusic.shared.resources.search_no_results
import com.music.innertube.models.SongItem
import org.jetbrains.compose.resources.stringResource

/** [state] lives in the app shell, so the query and results survive tab and layout changes. */
@Composable
internal fun SearchScreen(
    state: SearchState,
    onPlay: (SongItem) -> Unit,
    onQueue: (SongItem) -> Unit,
    queueEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 32.dp)) {
        Spacer(Modifier.height(32.dp))
        Text(stringResource(Res.string.search), style = MaterialTheme.typography.displaySmall, color = Color.White)
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            if (state.query.isEmpty()) {
                Text(
                    stringResource(Res.string.search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
            BasicTextField(
                value = state.query,
                onValueChange = { state.query = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { state.submit() }, onDone = { state.submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        when (val current = state.ui) {
            SearchUi.Idle -> Unit
            SearchUi.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
            SearchUi.Failed -> Message(stringResource(Res.string.search_failed))
            is SearchUi.Results ->
                if (current.songs.isEmpty()) {
                    Message(stringResource(Res.string.search_no_results))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(current.songs.distinctBy { it.id }, key = { it.id }) { song ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SongRow(song, onClick = { onPlay(song) }, modifier = Modifier.weight(1f))
                                val label = stringResource(Res.string.add_to_queue)
                                IconButton(
                                    onClick = { onQueue(song) },
                                    enabled = queueEnabled,
                                    modifier = Modifier.semantics { contentDescription = label },
                                ) {
                                    Text("+", style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(96.dp)) }
                    }
                }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

@Composable
fun SongRow(song: SongItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        AsyncImage(
            model = song.thumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.06f)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        song.duration?.takeIf { it > 0 }?.let { seconds ->
            Spacer(Modifier.width(12.dp))
            Text(formatDuration(seconds * 1000L), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

/** "3:07", "1:02:45": plain digits, so it needs no locale formatter in common code. */
fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = (total % 60).toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$s" else "$m:$s"
}
