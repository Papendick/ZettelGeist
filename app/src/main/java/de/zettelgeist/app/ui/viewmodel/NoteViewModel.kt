package de.zettelgeist.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.zettelgeist.app.ai.LlmManager
import de.zettelgeist.app.ai.KeyStore
import de.zettelgeist.app.data.db.AppDatabase
import de.zettelgeist.app.data.model.Note
import de.zettelgeist.app.data.model.NoteLink
import de.zettelgeist.app.data.model.Tag
import de.zettelgeist.app.data.repository.NoteRepository
import de.zettelgeist.app.data.vault.VaultManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val vault = VaultManager(application)
    private val repository = NoteRepository(db, vault)
    private val llmManager = LlmManager(KeyStore(application))

    val inboxNotes = repository.getNotesByWorkspace("inbox")
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val zettelNotes = repository.getNotesByWorkspace("zettel")
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val sourceNotes = repository.getNotesByWorkspace("source")
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val inboxCount = repository.getInboxCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val allNotes = repository.getAllActiveNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val isLlmAvailable: Boolean get() = llmManager.isAvailable()

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote

    private val _currentNoteContent = MutableStateFlow("")
    val currentNoteContent: StateFlow<String> = _currentNoteContent

    private val _currentNoteTags = MutableStateFlow<List<Tag>>(emptyList())
    val currentNoteTags: StateFlow<List<Tag>> = _currentNoteTags

    private val _backlinks = MutableStateFlow<List<NoteLink>>(emptyList())
    val backlinks: StateFlow<List<NoteLink>> = _backlinks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _aiFormattedText = MutableStateFlow<String?>(null)
    val aiFormattedText: StateFlow<String?> = _aiFormattedText

    private val _aiTagSuggestions = MutableStateFlow<List<String>>(emptyList())
    val aiTagSuggestions: StateFlow<List<String>> = _aiTagSuggestions

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError

    fun loadNote(noteId: String) {
        viewModelScope.launch {
            val note = repository.getNoteById(noteId)
            _currentNote.value = note
            if (note != null) {
                _currentNoteContent.value = repository.readNoteContent(note.filePath)
                _currentNoteTags.value = repository.getTagsForNote(note.id)
                _backlinks.value = repository.getBacklinks(note.id)
            }
        }
    }

    fun clearCurrentNote() {
        _currentNote.value = null
        _currentNoteContent.value = ""
        _currentNoteTags.value = emptyList()
        _backlinks.value = emptyList()
        _aiFormattedText.value = null
        _aiTagSuggestions.value = emptyList()
        _aiError.value = null
    }

    fun saveNote(
        id: String?,
        title: String,
        body: String,
        workspace: String,
        sourceType: String? = null,
        sourceAuthor: String? = null,
        sourceUrl: String? = null
    ) {
        viewModelScope.launch {
            val note = repository.saveNote(id, title, body, workspace, sourceType, sourceAuthor, sourceUrl)
            _currentNote.value = note
            _currentNoteContent.value = body
            _currentNoteTags.value = repository.getTagsForNote(note.id)
            _backlinks.value = repository.getBacklinks(note.id)
        }
    }

    fun softDeleteNote(id: String) {
        viewModelScope.launch {
            repository.softDeleteNote(id)
        }
    }

    fun readNoteContent(filePath: String): String =
        repository.readNoteContent(filePath)

    suspend fun getNoteById(id: String): Note? =
        repository.getNoteById(id)

    suspend fun getAllActiveNotesOnce(): List<Note> =
        repository.getAllActiveNotesOnce()

    suspend fun getAllLinksOnce(): List<NoteLink> =
        repository.getAllLinksOnce()

    fun formatWithAi(body: String) {
        val service = llmManager.getService() ?: return
        _isLoading.value = true
        _aiError.value = null
        viewModelScope.launch {
            val systemPrompt = """Du bist ein Schreibassistent für eine Zettelkasten-Notiz-App.
Der Nutzer gibt dir den Rohtext einer Notiz.

Deine Aufgabe:
1. Formatiere den Text sauber mit Markdown (Überschriften, Listen, Fettdruck für Kernbegriffe)
2. Korrigiere Grammatik- und Rechtschreibfehler
3. Behalte den Inhalt und die Bedeutung exakt bei – ändere keine Fakten, füge nichts hinzu
4. Halte den Text kompakt und klar

Antworte NUR mit dem formatierten Text. Keine Erklärungen, keine Einleitung."""

            val result = service.complete(systemPrompt, body)
            result.onSuccess { _aiFormattedText.value = it }
            result.onFailure { _aiError.value = it.message }
            _isLoading.value = false
        }
    }

    fun suggestTagsWithAi(body: String) {
        val service = llmManager.getService() ?: return
        _isLoading.value = true
        _aiError.value = null
        viewModelScope.launch {
            val allTags = repository.getAllTagsOnce()
            val tagList = allTags.joinToString(", ") { it.name }

            val systemPrompt = """Du bist ein Tag-Assistent für eine Zettelkasten-Notiz-App.

Bestehende Tags in der Wissensbasis des Nutzers:
$tagList

Notiztext:
$body

Schlage 2-5 passende Tags vor. Bevorzuge bestehende Tags wenn sie passen.
Tags müssen kleingeschrieben sein, keine Leerzeichen, Bindestriche erlaubt.

Antworte NUR als JSON-Array: ["tag1", "tag2", "tag3"]"""

            val result = service.complete(systemPrompt, body)
            result.onSuccess { response ->
                try {
                    val cleaned = response.trim()
                        .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val tags = kotlinx.serialization.json.Json.decodeFromString<List<String>>(cleaned)
                    _aiTagSuggestions.value = tags
                } catch (_: Exception) {
                    _aiError.value = "Tags konnten nicht geparst werden"
                }
            }
            result.onFailure { _aiError.value = it.message }
            _isLoading.value = false
        }
    }

    fun clearAiFormattedText() {
        _aiFormattedText.value = null
    }

    fun dismissTagSuggestion(tag: String) {
        _aiTagSuggestions.value = _aiTagSuggestions.value.filter { it != tag }
    }

    fun clearAiError() {
        _aiError.value = null
    }
}
