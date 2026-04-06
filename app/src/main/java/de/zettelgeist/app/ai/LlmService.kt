package de.zettelgeist.app.ai

interface LlmService {
    suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        maxTokens: Int = 1024
    ): Result<String>

    suspend fun testConnection(): Result<Boolean>
}
