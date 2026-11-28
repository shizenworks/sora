package com.mochyy.mochymusic.utils

/**
 * A queued YouTube write may run only while the account that queued it is still signed in.
 * [queuedBy] and [activeId] are [SavedAccount.opaqueId] values; null means no account was recorded.
 */
fun belongsToActiveAccount(queuedBy: String?, activeId: String?): Boolean =
    !queuedBy.isNullOrEmpty() && queuedBy == activeId
