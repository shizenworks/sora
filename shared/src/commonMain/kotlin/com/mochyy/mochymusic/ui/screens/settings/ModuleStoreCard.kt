// the module store's door on the settings page: mochy peeking up from the card and its name

package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.modules_bar_mochy
import com.mochyy.mochymusic.shared.resources.modules_find_more
import com.mochyy.mochymusic.ui.theme.InterFontFamily
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardShape = RoundedCornerShape(26.dp)

// the same card grey the audio lab pages use (LabColors.Card); the lab's own kit is still app-side
private val CardColor = Color(0xFF1C1C1E)

@Composable
internal fun ModuleStoreCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(CardShape)
            .background(CardColor)
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // the drawing ends at her jacket, so sitting on the card's bottom edge cuts her off there like the design
        Image(
            painterResource(Res.drawable.modules_bar_mochy), null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.Bottom).padding(top = 10.dp).fillMaxHeight(),
        )
        Spacer(Modifier.width(22.dp))
        Text(
            stringResource(Res.string.modules_find_more),
            fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 24.sp,
            letterSpacing = (-0.7).sp, color = Color.White, modifier = Modifier.weight(1f),
        )
    }
}
