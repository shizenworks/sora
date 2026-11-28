
package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.booleanPreferencesKey

// on: lossy youtube audio (aac, opus, vorbis) is decoded inside the app with ffmpeg; off: the phone's own codec does it, as before
val EfficientDecodingKey = booleanPreferencesKey("efficientDecoding")
