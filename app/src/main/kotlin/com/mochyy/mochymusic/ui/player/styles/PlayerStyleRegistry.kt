package com.mochyy.mochymusic.ui.player.styles

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mochyy.mochymusic.constants.PlayerStyle

/*
 * The spine every player style hangs off. Adding a style takes three steps, all in one place each:
 *  1. an entry in PlayerStyle (constants/PreferenceKeys.kt) with its PlayerStyleTraits, before MODULE
 *  2. its composable, in its own package under ui/player/, taking (StyleActions, Modifier, preview)
 *  3. one line in the `when` below, and a spot in PlayerStyle.selectorColumns
 * The `when` has no else branch, so a style that was given an entry but no drawing won't compile.
 * Everything else (the handle, the studio, rotation, the tv) reads the traits, never the style's name.
 */

/** draws [style] as it looks on the player or, [preview], on a selector card */
@Composable
internal fun PlayerStyleContent(
    style: PlayerStyle,
    actions: StyleActions,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
) {
    when (style) {
        PlayerStyle.POSTER -> PosterPlayerStyle(actions, modifier, preview)
        PlayerStyle.TIMELINE -> TimelinePlayerStyle(actions, modifier, preview)
        PlayerStyle.LYRICS -> LyricsPlayerStyle(actions, modifier, preview)
        PlayerStyle.IPOD -> com.mochyy.mochymusic.ui.player.ipod.IpodPlayerStyle(modifier, preview)
        PlayerStyle.VIDEO -> com.mochyy.mochymusic.ui.player.video.VideoPlayerStyle(actions, modifier, preview)
        PlayerStyle.BEDSIDE -> com.mochyy.mochymusic.ui.player.bedside.BedsidePlayerStyle(actions, modifier, preview)
        PlayerStyle.BEDSIDE_TILES -> com.mochyy.mochymusic.ui.player.bedside.BedsideTilesStyle(actions, modifier, preview)
        PlayerStyle.NAMIDA -> com.mochyy.mochymusic.ui.player.namida.NamidaPlayerStyle(actions, modifier, preview)
        PlayerStyle.APPLE_MUSIC -> com.mochyy.mochymusic.ported.PortedPlayerHost(style, actions, modifier, preview)
        PlayerStyle.TIKTOK -> com.mochyy.mochymusic.ported.PortedPlayerHost(style, actions, modifier, preview)
        PlayerStyle.LOOPER -> com.mochyy.mochymusic.ui.player.looper.LooperPlayerStyle(actions, modifier, preview)
        PlayerStyle.SIMP -> com.mochyy.mochymusic.ui.player.simp.SimpPlayerStyle(actions, modifier, preview)
        PlayerStyle.METROLIST -> MetrolistPlayerStyle(actions, modifier, preview, vintage = false)
        PlayerStyle.METROLIST_VINTAGE -> MetrolistPlayerStyle(actions, modifier, preview, vintage = true)
        PlayerStyle.EDITORIAL -> com.mochyy.mochymusic.ui.player.editorial.EditorialPlayerStyle(actions, modifier, preview)
        PlayerStyle.FLAMINGO -> com.mochyy.mochymusic.ui.player.flamingo.FlamingoPlayerStyle(actions, modifier, preview)
        PlayerStyle.BITCHORD -> com.mochyy.mochymusic.ui.player.bitchord.BitChordPlayerStyle(actions, modifier, preview)
        PlayerStyle.SPATIAL_FLOW -> com.mochyy.mochymusic.ui.player.spatialflow.SpatialFlowPlayerStyle(actions, modifier, preview)
        PlayerStyle.MODULE -> ModulePlayerStyle(actions, modifier, preview)
        // the flagship family is drawn by the flagship player itself (StyleFamily.Flagship)
        PlayerStyle.FLAGSHIP, PlayerStyle.TURNTABLE -> Unit
    }
}
