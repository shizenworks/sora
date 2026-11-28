package com.mochyy.mochymusic.db

// TODO: NSString localizedStandardCompare for language-aware order; case-insensitive until iOS has the library
actual fun nameCollator(): Comparator<String> = Comparator { a, b -> a.compareTo(b, ignoreCase = true) }
