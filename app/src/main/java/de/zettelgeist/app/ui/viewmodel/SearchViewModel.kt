package de.zettelgeist.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.zettelgeist.app.data.db.AppDatabase
import de.zettelgeist.app.data.db.TagWithCount
import de.zettelgeist.app.data.model.Note
import de.zettelgeist.app.data.repository.NoteRepository
import de.zettelgeist.app.data.vault.VaultManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val vault = VaultManager(application)
    private val repository = NoteRepository(db, vault)

    val tagsWithCount = repository.getTagsWithCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchResults = MutableStateFlow<List<Note>>(emptyList())
    val searchResults: StateFlow<List<Note>> = _searchResults

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = repository.searchNotes(query)
        }
    }

    fun filterByTag(tagName: String) {
        _selectedTag.value = if (_selectedTag.value == tagName) null else tagName
        viewModelScope.launch {
            val tag = _selectedTag.value
            if (tag != null) {
                val noteIds = repository.getNoteIdsByTag(tag)
                val notes = noteIds.mapNotNull { repository.getNoteById(it) }
                _searchResults.value = notes
            } else if (_searchQuery.value.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                _searchResults.value = repository.searchNotes(_searchQuery.value)
            }
        }
    }

    fun readNoteContent(filePath: String): String = repository.readNoteContent(filePath)
}
