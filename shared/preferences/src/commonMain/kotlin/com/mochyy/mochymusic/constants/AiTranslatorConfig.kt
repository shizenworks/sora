package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.MutablePreferences

/** OpenRouter's own chat completions address; the OpenRouter choice always sends here. */
const val OpenRouterEndpoint = "https://openrouter.ai/api/v1/chat/completions"

/**
 * Where a lyrics translation goes and with which key, decided in one place from the chosen provider,
 * so the provider shown in settings and the server a key is sent to can't disagree. An unusable
 * choice (no key, or Custom with no address) has a blank [apiKey], and the caller then uses the
 * keyless translator instead.
 */
data class AiTranslatorConfig(val provider: String, val apiKey: String, val baseUrl: String, val model: String) {
    val usable: Boolean get() = apiKey.isNotBlank()

    // the key never appears in logs or crash reports
    override fun toString() = "AiTranslatorConfig(provider=$provider, baseUrl=$baseUrl, model=$model, hasKey=$usable)"

    companion object {
        const val OpenRouter = "OpenRouter"
        const val Custom = "Custom"
        const val DeepL = "DeepL"

        /** What a never-saved Custom model reads as, so a saved blank ("let the provider pick") stays blank. */
        const val ModelUnset = "\u0000unset"

        /** Custom's model as settings show it and requests use it: the saved value, blank included, else none. */
        fun customModel(saved: String): String = if (saved == ModelUnset) "" else saved

        /**
         * Run once at startup. The model field OpenRouter and Custom used to share is copied into Custom's own
         * model, once, when it belonged to a Custom setup (the old shared address was set). From then on
         * Custom's model is its own, so changing OpenRouter's model never changes what Custom sends.
         */
        fun migrateLegacyCustomModel(prefs: MutablePreferences) {
            if (prefs[CustomAiModelKey] != null) return
            if (prefs[OpenRouterBaseUrlKey].isNullOrBlank()) return
            prefs[CustomAiModelKey] = prefs[OpenRouterModelKey].orEmpty()
        }

        /**
         * [legacyBaseUrl] is the address field OpenRouter and Custom used to share. It is only ever
         * used as Custom's address (that's what it was for), never for OpenRouter, and no key comes
         * with it: a Custom translator set up before keys were split asks for its key again.
         */
        fun resolve(
            provider: String,
            openRouterKey: String,
            openRouterModel: String,
            customBaseUrl: String,
            customKey: String,
            customModel: String,
            legacyBaseUrl: String,
            deeplKey: String,
        ): AiTranslatorConfig = when (provider) {
            OpenRouter -> AiTranslatorConfig(provider, openRouterKey.trim(), OpenRouterEndpoint, openRouterModel)
            Custom -> {
                val url = customBaseUrl.ifBlank { legacyBaseUrl }.trim()
                // no address means nowhere safe to send the key
                AiTranslatorConfig(provider, if (url.isBlank()) "" else customKey.trim(), url, customModel(customModel))
            }
            DeepL -> AiTranslatorConfig(provider, deeplKey.trim(), "", "")
            // a provider no longer offered gets no key, rather than borrowing another provider's
            else -> AiTranslatorConfig(provider, "", "", "")
        }
    }
}
