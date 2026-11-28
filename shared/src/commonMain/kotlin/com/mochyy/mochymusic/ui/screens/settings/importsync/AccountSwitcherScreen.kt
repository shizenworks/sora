// AccountSwitcherScreen.kt

package com.mochyy.mochymusic.ui.screens.settings.importsync

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.account
import com.mochyy.mochymusic.shared.resources.account_switcher_active
import com.mochyy.mochymusic.shared.resources.account_switcher_add
import com.mochyy.mochymusic.shared.resources.account_switcher_add_desc
import com.mochyy.mochymusic.shared.resources.account_switcher_empty
import com.mochyy.mochymusic.shared.resources.account_switcher_remove
import com.mochyy.mochymusic.shared.resources.account_switcher_remove_body
import com.mochyy.mochymusic.shared.resources.account_switcher_remove_title
import com.mochyy.mochymusic.shared.resources.account_switcher_saved_header
import com.mochyy.mochymusic.shared.resources.account_switcher_title
import com.mochyy.mochymusic.shared.resources.add
import com.mochyy.mochymusic.shared.resources.yt_sync_cancel
import com.mochyy.mochymusic.shared.resources.yt_sync_signed_in
import com.mochyy.mochymusic.ui.component.DefaultDialog
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.viewmodels.AccountSwitcherViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

const val AccountSwitcherRoute = "youtube_accounts"

@Composable
fun AccountSwitcherScreen(
    navController: NavController,
    viewModel: AccountSwitcherViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var pendingRemove by remember { mutableStateOf<SavedAccountInfo?>(null) }

    SubSettingsScaffold(
        title = stringResource(Res.string.account_switcher_title),
        onBack = { navController.navigateUp() },
    ) {
        SettingsGroup(
            title = stringResource(Res.string.account_switcher_saved_header),
            style = SettingsGroupStyle.Grouped,
            items =
                if (state.accounts.isEmpty()) {
                    listOf(
                        SettingsItem(
                            title = { Text(stringResource(Res.string.account_switcher_empty)) },
                            icon = painterResource(Res.drawable.account),
                            enabled = false,
                        ),
                    )
                } else {
                    state.accounts.map { account ->
                        val active = account.id == state.activeId
                        val onSwitch: () -> Unit = { viewModel.switchTo(account) }
                        SettingsItem(
                            title = { Text(account.name.ifBlank { account.email.ifBlank { stringResource(Res.string.yt_sync_signed_in) } }) },
                            descriptionText = account.email.ifBlank { null },
                            icon = painterResource(Res.drawable.account),
                            iconShape = CircleShape,
                            trailingContent = {
                                if (active) {
                                    Text(
                                        stringResource(Res.string.account_switcher_active),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    TextButton(onClick = { pendingRemove = account }) {
                                        Text(stringResource(Res.string.account_switcher_remove))
                                    }
                                }
                            },
                            onClick = if (active) null else onSwitch,
                        )
                    }
                },
        )

        Spacer(Modifier.height(16.dp))

        SettingsGroup(
            style = SettingsGroupStyle.Grouped,
            items =
                listOf(
                    SettingsItem(
                        title = { Text(stringResource(Res.string.account_switcher_add)) },
                        descriptionText = stringResource(Res.string.account_switcher_add_desc),
                        icon = painterResource(Res.drawable.add),
                        iconShape = CircleShape,
                        onClick = { navController.navigate(YouTubeLoginRoute) },
                    ),
                ),
        )
    }

    pendingRemove?.let { account ->
        DefaultDialog(
            onDismiss = { pendingRemove = null },
            horizontalAlignment = Alignment.Start,
            title = { Text(stringResource(Res.string.account_switcher_remove_title)) },
            buttons = {
                TextButton(onClick = { pendingRemove = null }) { Text(stringResource(Res.string.yt_sync_cancel)) }
                TextButton(onClick = {
                    pendingRemove = null
                    viewModel.remove(account)
                }) { Text(stringResource(Res.string.account_switcher_remove)) }
            },
        ) {
            Text(
                stringResource(Res.string.account_switcher_remove_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
