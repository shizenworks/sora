// settings → storage: how much room mochy takes on each drive, the cache limit, clearing things, where things are saved and exporting songs

package com.mochyy.mochymusic.ui.screens.settings.storage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.MaxImageCacheSizeKey
import com.mochyy.mochymusic.constants.MaxSongCacheSizeKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.storage_export_done
import com.mochyy.mochymusic.shared.resources.cached
import com.mochyy.mochymusic.shared.resources.storage
import com.mochyy.mochymusic.shared.resources.storage_breakdown_title
import com.mochyy.mochymusic.shared.resources.storage_cache_applies_later
import com.mochyy.mochymusic.shared.resources.storage_cache_gb
import com.mochyy.mochymusic.shared.resources.storage_cache_limit
import com.mochyy.mochymusic.shared.resources.storage_cache_mb
import com.mochyy.mochymusic.shared.resources.storage_cache_none
import com.mochyy.mochymusic.shared.resources.storage_cache_off
import com.mochyy.mochymusic.shared.resources.storage_cache_place
import com.mochyy.mochymusic.shared.resources.storage_cache_unlimited
import com.mochyy.mochymusic.shared.resources.storage_canvas_gifs_detail
import com.mochyy.mochymusic.shared.resources.storage_canvas_gifs_none
import com.mochyy.mochymusic.shared.resources.storage_clear_cache
import com.mochyy.mochymusic.shared.resources.storage_clear_cache_action
import com.mochyy.mochymusic.shared.resources.storage_clear_cache_message
import com.mochyy.mochymusic.shared.resources.storage_clear_cache_title
import com.mochyy.mochymusic.shared.resources.storage_clear_canvas_gifs
import com.mochyy.mochymusic.shared.resources.storage_clear_canvas_gifs_action
import com.mochyy.mochymusic.shared.resources.storage_clear_canvas_gifs_message
import com.mochyy.mochymusic.shared.resources.storage_clear_canvas_gifs_title
import com.mochyy.mochymusic.shared.resources.storage_clear_local_covers
import com.mochyy.mochymusic.shared.resources.storage_clear_local_covers_action
import com.mochyy.mochymusic.shared.resources.storage_clear_local_covers_message
import com.mochyy.mochymusic.shared.resources.storage_clear_local_covers_title
import com.mochyy.mochymusic.shared.resources.storage_downloads_place
import com.mochyy.mochymusic.shared.resources.storage_export_downloads
import com.mochyy.mochymusic.shared.resources.storage_export_progress
import com.mochyy.mochymusic.shared.resources.storage_export_starting
import com.mochyy.mochymusic.shared.resources.storage_free
import com.mochyy.mochymusic.shared.resources.storage_hint_cache
import com.mochyy.mochymusic.shared.resources.storage_hint_downloads
import com.mochyy.mochymusic.shared.resources.storage_hint_full
import com.mochyy.mochymusic.shared.resources.storage_image_cache_limit
import com.mochyy.mochymusic.shared.resources.storage_junk_title
import com.mochyy.mochymusic.shared.resources.storage_legend_app_data
import com.mochyy.mochymusic.shared.resources.storage_legend_cache
import com.mochyy.mochymusic.shared.resources.storage_legend_database
import com.mochyy.mochymusic.shared.resources.storage_legend_downloads
import com.mochyy.mochymusic.shared.resources.storage_legend_local_covers
import com.mochyy.mochymusic.shared.resources.storage_legend_mochy
import com.mochyy.mochymusic.shared.resources.storage_legend_yours
import com.mochyy.mochymusic.shared.resources.storage_local_covers_detail
import com.mochyy.mochymusic.shared.resources.storage_manage_downloads
import com.mochyy.mochymusic.shared.resources.storage_measuring
import com.mochyy.mochymusic.shared.resources.storage_move_failed
import com.mochyy.mochymusic.shared.resources.storage_moving
import com.mochyy.mochymusic.shared.resources.storage_or
import com.mochyy.mochymusic.shared.resources.storage_overall
import com.mochyy.mochymusic.shared.resources.storage_remove_downloads
import com.mochyy.mochymusic.shared.resources.storage_remove_downloads_message
import com.mochyy.mochymusic.shared.resources.storage_remove_downloads_title
import com.mochyy.mochymusic.shared.resources.storage_uses
import com.mochyy.mochymusic.shared.resources.storage_uses_drives
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.viewmodels.DriveUsage
import com.mochyy.mochymusic.viewmodels.ExportStep
import com.mochyy.mochymusic.viewmodels.MoveStep
import com.mochyy.mochymusic.viewmodels.StorageKind
import com.mochyy.mochymusic.viewmodels.StorageUsage
import com.mochyy.mochymusic.viewmodels.StorageViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

const val StorageRoute = "storage_settings"

// song cache limits on offer, in mb, -1 is no limit, the player's default is 1 gb
private val CacheLimitSteps = listOf(0, 256, 512, 1024, 2048, 4096, 8192, -1)
// images are read in megabytes at app start and have no unlimited step
private val ImageCacheLimitSteps = listOf(0, 128, 256, 512, 1024, 2048)

/**
 * What the page asks the app to open: the confirm dialog and the three sheets all live in :app's popup
 * kit, so they come in as calls instead of being part of the shared page. A host that passes none of
 * them gets the page without its action sheets; Android passes all of them.
 */
class StorageScreenHost(
    val showConfirm: (title: String, message: String, action: String, onConfirm: () -> Unit) -> Unit = { _, _, _, _ -> },
    val manageDownloads: () -> Unit = {},
    val storagePlace: (StorageKind) -> Unit = {},
    val exportDownloads: () -> Unit = {},
    // the transfer section lives in :app with its SAF pickers and the transfer engine
    val transferSection: (@Composable () -> Unit)? = null,
)

@Composable
fun StorageScreen(
    navController: NavController,
    viewModel: StorageViewModel = koinViewModel(),
    host: StorageScreenHost = StorageScreenHost(),
) {
    val usage by viewModel.usage.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val (cacheLimit, setCacheLimit) = rememberPreference(MaxSongCacheSizeKey, 1024)
    // the player picked its limit when the app started; a new one only applies from the next start
    val startingLimit = remember { cacheLimit }
    val (imageCacheLimit, setImageCacheLimit) = rememberPreference(MaxImageCacheSizeKey, 512)
    val startingImageLimit = remember { imageCacheLimit }

    // songs removed from the Manage downloads sheet change the numbers here too
    LaunchedEffect(downloads?.size) { viewModel.measure() }

    SubSettingsScaffold(title = stringResource(Res.string.storage), onBack = { navController.navigateUp() }) {
        // one overall card per drive mochy keeps things on, the phone first
        val drives = usage?.drives
        if (drives == null) {
            OverallCard(null)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { drives.forEach { OverallCard(it) } }
        }

        Spacer(Modifier.height(22.dp))
        SectionText(usage?.let { headline(it) } ?: stringResource(Res.string.storage_measuring))
        Spacer(Modifier.height(14.dp))
        BreakdownCard(usage)

        Spacer(Modifier.height(26.dp))
        SectionText(stringResource(Res.string.storage_junk_title))
        usage?.let { SmartHint(it, downloadCount = downloads?.size ?: 0) }
        Spacer(Modifier.height(14.dp))
        CacheLimitCard(
            limit = cacheLimit,
            onLimitChange = setCacheLimit,
            appliesLater = cacheLimit != startingLimit,
        )
        Spacer(Modifier.height(10.dp))
        CacheLimitCard(
            limit = imageCacheLimit,
            onLimitChange = setImageCacheLimit,
            appliesLater = imageCacheLimit != startingImageLimit,
            title = stringResource(Res.string.storage_image_cache_limit),
            steps = ImageCacheLimitSteps,
        )
        Spacer(Modifier.height(10.dp))
        ClearButtons(viewModel, host, downloadCount = downloads?.size ?: 0, usage = usage)

        Spacer(Modifier.height(26.dp))
        SectionText(stringResource(Res.string.storage_or))
        Spacer(Modifier.height(14.dp))
        PlaceButtons(viewModel, host, downloadCount = downloads?.size ?: 0)

        Spacer(Modifier.height(26.dp))
        host.transferSection?.invoke()
    }
}

@Composable
private fun SectionText(text: String) {
    Text(text, style = storageText(17f, FontWeight.SemiBold), modifier = Modifier.padding(horizontal = 2.dp))
}

@Composable
private fun headline(usage: StorageUsage): String =
    if (usage.drives.size == 1) {
        stringResource(Res.string.storage_uses, percentOf(usage.mochy, usage.phone.total), formatSize(usage.mochy))
    } else {
        stringResource(Res.string.storage_uses_drives, formatSize(usage.mochy), usage.drives.size)
    }

// like 1.4%, or <0.1% for a sliver, so a small app never reads as using nothing
private fun percentOf(part: Long, total: Long): String {
    if (total <= 0L) return "0%"
    val p = part * 100.0 / total
    return when {
        p < 0.1 -> "<0.1%"
        p < 10 -> {
            // one decimal, by integer arithmetic: common Kotlin has no locale-aware %.1f
            val tenths = (p * 10).roundToInt()
            "${tenths / 10}.${tenths % 10}%"
        }
        else -> "${p.toInt()}%"
    }
}

@Composable
private fun OverallCard(drive: DriveUsage?) {
    StorageCardColumn {
        Text(
            if (drive == null || drive.isPhone) stringResource(Res.string.storage_overall) else drive.label,
            style = storageText(17f),
        )
        Spacer(Modifier.height(14.dp))
        UsageBar(
            parts = listOf(
                BarPart(MochyGreen, drive?.mochy ?: 0),
                BarPart(OthersGrey, drive?.othersUsed ?: 0),
            ),
            total = drive?.total ?: 0,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            LegendSwatch(MochyGreen)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.storage_legend_mochy), style = storageText(16f, FontWeight.SemiBold))
            Spacer(Modifier.width(16.dp))
            LegendSwatch(OthersGrey)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.storage_legend_yours), style = storageText(16f, FontWeight.SemiBold), modifier = Modifier.weight(1f), maxLines = 1)
            if (drive != null) {
                Text(stringResource(Res.string.storage_free, formatSize(drive.free)), style = storageText(12f, FontWeight.Medium, StorageMuted, 0f))
            }
        }
    }
}

@Composable
private fun BreakdownCard(usage: StorageUsage?) {
    StorageCardColumn {
        Text(stringResource(Res.string.storage_breakdown_title), style = storageText(17f))
        Spacer(Modifier.height(14.dp))
        UsageBar(
            parts = listOf(
                BarPart(CacheOrange, usage?.cache ?: 0),
                BarPart(MochyGreen, usage?.downloads ?: 0),
                BarPart(DatabaseRed, usage?.database ?: 0),
                BarPart(CoversYellow, usage?.localCovers ?: 0),
                BarPart(AppDataBlue, usage?.appData ?: 0),
            ),
            total = usage?.mochy ?: 0,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LegendRow(CacheOrange, stringResource(Res.string.storage_legend_cache), usage?.let { formatSize(it.cache) })
            LegendRow(MochyGreen, stringResource(Res.string.storage_legend_downloads), usage?.let { formatSize(it.downloads) })
            LegendRow(DatabaseRed, stringResource(Res.string.storage_legend_database), usage?.let { formatSize(it.database) })
            LegendRow(CoversYellow, stringResource(Res.string.storage_legend_local_covers), usage?.let { formatSize(it.localCovers) })
            LegendRow(AppDataBlue, stringResource(Res.string.storage_legend_app_data), usage?.let { formatSize(it.appData) })
        }
    }
}

// one line pointing at the biggest easy win, the cache or the downloads sheet, and a nudge when the phone is nearly full
@Composable
private fun SmartHint(usage: StorageUsage, downloadCount: Int) {
    val phone = usage.phone
    val nearlyFull = phone.total > 0 && phone.free * 10 < phone.total
    val hint = when {
        usage.cache >= usage.downloads && usage.cache > 50_000_000L -> stringResource(Res.string.storage_hint_cache, formatSize(usage.cache))
        downloadCount > 0 && usage.downloads > 50_000_000L -> stringResource(Res.string.storage_hint_downloads)
        else -> null
    }
    val full = if (nearlyFull) stringResource(Res.string.storage_hint_full) else null
    val text = listOfNotNull(full, hint).joinToString(" ")
    if (text.isEmpty()) return
    Text(
        text,
        style = storageText(13f, FontWeight.Medium, StorageMuted, -0.2f),
        modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 6.dp),
    )
}

@Composable
private fun CacheLimitCard(
    limit: Int,
    onLimitChange: (Int) -> Unit,
    appliesLater: Boolean,
    title: String = stringResource(Res.string.storage_cache_limit),
    steps: List<Int> = CacheLimitSteps,
) {
    StorageCardColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDADADA)),
            ) {
                Icon(painterResource(Res.drawable.cached), contentDescription = null, tint = StorageCard, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = storageText(15f, FontWeight.SemiBold), modifier = Modifier.weight(1f))
            Text(cacheLimitLabel(limit), style = storageText(15f, FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))
        StepSlider(steps = steps, value = limit, onValueChange = onLimitChange)
        Row(Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)) {
            Text(stringResource(Res.string.storage_cache_none), style = storageText(10f, FontWeight.Medium, StorageMuted, 0f), modifier = Modifier.weight(1f))
            // the slider's real ends: the image cache tops out at 2 gb, only the song cache has a no-limit step
            Text(cacheLimitLabel(steps.last()), style = storageText(10f, FontWeight.Medium, StorageMuted, 0f))
        }
        AnimatedVisibility(
            visible = appliesLater,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Text(
                stringResource(Res.string.storage_cache_applies_later),
                style = storageText(12f, FontWeight.Medium, StorageMuted, -0.1f),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun cacheLimitLabel(megabytes: Int): String = when {
    megabytes < 0 -> stringResource(Res.string.storage_cache_unlimited)
    megabytes == 0 -> stringResource(Res.string.storage_cache_off)
    megabytes < 1024 -> stringResource(Res.string.storage_cache_mb, megabytes)
    else -> stringResource(Res.string.storage_cache_gb, megabytes / 1024)
}

@Composable
private fun ClearButtons(viewModel: StorageViewModel, host: StorageScreenHost, downloadCount: Int, usage: StorageUsage?) {
    val busy by viewModel.busy.collectAsState()
    val gifs by viewModel.canvasGifs.collectAsState()
    val removeTitle = stringResource(Res.string.storage_remove_downloads_title)
    val removeMessage = stringResource(Res.string.storage_remove_downloads_message)
    val removeAction = stringResource(Res.string.storage_remove_downloads)
    val clearTitle = stringResource(Res.string.storage_clear_cache_title)
    val clearMessage = stringResource(Res.string.storage_clear_cache_message)
    val clearAction = stringResource(Res.string.storage_clear_cache_action)
    val coversTitle = stringResource(Res.string.storage_clear_local_covers_title)
    val coversMessage = stringResource(Res.string.storage_clear_local_covers_message)
    val coversAction = stringResource(Res.string.storage_clear_local_covers_action)
    val gifTitle = stringResource(Res.string.storage_clear_canvas_gifs_title)
    val gifMessage = stringResource(Res.string.storage_clear_canvas_gifs_message)
    val gifAction = stringResource(Res.string.storage_clear_canvas_gifs_action)
    // how much room the canvas gifs for discord take, measured again after a clear
    LaunchedEffect(busy) { if (!busy) viewModel.refreshCanvasGifs() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StorageActionButton(
            label = removeAction,
            danger = true,
            enabled = !busy && downloadCount > 0,
            onClick = { host.showConfirm(removeTitle, removeMessage, removeAction, viewModel::removeAllDownloads) },
        )
        StorageActionButton(
            label = stringResource(Res.string.storage_clear_cache),
            danger = true,
            enabled = !busy,
            onClick = { host.showConfirm(clearTitle, clearMessage, clearAction, viewModel::clearCaches) },
        )
        StorageActionButton(
            label = stringResource(Res.string.storage_clear_local_covers),
            detail = usage?.let { stringResource(Res.string.storage_local_covers_detail, formatSize(it.localCovers)) },
            danger = true,
            enabled = !busy && (usage?.localCovers ?: 0L) > 0L,
            onClick = { host.showConfirm(coversTitle, coversMessage, coversAction, viewModel::clearLocalCovers) },
        )
        StorageActionButton(
            label = stringResource(Res.string.storage_clear_canvas_gifs),
            detail = if ((gifs?.count ?: 0) == 0) stringResource(Res.string.storage_canvas_gifs_none)
            else stringResource(Res.string.storage_canvas_gifs_detail, formatSize(gifs?.bytes ?: 0L), gifs?.count ?: 0),
            danger = true,
            enabled = !busy && (gifs?.count ?: 0) > 0,
            onClick = { host.showConfirm(gifTitle, gifMessage, gifAction, viewModel::clearCanvasGifs) },
        )
        StorageActionButton(
            label = stringResource(Res.string.storage_manage_downloads),
            enabled = downloadCount > 0,
            onClick = host.manageDownloads,
        )
    }
}

// where downloads and the cache are saved, and copying the songs out; each button only speaks up while its job runs
@Composable
private fun PlaceButtons(viewModel: StorageViewModel, host: StorageScreenHost, downloadCount: Int) {
    val move by viewModel.moveStep.collectAsState()
    val export by viewModel.exportStep.collectAsState()
    val moving = move as? MoveStep.Working

    @Composable
    fun moveDetail(kind: StorageKind): String? = when {
        moving?.kind == kind -> stringResource(Res.string.storage_moving, (moving.progress * 100).toInt())
        (move as? MoveStep.Failed)?.kind == kind -> stringResource(Res.string.storage_move_failed)
        else -> null
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StorageActionButton(
            label = stringResource(Res.string.storage_downloads_place),
            detail = moveDetail(StorageKind.Downloads),
            enabled = moving == null,
            onClick = { host.storagePlace(StorageKind.Downloads) },
        )
        StorageActionButton(
            label = stringResource(Res.string.storage_cache_place),
            detail = moveDetail(StorageKind.SongCache),
            enabled = moving == null,
            onClick = { host.storagePlace(StorageKind.SongCache) },
        )
        StorageActionButton(
            label = stringResource(Res.string.storage_export_downloads),
            detail = when (val step = export) {
                is ExportStep.Working -> if (step.total == 0) stringResource(Res.string.storage_export_starting)
                else stringResource(Res.string.storage_export_progress, step.done + 1, step.total)
                is ExportStep.Done -> pluralStringResource(Res.plurals.storage_export_done, step.songs, step.songs)
                is ExportStep.Failed -> step.message
                ExportStep.Idle -> null
            },
            enabled = downloadCount > 0,
            onClick = host.exportDownloads,
        )
    }
}
