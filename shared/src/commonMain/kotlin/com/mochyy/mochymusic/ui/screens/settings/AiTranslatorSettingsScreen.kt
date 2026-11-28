// AiTranslatorSettingsScreen.kt

package com.mochyy.mochymusic.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mochyy.mochymusic.constants.AiTranslatorConfig
import com.mochyy.mochymusic.constants.CustomAiModelKey
import com.mochyy.mochymusic.constants.CustomAiBaseUrlKey
import com.mochyy.mochymusic.constants.CustomAiApiKey
import com.mochyy.mochymusic.constants.AiProviderKey
import com.mochyy.mochymusic.constants.DeeplApiKey
import com.mochyy.mochymusic.constants.OpenRouterApiKey
import com.mochyy.mochymusic.constants.OpenRouterBaseUrlKey
import com.mochyy.mochymusic.constants.OpenRouterModelKey
import com.mochyy.mochymusic.constants.TranslateLanguageKey
import com.mochyy.mochymusic.constants.TranslateLyricsKey
import com.mochyy.mochymusic.shared.resources.Res
import com.mochyy.mochymusic.shared.resources.ai_translator_endpoint
import com.mochyy.mochymusic.shared.resources.ai_translator_endpoint_hint
import com.mochyy.mochymusic.shared.resources.ai_translator_key
import com.mochyy.mochymusic.shared.resources.ai_translator_key_clear
import com.mochyy.mochymusic.shared.resources.ai_translator_key_empty
import com.mochyy.mochymusic.shared.resources.ai_translator_key_saved
import com.mochyy.mochymusic.shared.resources.ai_translator_model
import com.mochyy.mochymusic.shared.resources.ai_translator_model_default
import com.mochyy.mochymusic.shared.resources.ai_translator_provider
import com.mochyy.mochymusic.shared.resources.ai_translator_provider_custom
import com.mochyy.mochymusic.shared.resources.ai_translator_provider_deepl
import com.mochyy.mochymusic.shared.resources.ai_translator_provider_openrouter
import com.mochyy.mochymusic.shared.resources.ai_translator_target
import com.mochyy.mochymusic.shared.resources.key
import com.mochyy.mochymusic.shared.resources.link
import com.mochyy.mochymusic.shared.resources.lyrics
import com.mochyy.mochymusic.shared.resources.settings_ai_translator
import com.mochyy.mochymusic.shared.resources.settings_translate_auto
import com.mochyy.mochymusic.shared.resources.settings_translate_auto_desc
import com.mochyy.mochymusic.shared.resources.translate
import com.mochyy.mochymusic.ui.component.DefaultDialog
import com.mochyy.mochymusic.ui.component.SettingsItem
import com.mochyy.mochymusic.ui.component.SubSettingsScaffold
import com.mochyy.mochymusic.ui.component.TextFieldDialog
import com.mochyy.mochymusic.ui.component.platformCancelLabel
import com.mochyy.mochymusic.ui.component.platformOkLabel
import com.mochyy.mochymusic.utils.rememberPreference
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// the route the lyrics section's AI translator row opens
const val AiTranslatorSettingsRoute = "ai_translator_settings"

// the values AiProviderKey takes; the lyrics translator reads them
private const val ProviderOpenRouter = "OpenRouter"
private const val ProviderCustom = "Custom"
private const val ProviderDeepL = "DeepL"

private enum class AiTranslatorDialog { Provider, Endpoint, Model, Key }

// settings > now playing > lyrics > ai translator: which service translates lyrics, and its endpoint, model and key
@Composable
fun AiTranslatorSettingsScreen(
    navController: NavController,
    // the app's translation languages and its full-language picker: both lean on Android-only sheets
    targets: List<Pair<String, String>>,
    languagePicker: @Composable (title: String, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) -> Unit,
) {
    val (provider, setProvider) = rememberPreference(AiProviderKey, defaultValue = ProviderOpenRouter)
    val (legacyEndpoint) = rememberPreference(OpenRouterBaseUrlKey, defaultValue = "")
    val (customEndpoint, setCustomEndpoint) = rememberPreference(CustomAiBaseUrlKey, defaultValue = "")
    val (openRouterModel, setOpenRouterModel) = rememberPreference(OpenRouterModelKey, defaultValue = "")
    val (customModel, setCustomModel) = rememberPreference(CustomAiModelKey, defaultValue = AiTranslatorConfig.ModelUnset)
    val (openRouterKey, setOpenRouterKey) = rememberPreference(OpenRouterApiKey, defaultValue = "")
    val (customKey, setCustomKey) = rememberPreference(CustomAiApiKey, defaultValue = "")
    val (deeplKey, setDeeplKey) = rememberPreference(DeeplApiKey, defaultValue = "")
    val (targetLanguage, setTargetLanguage) = rememberPreference(TranslateLanguageKey, defaultValue = "en")
    val (translateAutomatically, setTranslateAutomatically) = rememberPreference(TranslateLyricsKey, defaultValue = false)

    val isDeepL = provider == ProviderDeepL
    val isCustom = provider == ProviderCustom
    // every provider keeps its own key, model and address (AiTranslatorConfig decides what a request uses);
    // the address the two used to share only ever counts as Custom's
    val endpoint = customEndpoint.ifBlank { legacyEndpoint }
    val model = if (isCustom) AiTranslatorConfig.customModel(customModel) else openRouterModel
    val apiKey = when {
        isDeepL -> deeplKey
        isCustom -> customKey
        else -> openRouterKey
    }
    val targetName = targets.firstOrNull { it.first == targetLanguage }?.second ?: targetLanguage

    var dialog by remember { mutableStateOf<AiTranslatorDialog?>(null) }
    var pickingLanguage by remember { mutableStateOf(false) }

    SubSettingsScaffold(
        title = stringResource(Res.string.settings_ai_translator),
        onBack = { navController.navigateUp() },
    ) {
        val items = buildList {
            add(
                SettingsItem(
                    title = { Text(stringResource(Res.string.ai_translator_provider)) },
                    descriptionText = stringResource(providerLabelRes(provider)),
                    icon = painterResource(Res.drawable.translate),
                    onClick = { dialog = AiTranslatorDialog.Provider },
                ),
            )
            // openrouter and deepl send to their own addresses; only a custom translator has one to set
            if (isCustom) {
                add(
                    SettingsItem(
                        title = { Text(stringResource(Res.string.ai_translator_endpoint)) },
                        descriptionText = endpoint.ifBlank { stringResource(Res.string.ai_translator_endpoint_hint) },
                        icon = painterResource(Res.drawable.link),
                        onClick = { dialog = AiTranslatorDialog.Endpoint },
                    ),
                )
            }
            if (!isDeepL) {
                add(
                    SettingsItem(
                        title = { Text(stringResource(Res.string.ai_translator_model)) },
                        descriptionText = model.ifBlank { stringResource(Res.string.ai_translator_model_default) },
                        icon = painterResource(Res.drawable.lyrics),
                        onClick = { dialog = AiTranslatorDialog.Model },
                    ),
                )
            }
            add(
                SettingsItem(
                    title = { Text(stringResource(Res.string.ai_translator_key)) },
                    // the key is never shown, only whether one is saved
                    descriptionText = stringResource(if (apiKey.isBlank()) Res.string.ai_translator_key_empty else Res.string.ai_translator_key_saved),
                    icon = painterResource(Res.drawable.key),
                    onClick = { dialog = AiTranslatorDialog.Key },
                ),
            )
            add(
                SettingsItem(
                    title = { Text(stringResource(Res.string.ai_translator_target)) },
                    descriptionText = targetName,
                    icon = painterResource(Res.drawable.translate),
                    onClick = { pickingLanguage = true },
                ),
            )
            add(
                switchItem(
                    key = "Translate lyrics automatically",
                    title = stringResource(Res.string.settings_translate_auto),
                    description = stringResource(Res.string.settings_translate_auto_desc),
                    icon = painterResource(Res.drawable.translate),
                    checked = translateAutomatically,
                    onCheckedChange = setTranslateAutomatically,
                ),
            )
        }
        SettingsSection(null, items)
        Spacer(Modifier.height(140.dp))
    }

    when (dialog) {
        AiTranslatorDialog.Provider -> ProviderDialog(
            selected = provider,
            onSelect = {
                setProvider(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        AiTranslatorDialog.Endpoint -> TextFieldDialog(
            title = { Text(stringResource(Res.string.ai_translator_endpoint)) },
            initialTextFieldValue = TextFieldValue(endpoint, TextRange(endpoint.length)),
            placeholder = { Text(stringResource(Res.string.ai_translator_endpoint_hint)) },
            // https only, so the key never travels in clear; there's no default to fall back to
            isInputValid = { it.trim().startsWith("https://") },
            keyboardType = KeyboardType.Uri,
            onDone = {
                setCustomEndpoint(it.trim())
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        AiTranslatorDialog.Model -> TextFieldDialog(
            title = { Text(stringResource(Res.string.ai_translator_model)) },
            initialTextFieldValue = TextFieldValue(model, TextRange(model.length)),
            placeholder = { Text(stringResource(Res.string.ai_translator_model_default)) },
            // blank lets the provider pick its own model
            isInputValid = { true },
            onDone = {
                if (isCustom) setCustomModel(it.trim()) else setOpenRouterModel(it.trim())
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        AiTranslatorDialog.Key -> KeyDialog(
            current = apiKey,
            onDone = { value ->
                when {
                    isDeepL -> setDeeplKey(value.trim())
                    isCustom -> setCustomKey(value.trim())
                    else -> setOpenRouterKey(value.trim())
                }
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }

    if (pickingLanguage) {
        languagePicker(
            stringResource(Res.string.ai_translator_target),
            targetLanguage,
            { setTargetLanguage(it) },
            { pickingLanguage = false },
        )
    }
}

private fun providerLabelRes(provider: String): StringResource = when (provider) {
    ProviderCustom -> Res.string.ai_translator_provider_custom
    ProviderDeepL -> Res.string.ai_translator_provider_deepl
    else -> Res.string.ai_translator_provider_openrouter
}

@Composable
private fun ProviderDialog(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    DefaultDialog(
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.Start,
        title = { Text(stringResource(Res.string.ai_translator_provider)) },
    ) {
        listOf(ProviderOpenRouter, ProviderCustom, ProviderDeepL).forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(selected = option == selected, onClick = { onSelect(option) })
                Text(stringResource(providerLabelRes(option)), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun KeyDialog(current: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(current) }
    DefaultDialog(
        onDismiss = onDismiss,
        horizontalAlignment = Alignment.Start,
        title = { Text(stringResource(Res.string.ai_translator_key)) },
        buttons = {
            TextButton(onClick = { onDone("") }) { Text(stringResource(Res.string.ai_translator_key_clear)) }
            TextButton(onClick = onDismiss) { Text(platformCancelLabel()) }
            TextButton(onClick = { onDone(value) }) { Text(platformOkLabel()) }
        },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            label = { Text(stringResource(Res.string.ai_translator_key)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
