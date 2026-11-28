package com.mochyy.mochymusic.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import com.mochyy.mochymusic.importer.ImportSource
import com.mochyy.mochymusic.importer.account.AccountService
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.library_music
import com.mochyy.mochymusic.ui.screens.search.SearchColors
import com.mochyy.mochymusic.ui.screens.setup.onboarding.ProviderSquircle
import com.mochyy.mochymusic.ui.screens.setup.onboarding.SquircleShape
import org.jetbrains.compose.resources.painterResource

/** The account service a source stands for, where it has an app icon of its own to show. */
fun ImportSource.service(): AccountService? = when (this) {
    ImportSource.SPOTIFY -> AccountService.SPOTIFY
    ImportSource.APPLE_MUSIC -> AccountService.APPLE_MUSIC
    ImportSource.TIDAL -> AccountService.TIDAL
    ImportSource.YOUTUBE_MUSIC -> AccountService.YOUTUBE_MUSIC
    ImportSource.DEEZER, ImportSource.SOUNDCLOUD, ImportSource.OTHER -> null
}

/**
 * A source's service icon, or a neutral squircle with a note for a source with no icon of ours. The
 * library's Imported tab, an auto playlist's header and onboarding's link page all draw it, so it lives
 * here once.
 */
@Composable
fun ImportSourceIcon(source: ImportSource, size: Dp, modifier: Modifier = Modifier) {
    val service = source.service()
    if (service != null) {
        ProviderSquircle(service, size = size, modifier = modifier)
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(SquircleShape)
                .background(SearchColors.TileHigh),
        ) {
            Icon(
                painter = painterResource(Res.drawable.library_music),
                contentDescription = source.label,
                tint = SearchColors.Secondary,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}
