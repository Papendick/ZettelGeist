package de.zettelgeist.app.data.repository

import de.zettelgeist.app.data.db.AppDatabase
import de.zettelgeist.app.data.db.TagWithCount
import de.zettelgeist.app.data.model.*
import de.zettelgeist.app.data.vault.VaultManager
import de.zettelgeist.app.util.IdGenerator
import de.zettelgeist.app.util.MarkdownParser
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class NoteRepository(
    private val db: AppDatabase,
    private val vault: VaultManager
) {
    private val noteDao = db.noteDao()
    private val tagDao = db.tagDao()
    private val linkDao = db.noteLinkDao()

    fun getNotesByWorkspace(workspace: String): Flow<List<Note>> =
        noteDao.getNotesByWorkspace(workspace)

    fun getAllActiveNotes(): Flow<List<Note>> =
        noteDao.getAllActiveNotes()

    fun getDeletedNotes(): Flow<List<Note>> =
        noteDao.getDeletedNotes()

    fun getInboxCount(): Flow<Int> =
        noteDao.getCountByWorkspace("inbox")

    suspend fun getNoteById(id: String): Note? =
        noteDao.getNoteById(id)

    suspend fun getNoteByTitle(title: String): Note? =
        noteDao.getNoteByTitle(title)

    suspend fun getAllActiveNotesOnce(): List<Note> =
        noteDao.getAllActiveNotesOnce()

    fun readNoteContent(filePath: String): String =
        vault.readNote(filePath)

    suspend fun searchNotes(query: String): List<Note> {
        val ftsQuery = query.trim().split(" ").joinToString(" ") { "$it*" }
        return try {
            noteDao.searchNotes(ftsQuery)
        } catch (_: Exception) {
            noteDao.searchNotesByTitle(query)
        }
    }

    suspend fun searchNotesByTitle(query: String): List<Note> =
        noteDao.searchNotesByTitle(query)

    suspend fun saveNote(
        id: String? = null,
        title: String,
        body: String,
        workspace: String,
        sourceType: String? = null,
        sourceAuthor: String? = null,
        sourceUrl: String? = null
    ): Note {
        val now = Instant.now().toString()
        val noteId = id ?: IdGenerator.generate()
        val fileName = MarkdownParser.sanitizeFileName(title) + ".md"
        val filePath = "$workspace/$fileName"
        val hash = MarkdownParser.contentHash(body)

        val existingNote = noteDao.getNoteById(noteId)

        // Write markdown file
        vault.writeNote(filePath, body)

        // Delete old file if path changed
        if (existingNote != null && existingNote.filePath != filePath) {
            vault.deleteNote(existingNote.filePath)
        }

        val note = Note(
            id = noteId,
            title = title,
            filePath = filePath,
            workspace = workspace,
            contentHash = hash,
            charCount = body.length,
            createdAt = existingNote?.createdAt ?: now,
            updatedAt = now,
            deletedAt = null,
            sourceType = sourceType,
            sourceAuthor = sourceAuthor,
            sourceUrl = sourceUrl
        )

        noteDao.insertNote(note)

        // Parse and save tags
        syncTags(noteId, body)

        // Parse and save wikilinks
        syncLinks(noteId, body)

        return note
    }

    private suspend fun syncTags(noteId: String, body: String) {
        tagDao.deleteNoteTagsByNoteId(noteId)
        val tagNames = MarkdownParser.extractTags(body)
        for (name in tagNames) {
            var tag = tagDao.getTagByName(name)
            if (tag == null) {
                val tagId = tagDao.insertTag(Tag(name = name))
                tag = Tag(id = tagId, name = name)
            }
            tagDao.insertNoteTag(NoteTag(noteId = noteId, tagId = tag.id))
        }
    }

    private suspend fun syncLinks(noteId: String, body: String) {
        linkDao.deleteLinksFromNote(noteId)
        val linkTexts = MarkdownParser.extractWikilinks(body)
        val now = Instant.now().toString()
        for (linkText in linkTexts) {
            val targetNote = noteDao.getNoteByTitle(linkText)
            linkDao.insertLink(
                NoteLink(
                    sourceNoteId = noteId,
                    targetNoteId = targetNote?.id,
                    linkText = linkText,
                    createdAt = now
                )
            )
        }
    }

    suspend fun softDeleteNote(id: String) {
        val now = Instant.now().toString()
        noteDao.softDeleteNote(id, now)
    }

    suspend fun restoreNote(id: String) {
        noteDao.restoreNote(id)
    }

    suspend fun getBacklinks(noteId: String): List<NoteLink> =
        linkDao.getBacklinksToNote(noteId)

    suspend fun getLinksFromNote(noteId: String): List<NoteLink> =
        linkDao.getLinksFromNote(noteId)

    fun getAllLinks(): Flow<List<NoteLink>> =
        linkDao.getAllLinks()

    suspend fun getAllLinksOnce(): List<NoteLink> =
        linkDao.getAllLinksOnce()

    suspend fun getTagsForNote(noteId: String): List<Tag> =
        tagDao.getTagsForNote(noteId)

    fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags()

    suspend fun getAllTagsOnce(): List<Tag> =
        tagDao.getAllTagsOnce()

    fun getTagsWithCount(): Flow<List<TagWithCount>> =
        tagDao.getTagsWithCount()

    suspend fun getNoteIdsByTag(tagName: String): List<String> =
        tagDao.getNoteIdsByTag(tagName)
}
