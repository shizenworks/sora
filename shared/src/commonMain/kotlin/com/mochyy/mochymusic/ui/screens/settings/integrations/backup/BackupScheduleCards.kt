// the backup page's two cards: back up on a schedule and how often, then how many backups the cloud keeps

package com.mochyy.mochymusic.ui.screens.settings.integrations.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mochyy.mochymusic.constants.CloudBackupAutoKey
import com.mochyy.mochymusic.constants.CloudBackupEveryMinutesKey
import com.mochyy.mochymusic.constants.CloudBackupKeepKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.backup_every_days
import com.mochyy.mochymusic.shared.resources.backup_every_hours
import com.mochyy.mochymusic.shared.resources.backup_every_minutes
import com.mochyy.mochymusic.shared.resources.backup_every_weeks
import com.mochyy.mochymusic.shared.resources.backup_keep_count
import com.mochyy.mochymusic.shared.resources.backup_auto_desc
import com.mochyy.mochymusic.shared.resources.backup_auto_title
import com.mochyy.mochymusic.shared.resources.backup_every
import com.mochyy.mochymusic.shared.resources.backup_keep
import com.mochyy.mochymusic.shared.resources.backup_keep_note
import com.mochyy.mochymusic.shared.resources.history
import com.mochyy.mochymusic.shared.resources.update
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.screens.settings.storage.StepSlider
import com.mochyy.mochymusic.ui.screens.settings.storage.StorageCard
import com.mochyy.mochymusic.ui.screens.settings.storage.StorageCardColumn
import com.mochyy.mochymusic.ui.screens.settings.storage.StorageMuted
import com.mochyy.mochymusic.ui.screens.settings.storage.storageText
import com.mochyy.mochymusic.utils.rememberPreference
import com.mochyy.mochymusic.viewmodels.BackupScheduleDefaults
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val MinutesInHour = 60
private const val MinutesInDay = 24 * MinutesInHour
private const val MinutesInWeek = 7 * MinutesInDay

@Composable
internal fun BackupScheduleCards(connected: Boolean) {
    val (auto, onAutoChange) = rememberPreference(CloudBackupAutoKey, defaultValue = false)
    val (everyMinutes, onEveryMinutesChange) = rememberPreference(CloudBackupEveryMinutesKey, BackupScheduleDefaults.DefaultEveryMinutes)
    val (keep, onKeepChange) = rememberPreference(CloudBackupKeepKey, BackupScheduleDefaults.DefaultKeep)
    val scheduled = auto && connected

    StorageCardColumn {
        CardHeader(icon = Res.drawable.update, title = stringResource(Res.string.backup_auto_title)) {
            AppSwitch(checked = scheduled, onCheckedChange = onAutoChange, enabled = connected)
        }
        Text(
            stringResource(Res.string.backup_auto_desc),
            style = storageText(13f, FontWeight.Medium, StorageMuted, -0.2f),
            modifier = Modifier.padding(top = 8.dp),
        )
        AnimatedVisibility(visible = scheduled) {
            Column {
                Spacer(Modifier.height(18.dp))
                ValueRow(stringResource(Res.string.backup_every), everyLabel(everyMinutes))
                Spacer(Modifier.height(12.dp))
                StepSlider(steps = BackupScheduleDefaults.EveryMinutesSteps, value = everyMinutes, onValueChange = onEveryMinutesChange)
                EndLabels(everyLabel(BackupScheduleDefaults.EveryMinutesSteps.first()), everyLabel(BackupScheduleDefaults.EveryMinutesSteps.last()))
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    StorageCardColumn {
        CardHeader(icon = Res.drawable.history, title = stringResource(Res.string.backup_keep)) {
            Text(pluralStringResource(Res.plurals.backup_keep_count, keep, keep), style = storageText(15f, FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))
        StepSlider(steps = BackupScheduleDefaults.KeepSteps, value = keep, onValueChange = onKeepChange)
        EndLabels(BackupScheduleDefaults.KeepSteps.first().toString(), BackupScheduleDefaults.KeepSteps.last().toString())
        Text(
            stringResource(Res.string.backup_keep_note),
            style = storageText(13f, FontWeight.Medium, StorageMuted, -0.2f),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

// the same header the storage page's cache card has: a small light disc with the icon, the title, then whatever sits on the right
@Composable
private fun CardHeader(icon: DrawableResource, title: String, trailing: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFFDADADA)),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = StorageCard, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, style = storageText(15f, FontWeight.SemiBold), modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = storageText(14f, FontWeight.Medium, StorageMuted, -0.2f), modifier = Modifier.weight(1f))
        Text(value, style = storageText(15f, FontWeight.Bold))
    }
}

@Composable
private fun EndLabels(start: String, end: String) {
    Row(Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)) {
        Text(start, style = storageText(10f, FontWeight.Medium, StorageMuted, 0f), modifier = Modifier.weight(1f))
        Text(end, style = storageText(10f, FontWeight.Medium, StorageMuted, 0f))
    }
}

// the biggest whole unit the step fits, so 4320 minutes reads as every 3 days
@Composable
private fun everyLabel(minutes: Int): String = when {
    minutes % MinutesInWeek == 0 -> (minutes / MinutesInWeek).let { pluralStringResource(Res.plurals.backup_every_weeks, it, it) }
    minutes % MinutesInDay == 0 -> (minutes / MinutesInDay).let { pluralStringResource(Res.plurals.backup_every_days, it, it) }
    minutes % MinutesInHour == 0 -> (minutes / MinutesInHour).let { pluralStringResource(Res.plurals.backup_every_hours, it, it) }
    else -> pluralStringResource(Res.plurals.backup_every_minutes, minutes, minutes)
}
