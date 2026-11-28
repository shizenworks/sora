package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// the saved custom themes as a json list; each one is a name and the seed colour its palette is generated from
val CustomThemesKey = stringPreferencesKey("customThemes")

@Serializable
data class CustomTheme(val name: String, val seed: Int)

// a blank or broken value reads as no saved themes, so the settings page still opens
fun decodeCustomThemes(json: String): List<CustomTheme> =
    if (json.isBlank()) emptyList() else runCatching { Json.decodeFromString<List<CustomTheme>>(json) }.getOrDefault(emptyList())

fun encodeCustomThemes(themes: List<CustomTheme>): String = Json.encodeToString(themes)
