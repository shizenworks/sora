package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.db.nowEpochMillis
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.cipher_cache_cleared_msg
import com.mochyy.mochymusic.shared.resources.cipher_card_description
import com.mochyy.mochymusic.shared.resources.cipher_clear_cached_desc
import com.mochyy.mochymusic.shared.resources.cipher_clear_cached_title
import com.mochyy.mochymusic.shared.resources.cipher_downloading_script
import com.mochyy.mochymusic.shared.resources.cipher_due_now
import com.mochyy.mochymusic.shared.resources.cipher_duration_hms
import com.mochyy.mochymusic.shared.resources.cipher_duration_ms
import com.mochyy.mochymusic.shared.resources.cipher_duration_s
import com.mochyy.mochymusic.shared.resources.cipher_duration_zero
import com.mochyy.mochymusic.shared.resources.cipher_in_duration
import com.mochyy.mochymusic.shared.resources.cipher_last_updated
import com.mochyy.mochymusic.shared.resources.cipher_manual_refreshes_left
import com.mochyy.mochymusic.shared.resources.cipher_next_refresh
import com.mochyy.mochymusic.shared.resources.cipher_player_id
import com.mochyy.mochymusic.shared.resources.cipher_rate_limited
import com.mochyy.mochymusic.shared.resources.cipher_refresh_failed
import com.mochyy.mochymusic.shared.resources.cipher_refresh_now
import com.mochyy.mochymusic.shared.resources.cipher_refreshing
import com.mochyy.mochymusic.shared.resources.cipher_script_size
import com.mochyy.mochymusic.shared.resources.cipher_script_size_kb
import com.mochyy.mochymusic.shared.resources.cipher_settings_title
import com.mochyy.mochymusic.shared.resources.cipher_status_not_loaded
import com.mochyy.mochymusic.shared.resources.cipher_status_ready
import com.mochyy.mochymusic.shared.resources.cipher_status_stale
import com.mochyy.mochymusic.shared.resources.cipher_updated_to_player
import com.mochyy.mochymusic.shared.resources.refresh
import com.mochyy.mochymusic.shared.resources.restore
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** each refresh re-downloads youtube's player script, so spamming the button just gets us throttled */
private object CipherRefreshRateLimiter {
    private const val MAX_REFRESHES = 3
    private const val WINDOW_MS = 10 * 60 * 1000L

    private val timestamps = ArrayDeque<Long>()

    // the page reads and records this from its own composition and click handler, both on the UI thread
    private fun prune(now: Long) {
        while (timestamps.isNotEmpty() && now - timestamps.first() > WINDOW_MS) {
            timestamps.removeFirst()
        }
    }

    fun remaining(): Int {
        val now = nowEpochMillis()
        prune(now)
        return (MAX_REFRESHES - timestamps.size).coerceAtLeast(0)
    }

    /** milliseconds until another refresh is allowed, or 0 if one is available now */
    fun cooldownMs(): Long {
        val now = nowEpochMillis()
        prune(now)
        if (timestamps.size < MAX_REFRESHES) return 0L
        return (timestamps.first() + WINDOW_MS - now).coerceAtLeast(0L)
    }

    fun record() {
        timestamps.addLast(nowEpochMillis())
    }
}

@Composable
private fun formatDuration(millis: Long): String {
    if (millis <= 0L) return stringResource(Res.string.cipher_duration_zero)
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> stringResource(Res.string.cipher_duration_hms, hours.toInt(), minutes.toInt(), seconds.toInt())
        minutes > 0 -> stringResource(Res.string.cipher_duration_ms, minutes.toInt(), seconds.toInt())
        else -> stringResource(Res.string.cipher_duration_s, seconds.toInt())
    }
}

/** what the page has to say after a refresh or a clear, kept as the outcome so its sentence can be read in composition */
private sealed interface CipherMessage {
    data class Updated(val playerHash: String) : CipherMessage
    data object Failed : CipherMessage
    data object Cleared : CipherMessage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CipherSettingsScreen(
    navController: NavController,
    // the player-script download, its file cache and the date format stay in :app
    host: CipherHost,
) {
    val coroutineScope = rememberCoroutineScope()

    var status by remember { mutableStateOf(host.readStatus()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<CipherMessage?>(null) }
    // drives the countdown, ticks once a second
    var now by remember { mutableLongStateOf(nowEpochMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = nowEpochMillis()
            delay(1000)
        }
    }

    // reading now here keeps these recomputing as the clock ticks
    val remainingMs = remember(status, now, host.cacheTtlMs) { status.remainingMs(now, host.cacheTtlMs) }
    val cooldownMs = remember(now) { CipherRefreshRateLimiter.cooldownMs() }
    val remainingRefreshes = remember(now) { CipherRefreshRateLimiter.remaining() }

    val progress =
        remember(status, now, host.cacheTtlMs) {
            if (!status.hasCache) {
                0f
            } else {
                (1f - remainingMs.toFloat() / host.cacheTtlMs.toFloat()).coerceIn(0f, 1f)
            }
        }

    val statusLabel =
        when {
            !status.hasCache -> stringResource(Res.string.cipher_status_not_loaded)
            status.isExpired(now, host.cacheTtlMs) -> stringResource(Res.string.cipher_status_stale)
            else -> stringResource(Res.string.cipher_status_ready)
        }

    SubSettingsScaffold(
        title = stringResource(Res.string.cipher_settings_title),
        onBack = { navController.navigateUp() },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color =
                        if (status.isExpired(now, host.cacheTtlMs)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(Res.string.cipher_card_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                CipherInfoRow(stringResource(Res.string.cipher_player_id), status.playerHash ?: "—")
                CipherInfoRow(
                    stringResource(Res.string.cipher_last_updated),
                    status.lastUpdatedMs?.let { host.formatUpdatedAt(it) } ?: "—",
                )
                CipherInfoRow(
                    stringResource(Res.string.cipher_script_size),
                    if (status.sizeBytes > 0) stringResource(Res.string.cipher_script_size_kb, (status.sizeBytes / 1024).toInt()) else "—",
                )
                CipherInfoRow(
                    stringResource(Res.string.cipher_next_refresh),
                    if (!status.hasCache) {
                        "—"
                    } else if (remainingMs <=
                        0L
                    ) {
                        stringResource(Res.string.cipher_due_now)
                    } else {
                        stringResource(Res.string.cipher_in_duration, formatDuration(remainingMs))
                    },
                )

                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        message?.let { current ->
            Text(
                text = when (current) {
                    is CipherMessage.Updated -> stringResource(Res.string.cipher_updated_to_player, current.playerHash)
                    CipherMessage.Failed -> stringResource(Res.string.cipher_refresh_failed)
                    CipherMessage.Cleared -> stringResource(Res.string.cipher_cache_cleared_msg)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }

        SettingsGroup(
            style = SettingsGroupStyle.Grouped,
            items =
                listOf(
                    SettingsItem(
                        title = {
                            Text(
                                if (isRefreshing) {
                                    stringResource(
                                        Res.string.cipher_refreshing,
                                    )
                                } else {
                                    stringResource(Res.string.cipher_refresh_now)
                                },
                            )
                        },
                        descriptionText =
                            when {
                                isRefreshing -> {
                                    stringResource(Res.string.cipher_downloading_script)
                                }

                                cooldownMs > 0L -> {
                                    stringResource(Res.string.cipher_rate_limited, formatDuration(cooldownMs))
                                }

                                else -> {
                                    pluralStringResource(
                                        Res.plurals.cipher_manual_refreshes_left,
                                        remainingRefreshes,
                                        remainingRefreshes,
                                    )
                                }
                            },
                        icon = painterResource(Res.drawable.refresh),
                        iconShape = CircleShape,
                        onClick = onClick@{
                            if (isRefreshing || CipherRefreshRateLimiter.cooldownMs() > 0L) return@onClick
                            CipherRefreshRateLimiter.record()
                            isRefreshing = true
                            message = null
                            coroutineScope.launch {
                                val result = host.refresh()
                                status = host.readStatus()
                                message =
                                    if (result != null) {
                                        CipherMessage.Updated(result)
                                    } else {
                                        CipherMessage.Failed
                                    }
                                isRefreshing = false
                            }
                        },
                    ),
                    SettingsItem(
                        title = { Text(stringResource(Res.string.cipher_clear_cached_title)) },
                        descriptionText = stringResource(Res.string.cipher_clear_cached_desc),
                        icon = painterResource(Res.drawable.restore),
                        iconShape = CircleShape,
                        onClick = {
                            coroutineScope.launch {
                                host.clearCache()
                                status = host.readStatus()
                                message = CipherMessage.Cleared
                            }
                        },
                    ),
                ),
        )
    }
}

@Composable
private fun CipherInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

/** the player-script cache the cipher page reports on; :app owns the download, the file cache and the date format. */
interface CipherHost {
    /** how long a downloaded script is reused before it is fetched again */
    val cacheTtlMs: Long

    /** reads the cache metadata without downloading anything */
    fun readStatus(): CipherCacheStatus

    /** downloads a fresh script and answers its player hash, or null when the fetch failed */
    suspend fun refresh(): String?

    /** throws the cache away */
    suspend fun clearCache()

    /** the cache's download time in the device's own date and time format */
    fun formatUpdatedAt(epochMillis: Long): String
}

/** the on-disk player-script cache as the page needs it; hasCache and the countdown use [CipherHost.cacheTtlMs] */
data class CipherCacheStatus(
    val playerHash: String?,
    val lastUpdatedMs: Long?,
    val sizeBytes: Long,
) {
    val hasCache: Boolean get() = playerHash != null && lastUpdatedMs != null

    fun remainingMs(now: Long, ttlMs: Long): Long =
        lastUpdatedMs?.let { (it + ttlMs - now).coerceAtLeast(0L) } ?: 0L

    fun isExpired(now: Long, ttlMs: Long): Boolean = !hasCache || remainingMs(now, ttlMs) <= 0L
}
