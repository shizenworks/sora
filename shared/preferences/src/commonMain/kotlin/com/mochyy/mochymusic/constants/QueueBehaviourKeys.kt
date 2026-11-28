package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.booleanPreferencesKey

// queue rules: no duplicate songs, and play-next songs keep the order they were added in
val QueuePreventDuplicatesKey = booleanPreferencesKey("queuePreventDuplicates")
val QueueAddInOrderKey = booleanPreferencesKey("queueAddInOrder")
