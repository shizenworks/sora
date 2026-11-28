// Android's browser, toasts, resume callback and two backup sheets, wired into the shared backup page.

package com.mochyy.mochymusic.ui.screens.settings.integrations.backup

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.LocalZoomOutOverlayState
import com.mochyy.mochymusic.ui.component.PopupSheetHandle
import com.mochyy.mochymusic.viewmodels.BackupCloud
import com.mochyy.mochymusic.viewmodels.CloudBackupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AndroidBackupScreen(
    navController: NavController,
    viewModel: CloudBackupViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val overlay = LocalZoomOutOverlayState.current
    val host = BackupScreenHost(
        toast = { message, long ->
            Toast.makeText(context, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        },
        openUrl = { url ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: ActivityNotFoundException) {
                false
            }
        },
        onResume = { onResume -> LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { onResume() } },
        openWebDav = { vm ->
            var sheet: PopupSheetHandle? = null
            sheet = overlay.showWithHandle {
                WebDavSignInContent(
                    viewModel = vm,
                    current = vm.account.value?.takeIf { it.kind == BackupCloud.WEBDAV },
                    onConnected = { server ->
                        sheet?.dismiss()
                        Toast.makeText(context, context.getString(R.string.backup_connected_toast, server), Toast.LENGTH_LONG).show()
                    },
                )
            }
        },
        openRestore = { vm -> overlay.show { RestoreBackupContent(vm) } },
    )
    BackupScreen(navController = navController, viewModel = viewModel, host = host)
}
