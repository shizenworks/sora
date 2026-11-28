package com.mochyy.mochymusic.ui.screens.settings.importsync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.import_sync_tunemymusic_title
import com.mochyy.mochymusic.shared.resources.playlist_add
import com.mochyy.mochymusic.shared.resources.tunemymusic_choose_file
import com.mochyy.mochymusic.shared.resources.tunemymusic_choose_file_desc
import com.mochyy.mochymusic.shared.resources.tunemymusic_info_intro
import com.mochyy.mochymusic.shared.resources.tunemymusic_no_browser
import com.mochyy.mochymusic.shared.resources.tunemymusic_open_site
import com.mochyy.mochymusic.shared.resources.tunemymusic_open_site_desc
import com.mochyy.mochymusic.shared.resources.tunemymusic_step_1
import com.mochyy.mochymusic.shared.resources.tunemymusic_step_2
import com.mochyy.mochymusic.shared.resources.tunemymusic_step_3
import com.mochyy.mochymusic.shared.resources.tunemymusic_step_4
import com.mochyy.mochymusic.shared.resources.web_link
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.viewmodels.ImportSyncViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val TUNEMYMUSIC_URL = "https://www.tunemymusic.com/transfer"

@Composable
fun TuneMyMusicImportScreen(
    navController: NavController,
    viewModel: ImportSyncViewModel = koinViewModel(),
    // the platform's browser and CSV picker; without them those rows stay hidden
    openUrl: ((String) -> Boolean)? = null,
    picker: ImportFilePicker? = null,
    notify: (String) -> Unit = {},
) {
    val noBrowser = stringResource(Res.string.tunemymusic_no_browser)
    val openItem =
        if (openUrl != null) {
            SettingsItem(
                title = { Text(stringResource(Res.string.tunemymusic_open_site)) },
                descriptionText = stringResource(Res.string.tunemymusic_open_site_desc),
                icon = painterResource(Res.drawable.web_link),
                iconShape = CircleShape,
                onClick = { if (!openUrl(TUNEMYMUSIC_URL)) notify(noBrowser) },
            )
        } else {
            null
        }
    val fileItem =
        if (picker != null) {
            SettingsItem(
                title = { Text(stringResource(Res.string.tunemymusic_choose_file)) },
                descriptionText = stringResource(Res.string.tunemymusic_choose_file_desc),
                icon = painterResource(Res.drawable.playlist_add),
                iconShape = CircleShape,
                onClick = { picker.pick("TuneMyMusic") { uri -> if (uri != null) viewModel.loadFile(uri, "TuneMyMusic") } },
            )
        } else {
            null
        }

    SubSettingsScaffold(
        title = stringResource(Res.string.import_sync_tunemymusic_title),
        onBack = { navController.navigateUp() },
    ) {
        ImportInfoCard {
            Text(
                stringResource(Res.string.tunemymusic_info_intro),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Step(1, stringResource(Res.string.tunemymusic_step_1))
                Step(2, stringResource(Res.string.tunemymusic_step_2))
                Step(3, stringResource(Res.string.tunemymusic_step_3))
                Step(4, stringResource(Res.string.tunemymusic_step_4))
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            style = SettingsGroupStyle.Grouped,
            items =
                listOfNotNull(
                    openItem,
                    fileItem,
                ),
        )
    }

    PendingImportHost(viewModel = viewModel, navController = navController, notify = notify)
}

@Composable
private fun Step(
    number: Int,
    text: String,
) {
    Row {
        Text("$number.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
