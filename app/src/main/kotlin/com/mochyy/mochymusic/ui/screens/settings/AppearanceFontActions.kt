package com.mochyy.mochymusic.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.ui.component.TopToaster
import com.mochyy.mochymusic.ui.theme.CustomFont
import kotlinx.coroutines.launch

// the appearance page's custom font rows are android's own: the document picker is an ActivityResult, the
// picked file is copied into the app's storage, and a pick that can't be used shows the top toast. The
// shared page draws the rows and asks these four actions; on desktop and iOS it passes none.
@Composable
fun rememberCustomFontActions(): CustomFontActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tooBig = stringResource(R.string.settings_custom_font_too_big)
    val notAFont = stringResource(R.string.settings_custom_font_unreadable)
    val notSaved = stringResource(R.string.settings_custom_font_not_saved)
    fun picker(part: CustomFont.Part) = { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                when (CustomFont.pick(context.applicationContext, uri, part)) {
                    is CustomFont.Picked.Done -> Unit
                    CustomFont.Picked.TooBig -> TopToaster.error(tooBig)
                    CustomFont.Picked.NotAFont -> TopToaster.error(notAFont)
                    CustomFont.Picked.NotSaved -> TopToaster.error(notSaved)
                }
            }
        }
    }
    val pickRegular = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), picker(CustomFont.Part.Regular))
    val pickBold = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), picker(CustomFont.Part.Bold))
    return CustomFontActions(
        pickRegular = { pickRegular.launch(CustomFont.PickerTypes) },
        pickBold = { pickBold.launch(CustomFont.PickerTypes) },
        removeRegular = { scope.launch { CustomFont.remove(context.applicationContext, CustomFont.Part.Regular) } },
        removeBold = { scope.launch { CustomFont.remove(context.applicationContext, CustomFont.Part.Bold) } },
    )
}
