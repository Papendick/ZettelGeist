package de.zettelgeist.app.ai

enum class LlmProvider(
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String
) {
    OPENAI(
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini"
    ),
    GEMINI(
        displayName = "Google Gemini",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
        defaultModel = "gemini-2.5-flash"
    ),
    ANTHROPIC(
        displayName = "Anthropic Claude",
        baseUrl = "https://api.anthropic.com/v1/messages",
        defaultModel = "claude-sonnet-4-20250514"
    )
}
