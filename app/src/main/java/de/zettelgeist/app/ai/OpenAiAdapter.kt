package de.zettelgeist.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAiAdapter(private val apiKey: String) : LlmService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun complete(
        systemPrompt: String,
        userMessage: String,
        maxTokens: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("model", LlmProvider.OPENAI.defaultModel)
                put("max_tokens", maxTokens)
                putJsonArray("messages") {
                    addJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    }
                    addJsonObject {
                        put("role", "user")
                        put("content", userMessage)
                    }
                }
            }

            val request = Request.Builder()
                .url(LlmProvider.OPENAI.baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext Result.failure(
                Exception("Leere Antwort vom Server")
            )

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("API-Fehler (${response.code}): $responseBody")
                )
            }

            val json = Json.parseToJsonElement(responseBody).jsonObject
            val content = json["choices"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
                ?: return@withContext Result.failure(Exception("Unerwartetes Antwortformat"))

            Result.success(content)
        } catch (e: Exception) {
            Result.failure(Exception("Verbindungsfehler: ${e.message}"))
        }
    }

    override suspend fun testConnection(): Result<Boolean> {
        return complete("Say OK", "test", 5).map { true }
    }
}
