// The anchored lyrics action list follows the popup in ArchiveTune/Natune by Rukamori and 4nx3b
// (github.com/NatuneGroup/Natune, GPL-3.0).
package com.mochyy.mochymusic.ui.player.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.R

private enum class LyricsTool(@StringRes val label: Int, @DrawableRes val icon: Int) {
    SOURCE(R.string.video_mode_lyrics_provider, R.drawable.lyrics),
    TIMING(R.string.video_mode_lyrics_offset, R.drawable.fast_forward),
    TRANSLATE(R.string.video_mode_lyrics_translation, R.drawable.translate),
    EDIT(R.string.edit_lyrics, R.drawable.edit),
}

@Composable
internal fun LyricsOverflowButton(
    songId: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(songId) { mutableStateOf(false) }
    var selectedTool by remember(songId) { mutableStateOf<LyricsTool?>(null) }
    val active = enabled && songId != null
    val dismissTool: () -> Unit = { selectedTool = null }

    LaunchedEffect(active) {
        if (!active) expanded = false
    }

    Box(modifier) {
        IconButton(
            onClick = { expanded = true },
            enabled = active,
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription = stringResource(R.string.lyrics_options),
                    tint = Color.White.copy(alpha = if (active) 0.9f else 0.4f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded && active,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 224.dp, max = 280.dp),
            offset = DpOffset(0.dp, 4.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = Color(0xFF252528),
            tonalElevation = 0.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            LyricsTool.entries.forEachIndexed { index, tool ->
                if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 0.5.dp)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(tool.label),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    trailingIcon = {
                        Icon(painterResource(tool.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        expanded = false
                        selectedTool = tool
                    },
                )
            }
        }
    }

    when (selectedTool) {
        LyricsTool.SOURCE -> LyricsProviderSheet(onDismiss = dismissTool)
        LyricsTool.TIMING -> LyricsOffsetSheet(onDismiss = dismissTool)
        LyricsTool.TRANSLATE -> LyricsTranslationSheet(onDismiss = dismissTool)
        LyricsTool.EDIT -> LyricsEditorSheet(onDismiss = dismissTool)
        null -> Unit
    }
}
