package com.mochyy.mochymusic.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SharedMigrationsTest {

    // a version bump without its step here would leave desktop and iOS unable to open their library
    @Test
    fun everyVersionSinceTheFirstNonAndroidOneHasAStep() {
        val steps = SharedMigrations.associateBy { it.startVersion }
        for (from in FirstNonAndroidVersion until MusicDatabaseVersion) {
            val step = assertNotNull(steps[from], "no shared migration from $from; MusicDatabaseVersion is $MusicDatabaseVersion")
            assertEquals(from + 1, step.endVersion, "the shared migration from $from must go one version up")
        }
    }

    @Test
    fun noStepPointsPastTheCurrentVersion() {
        for (step in SharedMigrations) {
            assertTrue(step.startVersion >= FirstNonAndroidVersion, "step from ${step.startVersion} predates any desktop/iOS database")
            assertTrue(step.endVersion <= MusicDatabaseVersion, "step to ${step.endVersion} is past MusicDatabaseVersion $MusicDatabaseVersion")
        }
    }
}
