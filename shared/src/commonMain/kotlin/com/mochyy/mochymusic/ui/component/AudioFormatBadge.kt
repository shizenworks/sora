// AudioFormatBadge.kt

package com.mochyy.mochymusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import com.mochyy.mochymusic.constants.AudioQuality
import com.mochyy.mochymusic.constants.EnableSpatialAudioKey
import com.mochyy.mochymusic.db.entities.FormatEntity
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.aac
import com.mochyy.mochymusic.shared.resources.dolby
import com.mochyy.mochymusic.shared.resources.flac
import com.mochyy.mochymusic.shared.resources.hi_res_flac
import com.mochyy.mochymusic.shared.resources.low_res_mp3
import com.mochyy.mochymusic.shared.resources.pc_audio_format_cd
import com.mochyy.mochymusic.utils.rememberPreference
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AudioFormatBadge(
    format: FormatEntity?,
    tint: Color,
    height: Dp,
    modifier: Modifier = Modifier,
    audioQuality: AudioQuality = AudioQuality.AUTO,
    fallbackId: String? = null
) {
    val mimeType = format?.mimeType?.lowercase() ?: ""
    val codecs = format?.codecs?.lowercase() ?: ""
    val ext = fallbackId?.substringAfterLast('.')?.lowercase() ?: ""

    val (spatialAudio) = rememberPreference(EnableSpatialAudioKey, defaultValue = false)

    val iconRes = when {
        mimeType.contains("flac") || mimeType.contains("alac") || ext == "flac" || ext == "alac" -> {
            val sampleRate = format?.sampleRate ?: 0
            if (sampleRate >= 48000) Res.drawable.hi_res_flac else Res.drawable.flac
        }
        mimeType.contains("mp4") || mimeType.contains("aac") || codecs.contains("aac") || ext == "m4a" || ext == "aac" -> Res.drawable.aac
        codecs.contains("opus") || mimeType.contains("webm") || ext == "webm" || ext == "opus" -> Res.drawable.low_res_mp3
        mimeType.contains("mp3") || mimeType.contains("mpeg") || ext == "mp3" -> Res.drawable.low_res_mp3
        codecs.contains("ac-3") || codecs.contains("ec-3") -> Res.drawable.dolby
        else -> Res.drawable.low_res_mp3
    }
    val contentColor = if (tint == Color.Unspecified) Color.White else tint
    val badgeWidth = when (iconRes) {
        Res.drawable.low_res_mp3 -> height * (107f / 34f)
        Res.drawable.aac -> height * (71f / 34f)
        else -> height * 2.1f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.requiredSize(width = badgeWidth, height = height)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = stringResource(Res.string.pc_audio_format_cd),
            colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(contentColor) else null,
            modifier = Modifier.requiredSize(width = badgeWidth, height = height)
        )
    }
}
