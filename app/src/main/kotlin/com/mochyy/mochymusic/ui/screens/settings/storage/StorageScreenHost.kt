// Android's popup kit and transfer section, wired into the shared storage page.

package com.mochyy.mochymusic.ui.screens.settings.storage

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.mochyy.mochymusic.playback.StoredKind
import com.mochyy.mochymusic.ui.component.LocalMenuState
import com.mochyy.mochymusic.ui.screens.library.showConfirm
import com.mochyy.mochymusic.viewmodels.StorageKind
import com.mochyy.mochymusic.viewmodels.StorageViewModel
import org.koin.compose.viewmodel.koinViewModel

// the page is shared; its confirm dialog and three sheets are still :app's popup kit, and the
// transfer section keeps its SAF pickers and the transfer engine
@Composable
fun AndroidStorageScreen(
    navController: NavController,
    viewModel: StorageViewModel = koinViewModel(),
) {
    val menu = LocalMenuState.current
    val host = StorageScreenHost(
        showConfirm = { title, message, action, onConfirm ->
            menu.showConfirm(title, message, action, onConfirm)
        },
        manageDownloads = { menu.showManageDownloadsSheet(viewModel) },
        storagePlace = { kind -> menu.showStoragePlaceSheet(viewModel, kind.toStoredKind()) },
        exportDownloads = { menu.showExportDownloadsSheet(viewModel) },
        transferSection = { TransferSection() },
    )
    StorageScreen(navController = navController, viewModel = viewModel, host = host)
}

private fun StorageKind.toStoredKind(): StoredKind = when (this) {
    StorageKind.Downloads -> StoredKind.Downloads
    StorageKind.SongCache -> StoredKind.SongCache
}
