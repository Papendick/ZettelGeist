package de.zettelgeist.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiAdapter(private val apiKey: String) : LlmService {

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
            val url = LlmProvider.GEMINI.baseUrl
                .replace("{model}", LlmProvider.GEMINI.defaultModel) + "?key=$apiKey"

            val body = buildJsonObject {
                putJsonArray("contents") {
                    addJsonObject {
                        putJsonArray("parts") {
                            addJsonObject {
                                put("text", "$systemPrompt\n\n$userMessage")
                            }
                        }
                    }
                }
                putJsonObject("generationConfig") {
                    put("maxOutputTokens", maxTokens)
                }
            }

            val request = Request.Builder()
                .url(url)
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
            val content = json["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")?.jsonPrimitive?.content
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
