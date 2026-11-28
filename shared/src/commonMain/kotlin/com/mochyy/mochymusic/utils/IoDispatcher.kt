package com.mochyy.mochymusic.utils

import kotlinx.coroutines.CoroutineDispatcher

/**
 * The dispatcher for work that waits on the network or the disk, where the platform has one. The
 * app's own view models used Dispatchers.IO; this is that dispatcher spelled for commonMain. The
 * preferences cache reads and writes its store on it too.
 */
internal expect val ioDispatcher: CoroutineDispatcher
