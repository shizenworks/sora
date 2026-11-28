package com.mochyy.mochymusic.db

import java.text.Collator
import java.util.Locale

actual fun nameCollator(): Comparator<String> {
    val collator = Collator.getInstance(Locale.getDefault())
    collator.strength = Collator.PRIMARY
    return Comparator { a, b -> collator.compare(a, b) }
}
