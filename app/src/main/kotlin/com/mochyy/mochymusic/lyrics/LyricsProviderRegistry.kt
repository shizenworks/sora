// LyricsProviderRegistry.kt

package com.mochyy.mochymusic.lyrics

import com.mochyy.mochymusic.constants.PreferredLyricsProvider
import com.music.directlyrics.LrcRed
import com.music.directlyrics.Unison

object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "YouLyPlus"       to YouLyPlusLyricsProvider,
        "Paxsenix"        to PaxSenixLyricsProvider,
        "BetterLyrics"    to BetterLyricsProvider,
        "Unison"          to UnisonLyricsProvider,
        "SimpMusic"       to SimpMusicLyricsProvider,
        "Muzo"            to MuzoLyricsProvider,
        "LrcLib"          to LrcLibLyricsProvider,
        "Kugou"           to KuGouLyricsProvider,
        "NetEase"         to NetEaseLyricsProvider,
        "QQMusic"         to QQMusicLyricsProvider,
        "Deezer"          to DeezerLyricsProvider,
        "LrcRed"          to LrcRedLyricsProvider,
        "BiniLyrics"      to BiniLyricsProvider,
        "Megalobiz"       to MegalobizLyricsProvider,
        "BetterLyricsPortato" to BetterLyricsPortatoProvider,
        "Genius"          to GeniusLyricsProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
        "YouTubeMusic"    to YouTubeLyricsProvider,
        "YouTubeCaptions" to YouTubeCaptionsLyricsProvider,
    )

    // lyrics from modules that are on, set by the module registry; a module switched off simply isn't in here
    @Volatile
    private var moduleProviders: Map<String, ModuleLyricsProvider> = emptyMap()

    fun setModuleProviders(providers: List<ModuleLyricsProvider>) {
        moduleProviders = providers.associateBy { it.name }
    }

    val providerNames: List<String> get() = moduleProviders.keys.toList() + providerMap.keys

    // every installed module that adds lyrics, on or off, by provider name to the name it shows; an off one keeps its place in the order
    private var knownModules: Map<String, String> = emptyMap()

    fun setKnownModules(modules: Map<String, String>) {
        knownModules = modules
    }

    /** the names the order is kept over: every lyrics source in the app, the switched-off modules included */
    val orderNames: List<String> get() = (moduleProviders.keys + knownModules.keys).distinct() + providerMap.keys

    /** a module that adds lyrics but is switched off in Modules */
    fun isModuleOff(name: String): Boolean = name in knownModules && name !in moduleProviders

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name] ?: moduleProviders[name]

    /** the switch a source reads to say whether it's in the chain; null for a module's own sources */
    fun enableKey(name: String): androidx.datastore.preferences.core.Preferences.Key<Boolean>? = when (name) {
        "Kugou"           -> com.mochyy.mochymusic.constants.EnableKugouKey
        "LrcLib"          -> com.mochyy.mochymusic.constants.EnableLrcLibKey
        "BetterLyrics"    -> com.mochyy.mochymusic.constants.EnableBetterLyricsKey
        "SimpMusic"       -> com.mochyy.mochymusic.constants.EnableSimpMusicKey
        "YouLyPlus"       -> com.mochyy.mochymusic.constants.EnableYouLyPlusKey
        "Paxsenix"        -> com.mochyy.mochymusic.constants.EnablePaxsenixKey
        "NetEase"         -> com.mochyy.mochymusic.constants.EnableNetEaseKey
        "QQMusic"         -> com.mochyy.mochymusic.constants.EnableQQMusicKey
        "Deezer"          -> com.mochyy.mochymusic.constants.EnableDeezerKey
        "LrcRed"          -> com.mochyy.mochymusic.constants.EnableLrcRedKey
        "Unison"          -> com.mochyy.mochymusic.constants.EnableUnisonKey
        "Muzo"            -> com.mochyy.mochymusic.constants.EnableMuzoLyricsKey
        "YouTubeMusic"    -> com.mochyy.mochymusic.constants.EnableYouTubeMusicLyricsKey
        "YouTubeSubtitle" -> com.mochyy.mochymusic.constants.EnableYouTubeSubtitleLyricsKey
        "YouTubeCaptions" -> com.mochyy.mochymusic.constants.EnableYouTubeCaptionsKey
        else              -> null
    }

    /** A saved order, with any source added since it was saved at the end, in default order. */
    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) return getDefaultProviderOrder()
        val saved = orderString.split(",").map { it.trim() }.filter { it in orderNames }.distinct()
        // a lyrics module someone just turned on goes first, they added it to be asked
        val newModules = (moduleProviders.keys + knownModules.keys).distinct().filter { it !in saved }
        return newModules + saved + getDefaultProviderOrder().filter { it !in saved && it !in newModules }
    }

    // the order lyrics are really asked in: the saved one, or until someone sorts, the old single preferred pick first
    fun effectiveProviderOrder(orderString: String, preferred: PreferredLyricsProvider): List<String> {
        if (orderString.isNotBlank()) return deserializeProviderOrder(orderString)
        val preferredName = getProviderNameForEnum(preferred)
        return listOf(preferredName) + getDefaultProviderOrder().filter { it != preferredName }
    }

    fun serializeProviderOrder(providers: List<String>): String =
        providers.filter { it in orderNames }.joinToString(",")

    // NetEase, QQ Music and KuGou near the end: they are Chinese services, and LyricsHelper only
    // believes them first-hand for Chinese songs. NetEase leads them because it often has word
    // timing. YouTube's auto captions last of all - they are speech recognition.
    fun getDefaultProviderOrder(): List<String> = (moduleProviders.keys + knownModules.keys).distinct() + listOf(
        "BetterLyrics",
        "BetterLyricsPortato",
        "Unison",
        "YouLyPlus",
        "Paxsenix",
        "LrcRed",
        "BiniLyrics",
        "SimpMusic",
        "Muzo",
        "LrcLib",
        "Megalobiz",
        "Deezer",
        "YouTubeSubtitle",
        "YouTubeMusic",
        "NetEase",
        "QQMusic",
        "Kugou",
        "Genius",
        "YouTubeCaptions",
    )

    fun getProviderNameForEnum(enum: PreferredLyricsProvider): String = when (enum) {
        PreferredLyricsProvider.LRCLIB        -> "LrcLib"
        PreferredLyricsProvider.KUGOU         -> "Kugou"
        PreferredLyricsProvider.BETTER_LYRICS -> "BetterLyrics"
        PreferredLyricsProvider.SIMPMUSIC     -> "SimpMusic"
        PreferredLyricsProvider.YOULYPLUS     -> "YouLyPlus"
        PreferredLyricsProvider.PAXSENIX      -> "Paxsenix"
    }

    fun getDisplayName(name: String): String = when (name) {
        "YouLyPlus"       -> "YouLyPlus"
        "Paxsenix"        -> "PaxSenix"
        "BetterLyrics"    -> "Apple Music"
        "Unison"          -> "Unison"
        "SimpMusic"       -> "SimpMusic"
        "Muzo"            -> "Muzo Data"
        "LrcLib"          -> "LrcLib"
        "Kugou"           -> "KuGou"
        "NetEase"         -> "NetEase"
        "QQMusic"         -> "QQ Music"
        "Deezer"          -> "Deezer"
        "LrcRed"          -> "lrc.red"
        "BiniLyrics"      -> "BiniLyrics"
        "Megalobiz"       -> "Megalobiz"
        "BetterLyricsPortato" -> "BetterLyrics Portato"
        "Genius"          -> "Genius"
        "YouTubeSubtitle" -> "YouTube Subtitle"
        "YouTubeMusic"    -> "YouTube Music"
        "YouTubeCaptions" -> "YouTube Auto Captions"
        else              -> moduleProviders[name]?.displayName ?: knownModules[name] ?: name
    }

    /**
     * The open-source project behind a source, for the lyrics page's "Star on GitHub" button.
     * Only sources whose code is public: KuGou, NetEase, QQ Music, Deezer and YouTube are the
     * companies' own services, and Paxsenix hasn't published its server. SimpMusic's lyrics
     * server has no public repo of its own, so its star goes to the SimpMusic app.
     */
    fun getProject(name: String): LyricsProject? = when (name) {
        "YouLyPlus"    -> LyricsProject("LyricsPlus", "https://github.com/ibratabian17/LyricsPlus")
        "BetterLyrics" -> LyricsProject("Better Lyrics", "https://github.com/better-lyrics/better-lyrics")
        "Unison"       -> LyricsProject("Unison", Unison.RepoUrl)
        "LrcLib"       -> LyricsProject("LRCLIB", "https://github.com/tranxuanthang/lrclib")
        "SimpMusic"    -> LyricsProject("SimpMusic", "https://github.com/maxrave-dev/SimpMusic")
        else           -> null
    }

    // lrc.red and unison require linking back wherever their lyrics are shown
    fun getWebsite(name: String): String? = when (name) {
        "LrcRed" -> LrcRed.BaseUrl
        "BiniLyrics" -> "https://lyrics-api.binimum.org/"
        "Megalobiz" -> "https://www.megalobiz.com/"
        "Genius" -> "https://genius.com/"
        "Unison" -> Unison.BaseUrl
        else     -> null
    }
}

data class LyricsProject(val name: String, val repoUrl: String)
