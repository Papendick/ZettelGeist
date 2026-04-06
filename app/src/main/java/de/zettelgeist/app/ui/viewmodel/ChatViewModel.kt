package de.zettelgeist.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.zettelgeist.app.ai.KeyStore
import de.zettelgeist.app.ai.LlmManager
import de.zettelgeist.app.data.db.AppDatabase
import de.zettelgeist.app.data.model.ChatMessage
import de.zettelgeist.app.data.model.Conversation
import de.zettelgeist.app.data.repository.ChatRepository
import de.zettelgeist.app.data.repository.NoteRepository
import de.zettelgeist.app.data.vault.VaultManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val vault = VaultManager(application)
    private val chatRepo = ChatRepository(db)
    private val noteRepo = NoteRepository(db, vault)
    private val llmManager = LlmManager(KeyStore(application))

    val conversations = chatRepo.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId: StateFlow<String?> = _currentConversationId

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val isLlmAvailable: Boolean get() = llmManager.isAvailable()

    private var messagesCollectionJob: kotlinx.coroutines.Job? = null

    fun startNewConversation() {
        _currentConversationId.value = null
        _messages.value = emptyList()
        _error.value = null
        messagesCollectionJob?.cancel()
    }

    fun loadConversation(conversationId: String) {
        _currentConversationId.value = conversationId
        messagesCollectionJob?.cancel()
        messagesCollectionJob = viewModelScope.launch {
            chatRepo.getMessagesForConversation(conversationId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(userMessage: String) {
        val service = llmManager.getService() ?: return
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Create conversation if needed
                val convId = _currentConversationId.value
                    ?: chatRepo.createConversation(userMessage).also {
                        _currentConversationId.value = it.id
                        loadConversation(it.id)
                    }.id

                // Save user message
                chatRepo.addMessage(convId, "user", userMessage)

                // Search for relevant notes
                val relevantNotes = noteRepo.searchNotes(userMessage).take(10)
                val contextNoteIds = Json.encodeToString(relevantNotes.map { it.id })

                // Build context
                val notesContext = relevantNotes.joinToString("\n---\n") { note ->
                    val content = noteRepo.readNoteContent(note.filePath)
                    val tags = noteRepo.getTagsForNote(note.id).joinToString(", ") { it.name }
                    "Titel: ${note.title}\nWorkspace: ${note.workspace}\nTags: $tags\nInhalt:\n$content"
                }

                val systemPrompt = """Du bist ein Wissensassistent. Der Nutzer hat einen persönlichen Zettelkasten mit Notizen.
Hier sind die relevantesten Notizen zum Kontext:

---
$notesContext
---

Beantworte die Frage des Nutzers basierend auf seinen Notizen.
- Beziehe dich konkret auf Notizen (nenne Titel)
- Wenn du keine relevanten Notizen findest, sage das ehrlich
- Antworte auf Deutsch
- Halte die Antwort kompakt und hilfreich"""

                // Build conversation history
                val recentMessages = chatRepo.getRecentMessages(convId, 10).reversed()
                val historyText = recentMessages.joinToString("\n") { msg ->
                    "${if (msg.role == "user") "Nutzer" else "Assistent"}: ${msg.content}"
                }

                val fullUserMessage = if (historyText.isNotBlank()) {
                    "Bisheriger Verlauf:\n$historyText\n\nAktuelle Frage: $userMessage"
                } else {
                    userMessage
                }

                val result = service.complete(systemPrompt, fullUserMessage, 2048)
                result.onSuccess { response ->
                    chatRepo.addMessage(convId, "assistant", response, contextNoteIds)
                }
                result.onFailure { e ->
                    _error.value = e.message ?: "Unbekannter Fehler"
                }
            } catch (e: Exception) {
                _error.value = "Fehler: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            chatRepo.deleteConversation(id)
            if (_currentConversationId.value == id) {
                startNewConversation()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
