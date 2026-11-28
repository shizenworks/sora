package com.mochyy.mochymusic.ui.utils

import coil3.request.ImageRequest
import coil3.request.allowHardware

internal actual fun ImageRequest.Builder.prepareForPalette(): ImageRequest.Builder = allowHardware(false)
