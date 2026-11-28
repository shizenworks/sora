// repo numbers for dev spaces; the fetch itself is the platform's (app: AppDevSpaceRepository, HttpURLConnection + org.json)

package com.mochyy.mochymusic.ui.screens.settings.devspace

import kotlinx.coroutines.flow.StateFlow

data class DevIssue(
    val number: Int,
    val title: String,
    val open: Boolean,
    val labels: List<String>,
    val url: String,
    /** github's created_at as epoch millis; the page only shows how many days ago that was */
    val createdAtMillis: Long,
) {
    /** a "bug" label, or the word in the title, reporters don't always label */
    val isBug: Boolean = labels.any { it.contains("bug", true) } || title.contains("bug", true)

    val isFeature: Boolean = !isBug && (
        labels.any { it.contains("enhancement", true) || it.contains("feature", true) } ||
            title.contains("feature", true) || title.contains("request", true)
        )
}

data class DevSpaceStats(
    val openTotal: Int = 0,
    val closedTotal: Int = 0,
    val newIssues: Int? = null,
    val newResolved: Int? = null,
    val commits: Int? = null,
    val releases: Int? = null,
    val issues: List<DevIssue> = emptyList(),
    val fetchedAt: Long = 0L,
) {
    val reportedTotal: Int get() = openTotal + closedTotal
}

/**
 * The numbers and issues the dev spaces page shows. Shared code only reads [current], [loading] and
 * [error]; who fetches them is the platform's choice, because the fetch is a JVM/Android HTTP stack
 * in :app. The app's implementation holds the same singleton state it always did, so the playground
 * page still reuses what the dev spaces page fetched.
 */
interface DevSpaceRepository {
    val current: StateFlow<DevSpaceStats?>
    val loading: StateFlow<Boolean>
    val error: StateFlow<String?>

    /**
     * Fetches everything again, unless [force] is false and the copy is newer than [maxAgeMs]. If
     * GitHub refuses (rate limit, offline) the last good numbers stay and [error] says why.
     */
    suspend fun refresh(force: Boolean, maxAgeMs: Long = 60_000L)
}
