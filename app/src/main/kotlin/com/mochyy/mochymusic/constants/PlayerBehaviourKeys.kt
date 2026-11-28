package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.booleanPreferencesKey

// swiping up on the player opens the queue, not the lyrics; off keeps the lyrics first
val SwipeUpOpensQueueKey = booleanPreferencesKey("swipeUpOpensQueue")
