package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// fake keys only
class AiTranslatorConfigTest {
    private fun resolve(
        provider: String,
        openRouterKey: String = "fake-openrouter-key",
        customBaseUrl: String = "",
        customKey: String = "",
        customModel: String = AiTranslatorConfig.ModelUnset,
        legacyBaseUrl: String = "",
        deeplKey: String = "fake-deepl-key",
    ) = AiTranslatorConfig.resolve(
        provider = provider,
        openRouterKey = openRouterKey,
        openRouterModel = "or-model",
        customBaseUrl = customBaseUrl,
        customKey = customKey,
        customModel = customModel,
        legacyBaseUrl = legacyBaseUrl,
        deeplKey = deeplKey,
    )

    @Test
    fun openRouterAlwaysSendsItsKeyToOpenRouter() {
        val config = resolve("OpenRouter", legacyBaseUrl = "https://example.invalid/v1", customBaseUrl = "https://custom.invalid/v1")
        assertEquals(OpenRouterEndpoint, config.baseUrl)
        assertEquals("fake-openrouter-key", config.apiKey)
    }

    @Test
    fun customNeverUsesTheOpenRouterKey() {
        val config = resolve("Custom", customBaseUrl = "https://custom.invalid/v1")
        assertFalse(config.usable)
        assertEquals("", config.apiKey)
    }

    @Test
    fun customSendsItsOwnKeyToItsOwnAddress() {
        val config = resolve("Custom", customBaseUrl = "https://custom.invalid/v1", customKey = "fake-custom-key")
        assertEquals("https://custom.invalid/v1", config.baseUrl)
        assertEquals("fake-custom-key", config.apiKey)
    }

    @Test
    fun customWithNoAddressHasNoKey() {
        assertFalse(resolve("Custom", customKey = "fake-custom-key").usable)
    }

    @Test
    fun theOldSharedAddressBecomesCustomsAddressOnly() {
        val custom = resolve("Custom", legacyBaseUrl = "https://old.invalid/v1", customKey = "fake-custom-key")
        assertEquals("https://old.invalid/v1", custom.baseUrl)
        // the model comes over by migrateLegacyCustomModel, once, not by reading OpenRouter's live value
        assertEquals("", custom.model)
        assertEquals(OpenRouterEndpoint, resolve("OpenRouter", legacyBaseUrl = "https://old.invalid/v1").baseUrl)
    }

    @Test
    fun aClearedCustomModelStaysBlank() {
        val config = resolve("Custom", customBaseUrl = "https://custom.invalid/v1", customKey = "fake-custom-key", customModel = "")
        assertEquals("", config.model)
    }

    @Test
    fun customNeverTakesOpenRoutersOwnModel() {
        // an OpenRouter-only user (no old shared address) switching to Custom starts with no model
        val config = resolve("Custom", customBaseUrl = "https://custom.invalid/v1", customKey = "fake-custom-key")
        assertEquals("", config.model)
    }

    @Test
    fun aCustomSetupsOldModelIsCopiedOnceAndThenStaysPut() {
        val prefs = mutablePreferencesOf(
            OpenRouterBaseUrlKey to "https://old.invalid/v1",
            OpenRouterModelKey to "old-model",
        )
        AiTranslatorConfig.migrateLegacyCustomModel(prefs)
        assertEquals("old-model", prefs[CustomAiModelKey])
        prefs[OpenRouterModelKey] = "new-openrouter-model"
        AiTranslatorConfig.migrateLegacyCustomModel(prefs)
        assertEquals("old-model", prefs[CustomAiModelKey])
        assertEquals("old-model", AiTranslatorConfig.customModel(prefs[CustomAiModelKey]!!))
    }

    @Test
    fun anOpenRouterOnlySetupMigratesNothing() {
        val prefs = mutablePreferencesOf(OpenRouterModelKey to "or-model")
        AiTranslatorConfig.migrateLegacyCustomModel(prefs)
        assertEquals(null, prefs[CustomAiModelKey])
        assertEquals("", AiTranslatorConfig.customModel(AiTranslatorConfig.ModelUnset))
    }

    @Test
    fun aSavedCustomModelIsNeverOverwritten() {
        val prefs = mutablePreferencesOf(
            OpenRouterBaseUrlKey to "https://old.invalid/v1",
            OpenRouterModelKey to "old-model",
            CustomAiModelKey to "",
        )
        AiTranslatorConfig.migrateLegacyCustomModel(prefs)
        assertEquals("", prefs[CustomAiModelKey])
    }

    @Test
    fun deepLUsesOnlyItsOwnKey() {
        assertEquals("fake-deepl-key", resolve("DeepL").apiKey)
        assertFalse(resolve("DeepL", deeplKey = "").usable)
    }

    @Test
    fun anUnofferedProviderGetsNoKey() {
        assertFalse(resolve("Mistral").usable)
    }

    @Test
    fun theKeyIsNeverInToString() {
        val text = resolve("OpenRouter").toString()
        assertFalse("fake-openrouter-key" in text)
        assertTrue("hasKey=true" in text)
    }
}
