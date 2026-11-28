// ProxySettingsScreen.kt

package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.ProxyEnabledKey
import com.mochyy.mochymusic.constants.ProxyPasswordKey
import com.mochyy.mochymusic.constants.ProxyTypeKey
import com.mochyy.mochymusic.constants.ProxyUrlKey
import com.mochyy.mochymusic.constants.ProxyUsernameKey
import com.mochyy.mochymusic.extensions.toEnum
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.key
import com.mochyy.mochymusic.shared.resources.link
import com.mochyy.mochymusic.shared.resources.network_node
import com.mochyy.mochymusic.shared.resources.network_update
import com.mochyy.mochymusic.shared.resources.proxy
import com.mochyy.mochymusic.shared.resources.proxy_address
import com.mochyy.mochymusic.shared.resources.proxy_address_hint
import com.mochyy.mochymusic.shared.resources.proxy_address_not_set
import com.mochyy.mochymusic.shared.resources.proxy_check
import com.mochyy.mochymusic.shared.resources.proxy_check_desc
import com.mochyy.mochymusic.shared.resources.proxy_check_failed
import com.mochyy.mochymusic.shared.resources.proxy_check_login
import com.mochyy.mochymusic.shared.resources.proxy_check_no_answer
import com.mochyy.mochymusic.shared.resources.proxy_check_not_found
import com.mochyy.mochymusic.shared.resources.proxy_check_ok
import com.mochyy.mochymusic.shared.resources.proxy_checking
import com.mochyy.mochymusic.shared.resources.proxy_intro
import com.mochyy.mochymusic.shared.resources.proxy_login
import com.mochyy.mochymusic.shared.resources.proxy_login_clear
import com.mochyy.mochymusic.shared.resources.proxy_login_none
import com.mochyy.mochymusic.shared.resources.proxy_login_set
import com.mochyy.mochymusic.shared.resources.proxy_password
import com.mochyy.mochymusic.shared.resources.proxy_type
import com.mochyy.mochymusic.shared.resources.proxy_type_http
import com.mochyy.mochymusic.shared.resources.proxy_type_socks
import com.mochyy.mochymusic.shared.resources.proxy_use
import com.mochyy.mochymusic.shared.resources.proxy_use_desc
import com.mochyy.mochymusic.shared.resources.proxy_use_needs_address
import com.mochyy.mochymusic.shared.resources.proxy_username
import com.mochyy.mochymusic.shared.resources.wifi_proxy
import com.mochyy.mochymusic.ui.component.AppSwitch
import com.mochyy.mochymusic.ui.component.DefaultDialog
import com.mochyy.mochymusic.ui.component.SettingsGroup
import com.mochyy.mochymusic.ui.component.SettingsGroupStyle
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.component.TextFieldDialog
import com.mochyy.mochymusic.ui.component.platformCancelLabel
import com.mochyy.mochymusic.ui.component.platformOkLabel
import com.mochyy.mochymusic.utils.ProxyAddress
import com.mochyy.mochymusic.utils.ProxyCheckResult
import com.mochyy.mochymusic.utils.ProxyFailure
import com.mochyy.mochymusic.utils.ProxyTester
import com.mochyy.mochymusic.utils.ProxyType
import com.mochyy.mochymusic.utils.parseProxyAddress
import com.mochyy.mochymusic.utils.proxyAddressLabel
import com.mochyy.mochymusic.utils.rememberPreference
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// the route the app behaviour row opens
const val ProxySettingsRoute = "proxy_settings"

private enum class ProxyDialog { Address, Type, Login }

private sealed interface ProxyCheck {
    data object Idle : ProxyCheck
    data object Running : ProxyCheck
    data class Passed(val millis: Long) : ProxyCheck
    data class Failed(val reason: ProxyFailure) : ProxyCheck
}

// the stored settings read as one editable proxy, so a change to any of them makes an old check stale
private data class ProxyTarget(
    val type: ProxyType,
    val address: ProxyAddress,
    val username: String,
    val password: String,
)

// settings > app behaviour > proxy: youtube through another server, for blocked networks and countries youtube music skips
@Composable
fun ProxySettingsScreen(navController: NavController, tester: ProxyTester? = null) {
    var enabled by rememberPreference(ProxyEnabledKey, defaultValue = false)
    var address by rememberPreference(ProxyUrlKey, defaultValue = "")
    var typeName by rememberPreference(ProxyTypeKey, defaultValue = ProxyType.HTTP.name)
    var username by rememberPreference(ProxyUsernameKey, defaultValue = "")
    var password by rememberPreference(ProxyPasswordKey, defaultValue = "")

    val type = typeName.toEnum(defaultValue = ProxyType.HTTP)
    val config = remember(type, address, username, password) {
        parseProxyAddress(address)?.let { ProxyTarget(type, it, username, password) }
    }
    var dialog by remember { mutableStateOf<ProxyDialog?>(null) }
    var check by remember { mutableStateOf<ProxyCheck>(ProxyCheck.Idle) }
    val scope = rememberCoroutineScope()

    // a changed setting makes the last check's answer stale
    LaunchedEffect(config) { check = ProxyCheck.Idle }

    SubSettingsScaffold(
        title = stringResource(Res.string.proxy),
        onBack = { navController.navigateUp() },
    ) {
        com.mochyy.mochymusic.ui.component.PagePreview(com.mochyy.mochymusic.ui.component.SettingsPreviewPage.Proxy)
        Text(
            text = stringResource(Res.string.proxy_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 14.dp),
        )

        SettingsGroup(
            style = SettingsGroupStyle.Grouped,
            items = listOf(
                SettingsItem(
                    title = { Text(stringResource(Res.string.proxy_use)) },
                    descriptionText = stringResource(if (config == null) Res.string.proxy_use_needs_address else Res.string.proxy_use_desc),
                    icon = painterResource(Res.drawable.wifi_proxy),
                    iconShape = CircleShape,
                    enabled = config != null,
                    onClick = { enabled = !enabled },
                    trailingContent = {
                        AppSwitch(checked = enabled && config != null, onCheckedChange = { enabled = it }, enabled = config != null)
                    },
                ),
                SettingsItem(
                    title = { Text(stringResource(Res.string.proxy_address)) },
                    descriptionText = config?.let { proxyAddressLabel(it.address.host, it.address.port) } ?: stringResource(Res.string.proxy_address_not_set),
                    icon = painterResource(Res.drawable.link),
                    iconShape = CircleShape,
                    onClick = { dialog = ProxyDialog.Address },
                ),
                SettingsItem(
                    title = { Text(stringResource(Res.string.proxy_type)) },
                    descriptionText = stringResource(type.labelRes()),
                    icon = painterResource(Res.drawable.network_node),
                    iconShape = CircleShape,
                    onClick = { dialog = ProxyDialog.Type },
                ),
                SettingsItem(
                    title = { Text(stringResource(Res.string.proxy_login)) },
                    descriptionText = if (username.isEmpty()) stringResource(Res.string.proxy_login_none) else stringResource(Res.string.proxy_login_set, username),
                    icon = painterResource(Res.drawable.key),
                    iconShape = CircleShape,
                    onClick = { dialog = ProxyDialog.Login },
                ),
            ),
        )

        // the live check needs the platform's connect path, so it's a row Android has and the plain shared UI doesn't
        if (tester != null) {
            val runCheck = tester
            Spacer(Modifier.height(18.dp))

            SettingsGroup(
                style = SettingsGroupStyle.Grouped,
                items = listOf(
                    SettingsItem(
                        title = { Text(stringResource(if (check == ProxyCheck.Running) Res.string.proxy_checking else Res.string.proxy_check)) },
                        descriptionText = when (val result = check) {
                            is ProxyCheck.Passed -> stringResource(Res.string.proxy_check_ok, result.millis.toInt())
                            is ProxyCheck.Failed -> stringResource(reasonRes(result.reason))
                            else -> stringResource(Res.string.proxy_check_desc)
                        },
                        icon = painterResource(Res.drawable.network_update),
                        iconShape = CircleShape,
                        enabled = config != null && check != ProxyCheck.Running,
                        onClick = {
                            val target = config
                            if (target != null) {
                                check = ProxyCheck.Running
                                scope.launch {
                                    check = when (val result = runCheck.test(target.type, target.address, target.username, target.password)) {
                                        is ProxyCheckResult.Passed -> ProxyCheck.Passed(result.millis)
                                        is ProxyCheckResult.Failed -> ProxyCheck.Failed(result.reason)
                                    }
                                }
                            }
                        },
                    ),
                ),
            )
        }

        Spacer(Modifier.height(140.dp))
    }

    when (dialog) {
        ProxyDialog.Address -> AddressDialog(
            current = config?.let { proxyAddressLabel(it.address.host, it.address.port) }.orEmpty(),
            onDone = { text ->
                address = parseProxyAddress(text)?.let { proxyAddressLabel(it.host, it.port) }.orEmpty()
                // a pasted link names its own type
                when {
                    text.trim().startsWith("socks", ignoreCase = true) -> typeName = ProxyType.SOCKS.name
                    text.trim().startsWith("http", ignoreCase = true) -> typeName = ProxyType.HTTP.name
                }
                if (address.isEmpty()) enabled = false
            },
            onDismiss = { dialog = null },
        )
        ProxyDialog.Type -> TypeDialog(
            selected = type,
            onSelect = {
                typeName = it.name
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        ProxyDialog.Login -> LoginDialog(
            username = username,
            password = password,
            onDone = { newUsername, newPassword ->
                username = newUsername.trim()
                password = newPassword
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun AddressDialog(current: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    TextFieldDialog(
        title = { Text(stringResource(Res.string.proxy_address)) },
        initialTextFieldValue = TextFieldValue(current, TextRange(current.length)),
        placeholder = { Text(stringResource(Res.string.proxy_address_hint)) },
        // empty clears the proxy, anything else has to read as host:port
        isInputValid = { it.isBlank() || parseProxyAddress(it) != null },
        keyboardType = KeyboardType.Uri,
        onDone = onDone,
        onDismiss = onDismiss,
    )
}

@Composable
private fun TypeDialog(selected: ProxyType, onSelect: (ProxyType) -> Unit, onDismiss: () -> Unit) {
    DefaultDialog(
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.Start,
        title = { Text(stringResource(Res.string.proxy_type)) },
    ) {
        listOf(ProxyType.HTTP, ProxyType.SOCKS).forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(selected = option == selected, onClick = { onSelect(option) })
                Text(stringResource(option.labelRes()), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun LoginDialog(
    username: String,
    password: String,
    onDone: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newUsername by remember { mutableStateOf(username) }
    var newPassword by remember { mutableStateOf(password) }
    DefaultDialog(
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.Start,
        title = { Text(stringResource(Res.string.proxy_login)) },
        buttons = {
            TextButton(onClick = { onDone("", "") }) { Text(stringResource(Res.string.proxy_login_clear)) }
            TextButton(onClick = onDismiss) { Text(platformCancelLabel()) }
            TextButton(onClick = { onDone(newUsername, newPassword) }) { Text(platformOkLabel()) }
        },
    ) {
        OutlinedTextField(
            value = newUsername,
            onValueChange = { newUsername = it },
            singleLine = true,
            label = { Text(stringResource(Res.string.proxy_username)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            singleLine = true,
            label = { Text(stringResource(Res.string.proxy_password)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun ProxyType.labelRes(): StringResource =
    if (this == ProxyType.SOCKS) Res.string.proxy_type_socks else Res.string.proxy_type_http

// the line that says what to fix, from what the check ran into
private fun reasonRes(reason: ProxyFailure): StringResource = when (reason) {
    ProxyFailure.HostNotFound -> Res.string.proxy_check_not_found
    ProxyFailure.LoginRequired -> Res.string.proxy_check_login
    ProxyFailure.NoAnswer -> Res.string.proxy_check_no_answer
    ProxyFailure.Failed -> Res.string.proxy_check_failed
}
