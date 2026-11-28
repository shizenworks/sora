// LinkIdentity.kt

package com.mochyy.mochymusic.link

// this device as the other side sees it: a stable id that survives renames, and a name people recognise.
// android keeps the same preferences file and keys it always has, so nobody has to pair again after an update.
expect object LinkIdentity {
    fun id(platformContext: Any): String

    fun name(platformContext: Any): String
}
