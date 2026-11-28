// the pieces app behaviour, playback and now playing build their rows from; shared, so desktop and iOS can build theirs too

package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.mochyy.mochymusic.ui.component.LocalPlatformSkin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.navigate_next
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.theme.InterFontFamily
import org.jetbrains.compose.resources.painterResource

// a section's title (if it has one) and its grouped rows, left out whole when the search hides every row
@Composable
fun SettingsSection(title: String?, items: List<SettingsItem>) {
    if (items.isEmpty()) return
    if (title != null) {
        Text(
            text = title,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 10.dp, start = 4.dp),
        )
    }
    // rows with a preview stand alone as cards where the platform draws them; everything else stays grouped
    val skin = LocalPlatformSkin.current
    val card = skin.previewCard
    val runs = mutableListOf<List<SettingsItem>>()
    items.forEach { item ->
        val last = runs.lastOrNull()
        val plain = card == null || item.preview == null
        if (plain && last != null && (card == null || last.first().preview == null)) runs[runs.lastIndex] = last + item
        else runs += listOf(item)
    }
    runs.forEachIndexed { i, run ->
        val first = run.first()
        val gap = if (i == runs.lastIndex) 18.dp else 12.dp
        val preview = first.preview
        if (card == null || preview == null) {
            SettingsGroup(style = SettingsGroupStyle.Grouped, items = run)
            Spacer(Modifier.height(gap))
        } else if (first.isVisible) {
            val highlight = skin.highlight
            val key = first.highlightKey
            val marked = if (highlight != null && key != null) Modifier.highlight(listOf(key)) else Modifier
            card(
                first.previewCategory ?: first.descriptionText.orEmpty(),
                first.searchText.orEmpty(),
                { first.onClick?.invoke() },
                marked,
                preview,
            )
            Spacer(Modifier.height(gap))
        }
    }
}

fun List<SettingsItem>.matching(query: String): List<SettingsItem> {
    if (query.isBlank()) return this
    return filter {
        it.highlightKey?.contains(query, ignoreCase = true) == true ||
            it.searchText?.contains(query, ignoreCase = true) == true ||
            it.descriptionText?.contains(query, ignoreCase = true) == true ||
            it.detail?.contains(query, ignoreCase = true) == true
    }
}

// a row title with its value on the right, for the rows with a slider under them
@Composable
fun ValueTitle(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title)
        Text(
            text = value,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.70f),
        )
    }
}

// one switch row: key is the english name search results point at, subOptionOf tucks it under a parent that's on;
// enabled = false greys it out and leaves it as it is
fun switchItem(
    key: String,
    title: String,
    description: String,
    icon: Painter,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    detail: String? = null,
    subOptionOf: Boolean? = null,
    visible: Boolean = true,
    enabled: Boolean = true,
) = SettingsItem(
    title = { Text(title) },
    highlightKey = key,
    searchText = title,
    descriptionText = description,
    detail = detail,
    enabled = enabled,
    icon = icon,
    iconShape = CircleShape,
    isVisible = visible && subOptionOf != false,
    isSubOption = subOptionOf != null,
    onClick = { onCheckedChange(!checked) },
    trailingContent = {
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled && subOptionOf != false)
    },
)

// a row that opens another page, with the little arrow on the right
fun linkItem(
    key: String,
    title: String,
    description: String,
    icon: Painter,
    onClick: () -> Unit,
    visible: Boolean = true,
    isSubOption: Boolean = false,
    category: String? = null,
    preview: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null,
) = SettingsItem(
    title = { Text(title) },
    highlightKey = key,
    searchText = title,
    descriptionText = description,
    icon = icon,
    iconShape = CircleShape,
    isVisible = visible,
    isSubOption = isSubOption,
    onClick = onClick,
    trailingContent = { NextArrow() },
    preview = preview,
    previewCategory = category,
)

@Composable
private fun NextArrow() {
    Icon(
        painter = painterResource(Res.drawable.navigate_next),
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.5f),
        modifier = Modifier.size(20.dp),
    )
}
