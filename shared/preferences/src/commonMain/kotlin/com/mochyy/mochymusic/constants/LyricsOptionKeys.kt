
package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.Preferences

// the on/off switch of each lyrics source that has one, by the name LyricsProviderRegistry uses.
// the source's own isEnabled reads the same key, so flipping it here is what skips the source when fetching.
// sources not listed (YouTube's own lyrics, captions and subtitles, and modules) are always on.
data class LyricsSourceSwitch(val key: Preferences.Key<Boolean>, val defaultOn: Boolean = true)

val LyricsSourceSwitches: Map<String, LyricsSourceSwitch> = mapOf(
    "YouLyPlus" to LyricsSourceSwitch(EnableYouLyPlusKey),
    "Paxsenix" to LyricsSourceSwitch(EnablePaxsenixKey),
    "BetterLyrics" to LyricsSourceSwitch(EnableBetterLyricsKey),
    "BetterLyricsPortato" to LyricsSourceSwitch(EnableBetterLyricsPortatoKey),
    "Unison" to LyricsSourceSwitch(EnableUnisonKey),
    "SimpMusic" to LyricsSourceSwitch(EnableSimpMusicKey),
    "LrcLib" to LyricsSourceSwitch(EnableLrcLibKey),
    "Kugou" to LyricsSourceSwitch(EnableKugouKey),
    "NetEase" to LyricsSourceSwitch(EnableNetEaseKey),
    "QQMusic" to LyricsSourceSwitch(EnableQQMusicKey),
    "Deezer" to LyricsSourceSwitch(EnableDeezerKey),
    "LrcRed" to LyricsSourceSwitch(EnableLrcRedKey),
    "BiniLyrics" to LyricsSourceSwitch(EnableBiniLyricsKey),
    "Megalobiz" to LyricsSourceSwitch(EnableMegalobizKey),
    // genius scrapes a website, so it stays off until someone turns it on
    "Genius" to LyricsSourceSwitch(EnableGeniusLyricsKey, defaultOn = false),
)
