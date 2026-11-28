package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

// skip community-marked segments (sponsor reads, off-topic music) in YouTube songs
val SponsorBlockEnabledKey = booleanPreferencesKey("sponsorBlockEnabled")
val SponsorBlockCategoriesKey = stringSetPreferencesKey("sponsorBlockCategories")

/** Off-topic music is the one that matters for music; the rest are opt-in. Shared, because the settings page sets it. */
val SponsorBlockDefaultCategories = setOf("music_offtopic")
