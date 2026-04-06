package de.zettelgeist.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.zettelgeist.app.ai.KeyStore
import de.zettelgeist.app.ai.LlmManager
import de.zettelgeist.app.ai.LlmProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val keyStore = KeyStore(application)
    private val llmManager = LlmManager(keyStore)

    private val _selectedProvider = MutableStateFlow(keyStore.getSelectedProvider())
    val selectedProvider: StateFlow<LlmProvider?> = _selectedProvider

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Unknown)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting

    init {
        val provider = keyStore.getSelectedProvider()
        if (provider != null) {
            _apiKey.value = keyStore.getApiKey(provider) ?: ""
        }
    }

    fun selectProvider(provider: LlmProvider?) {
        _selectedProvider.value = provider
        _connectionStatus.value = ConnectionStatus.Unknown
        if (provider != null) {
            _apiKey.value = keyStore.getApiKey(provider) ?: ""
        } else {
            _apiKey.value = ""
        }
        llmManager.saveConfig(provider, if (provider != null) _apiKey.value.ifBlank { null } else null)
    }

    fun updateApiKey(key: String) {
        _apiKey.value = key
        _connectionStatus.value = ConnectionStatus.Unknown
        val provider = _selectedProvider.value ?: return
        llmManager.saveConfig(provider, key.ifBlank { null })
    }

    fun testConnection() {
        val service = llmManager.getService() ?: run {
            _connectionStatus.value = ConnectionStatus.Error("Kein API-Key konfiguriert")
            return
        }
        _isTesting.value = true
        _connectionStatus.value = ConnectionStatus.Testing
        viewModelScope.launch {
            val result = service.testConnection()
            result.onSuccess {
                _connectionStatus.value = ConnectionStatus.Connected
            }
            result.onFailure {
                _connectionStatus.value = ConnectionStatus.Error(it.message ?: "Unbekannter Fehler")
            }
            _isTesting.value = false
        }
    }

    fun getModelName(): String {
        return _selectedProvider.value?.defaultModel ?: ""
    }
}

sealed class ConnectionStatus {
    data object Unknown : ConnectionStatus()
    data object Testing : ConnectionStatus()
    data object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}
