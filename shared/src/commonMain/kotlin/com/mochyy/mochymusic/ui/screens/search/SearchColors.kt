// SearchColors.kt: the search page's palette, which settings rows also use for their card colour

package com.mochyy.mochymusic.ui.screens.search

import androidx.compose.ui.graphics.Color

object SearchColors {
    val Field = Color(0xFF141416)
    val FieldFocused = Color(0xFF1E1E21)
    val Tile = Color(0xFF121214)
    val TileHigh = Color(0xFF1C1C1F)
    val Divider = Color(0xFF232326)
    val Primary = Color.White
    val Secondary = Color(0xFF9A9AA0)
    val Placeholder = Color(0xFF77777D)

    fun page(pureBlack: Boolean): Color = Color.Black
}
