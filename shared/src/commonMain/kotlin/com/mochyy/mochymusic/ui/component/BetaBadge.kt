// the little outlined tag that marks something still settling: modules, lyrics share, which channel a build is

package com.mochyy.mochymusic.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.modules_beta
import org.jetbrains.compose.resources.stringResource

/** [label] in a thin pill of [color], a yellow "BETA" unless told otherwise */
@Composable
fun BetaBadge(
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.modules_beta),
    color: Color = BetaYellow,
) {
    Text(
        label,
        color = color,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.8f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 3.dp),
    )
}

private val BetaYellow = Color(0xFFF5E27A)
