package de.zettelgeist.app.ai

class LlmManager(private val keyStore: KeyStore) {

    fun isAvailable(): Boolean = keyStore.hasValidConfig()

    fun getSelectedProvider(): LlmProvider? = keyStore.getSelectedProvider()

    fun getService(): LlmService? {
        val provider = keyStore.getSelectedProvider() ?: return null
        val apiKey = keyStore.getApiKey(provider) ?: return null
        if (apiKey.isBlank()) return null

        return when (provider) {
            LlmProvider.OPENAI -> OpenAiAdapter(apiKey)
            LlmProvider.GEMINI -> GeminiAdapter(apiKey)
            LlmProvider.ANTHROPIC -> AnthropicAdapter(apiKey)
        }
    }

    fun saveConfig(provider: LlmProvider?, apiKey: String?) {
        keyStore.saveSelectedProvider(provider)
        if (provider != null && !apiKey.isNullOrBlank()) {
            keyStore.saveApiKey(provider, apiKey)
        }
    }

    fun getApiKey(provider: LlmProvider): String? = keyStore.getApiKey(provider)

    fun clearConfig() {
        keyStore.saveSelectedProvider(null)
    }
}
