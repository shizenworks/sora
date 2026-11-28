// the download rows Neiro's storage settings added; Android only

package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.SettingsItem

// Neiro's "download only on Wi-Fi": the row, with the BLOCKING badge while it is holding a metered network
@Composable
internal fun wifiOnlyItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    blocking: Boolean,
): SettingsItem {
    val title = stringResource(R.string.storage_download_wifi_only)
    val description = stringResource(
        if (blocking) R.string.storage_download_wifi_only_blocking else R.string.storage_download_wifi_only_desc,
    )
    return SettingsItem(
        title = { Text(title) },
        highlightKey = "Download only on Wi-Fi",
        searchText = title,
        descriptionText = description,
        icon = painterResource(R.drawable.wifi),
        iconShape = CircleShape,
        onClick = { onCheckedChange(!checked) },
        trailingContent = {
            AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
}

// Neiro's external downloader: off, it's a switch; on, it names the app and opens its editor
@Composable
internal fun externalDownloaderItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    packageName: String,
    onEdit: () -> Unit,
): SettingsItem {
    val title = stringResource(R.string.storage_external_downloader)
    val noPackage = stringResource(R.string.storage_external_downloader_no_package)
    val desc = stringResource(R.string.storage_external_downloader_desc)
    return SettingsItem(
        title = { Text(title) },
        highlightKey = "External downloader",
        searchText = title,
        descriptionText = when {
            !checked -> desc
            packageName.isBlank() -> noPackage
            else -> packageName
        },
        icon = painterResource(R.drawable.download),
        iconShape = CircleShape,
        onClick = { if (checked) onEdit() else onCheckedChange(true) },
        trailingContent = {
            if (checked) {
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.edit))
                }
            } else {
                AppSwitch(checked = false, onCheckedChange = onCheckedChange)
            }
        },
    )
}
