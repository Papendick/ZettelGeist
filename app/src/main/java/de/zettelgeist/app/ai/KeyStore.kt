package de.zettelgeist.app.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class KeyStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "zettelgeist_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(provider: LlmProvider, key: String) {
        prefs.edit().putString("api_key_${provider.name}", key).apply()
    }

    fun getApiKey(provider: LlmProvider): String? {
        return prefs.getString("api_key_${provider.name}", null)
    }

    fun deleteApiKey(provider: LlmProvider) {
        prefs.edit().remove("api_key_${provider.name}").apply()
    }

    fun saveSelectedProvider(provider: LlmProvider?) {
        if (provider == null) {
            prefs.edit().remove("selected_provider").apply()
        } else {
            prefs.edit().putString("selected_provider", provider.name).apply()
        }
    }

    fun getSelectedProvider(): LlmProvider? {
        val name = prefs.getString("selected_provider", null) ?: return null
        return try {
            LlmProvider.valueOf(name)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun hasValidConfig(): Boolean {
        val provider = getSelectedProvider() ?: return false
        val key = getApiKey(provider)
        return !key.isNullOrBlank()
    }
}
