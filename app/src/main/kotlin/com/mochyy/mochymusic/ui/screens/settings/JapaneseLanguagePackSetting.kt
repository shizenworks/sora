// Adapted from Natune's LanguagePackSettings (github.com/NatuneGroup/Natune, GPL-3.0).
package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.lyrics.JapaneseLanguagePackManager
import com.mochyy.mochymusic.lyrics.JapaneseLanguagePackState
import com.mochyy.mochymusic.ui.component.SettingsItem

@Composable
internal fun japaneseLanguagePackSetting(): SettingsItem {
    val pack by JapaneseLanguagePackManager.state.collectAsStateWithLifecycle()
    val title = stringResource(R.string.japanese_pack_title)
    val description = when (val state = pack) {
        JapaneseLanguagePackState.Checking -> stringResource(R.string.japanese_pack_checking)
        JapaneseLanguagePackState.Removing -> stringResource(R.string.japanese_pack_removing)
        JapaneseLanguagePackState.NotInstalled -> stringResource(R.string.japanese_pack_description)
        is JapaneseLanguagePackState.Downloading -> if (state.progressPercent < 0) {
            stringResource(R.string.japanese_pack_preparing)
        } else {
            stringResource(R.string.japanese_pack_progress, state.progressPercent)
        }
        is JapaneseLanguagePackState.Installed -> stringResource(R.string.japanese_pack_installed)
        is JapaneseLanguagePackState.Failed -> state.message
    }
    return SettingsItem(
        title = { Text(title) },
        searchText = title,
        descriptionText = description,
        trailingContent = {
            when (val state = pack) {
                JapaneseLanguagePackState.Checking, JapaneseLanguagePackState.Removing ->
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                JapaneseLanguagePackState.NotInstalled ->
                    PackAction(R.string.japanese_pack_download, JapaneseLanguagePackManager::install)
                is JapaneseLanguagePackState.Installed ->
                    PackAction(R.string.japanese_pack_remove, JapaneseLanguagePackManager::remove)
                is JapaneseLanguagePackState.Downloading ->
                    PackAction(R.string.japanese_pack_cancel, JapaneseLanguagePackManager::cancel)
                is JapaneseLanguagePackState.Failed -> if (state.removalFailed) {
                    PackAction(R.string.japanese_pack_remove, JapaneseLanguagePackManager::remove)
                } else {
                    PackAction(R.string.japanese_pack_retry, JapaneseLanguagePackManager::install)
                }
            }
        },
    )
}

@Composable
private fun PackAction(label: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(stringResource(label)) }
}
