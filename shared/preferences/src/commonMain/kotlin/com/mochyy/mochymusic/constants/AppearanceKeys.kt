
package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// hide the bottom nav bar while a list scrolls down, show it again on the way up
val HideNavOnScrollKey = booleanPreferencesKey("hideNavOnScroll")

// the accent colour for the app's chrome as an ARGB number; 0 keeps mochy's own grey
val AccentColorKey = intPreferencesKey("accentColor")

// take the accent from the playing song's cover instead of the picked colour
val AccentFromArtKey = booleanPreferencesKey("accentFromArt")

// the whole app's size as a percentage, 100 is the phone's own size
val UiScalePercentKey = intPreferencesKey("uiScalePercent")

// use the tablet (wide) layout on any screen size, not only on a wide one
val TabletLayoutKey = booleanPreferencesKey("tabletLayout")

// the app's text in the phone's system font instead of mochy's own
val SystemFontKey = booleanPreferencesKey("systemFont")

// the shown name of the user's own font file (copied into the app as fonts/custom-regular); blank when there is none
val CustomFontKey = stringPreferencesKey("customFont")

// the shown name of that font's optional bold file (fonts/custom-bold); blank when there is none
val CustomBoldFontKey = stringPreferencesKey("customBoldFont")

// the bar's tabs in order, as their routes separated by commas; a leading "-" hides a tab. blank means the default
val NavTabsKey = stringPreferencesKey("navTabs")
