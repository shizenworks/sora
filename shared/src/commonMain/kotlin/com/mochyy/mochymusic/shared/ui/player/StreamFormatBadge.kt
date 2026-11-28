// StreamFormatBadge.kt
// Mochy's codec badge ("OPUS · 48,0 KHz"), from the Android player, in common code so the desktop and
// iOS players show the same tag. Proportions are fractions of the height, as in the Android original.

package com.mochyy.mochymusic.shared.ui.player

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.shared.playback.StreamFormat

@Composable
fun StreamFormatBadge(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 20.dp,
    color: Color = Color.White.copy(alpha = 0.55f),
) {
    // the text is sized off the box, not the font scale, so the two never drift apart
    val fontSize = with(LocalDensity.current) { (height * 0.5f).toSp() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(height)
            .border(height * 0.0625f, color, RoundedCornerShape(height * 0.31f))
            .padding(horizontal = height * 0.29f),
    ) {
        Text(
            text = text,
            maxLines = 1,
            softWrap = false,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize,
                letterSpacing = 0.sp,
            ),
        )
    }
}

/** The badge's words: nothing until the codec is known; the sample rate written as the design writes it. */
fun streamBadgeText(format: StreamFormat?, showSampleRate: Boolean = true): String? {
    val codec = format?.codec ?: return null
    val rate = format.sampleRate.takeIf { showSampleRate }?.let(::formatSampleRateLabel) ?: return codec
    return "$codec · $rate"
}

/** The same compact rate label appears in Android's richer format badge and the shared player. */
fun formatSampleRateLabel(sampleRate: Int): String? {
    if (sampleRate <= 0) return null
    val tenths = (sampleRate.toLong() + 50) / 100
    return "${tenths / 10},${tenths % 10} KHz"
}
