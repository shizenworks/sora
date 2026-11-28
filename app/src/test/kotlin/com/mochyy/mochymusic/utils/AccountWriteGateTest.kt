package com.mochyy.mochymusic.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountWriteGateTest {

    @Test
    fun writeQueuedByAnotherAccountIsRejected() {
        assertFalse(belongsToActiveAccount(queuedBy = "account-a", activeId = "account-b"))
    }

    @Test
    fun writeQueuedBySameAccountIsApplied() {
        assertTrue(belongsToActiveAccount(queuedBy = "account-a", activeId = "account-a"))
    }

    @Test
    fun writeWithNoRecordedAccountIsRejected() {
        assertFalse(belongsToActiveAccount(queuedBy = null, activeId = "account-a"))
        assertFalse(belongsToActiveAccount(queuedBy = "", activeId = ""))
    }

    @Test
    fun writeWhenNobodyIsSignedInIsRejected() {
        assertFalse(belongsToActiveAccount(queuedBy = "account-a", activeId = null))
    }
}
