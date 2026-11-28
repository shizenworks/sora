package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.runtime.Composable
import com.mochyy.mochymusic.constants.VideoQuality
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.video_quality_4k
import com.mochyy.mochymusic.shared.resources.video_quality_auto
import com.mochyy.mochymusic.shared.resources.video_quality_size
import org.jetbrains.compose.resources.stringResource

// how a size reads: "720p", and 4k by the name people know it by
@Composable
fun videoSizeName(shortSide: Int): String =
    if (shortSide >= 2160) stringResource(Res.string.video_quality_4k)
    else stringResource(Res.string.video_quality_size, shortSide)

// how a quality reads in settings: "Auto", or the size itself; the Now Playing row, the quality
// sheet and the player's video menu all name a quality through these two
@Composable
fun videoQualityName(quality: VideoQuality): String =
    if (quality == VideoQuality.AUTO) stringResource(Res.string.video_quality_auto)
    else videoSizeName(quality.shortSide)
