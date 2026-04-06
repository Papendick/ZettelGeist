package de.zettelgeist.app.data.db

import androidx.room.*
import de.zettelgeist.app.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE workspace = :workspace AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getNotesByWorkspace(workspace: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: String): Note?

    @Query("SELECT * FROM notes WHERE title = :title AND deletedAt IS NULL LIMIT 1")
    suspend fun getNoteByTitle(title: String): Note?

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY title ASC")
    suspend fun getAllActiveNotesOnce(): List<Note>

    @Query("SELECT COUNT(*) FROM notes WHERE workspace = :workspace AND deletedAt IS NULL")
    fun getCountByWorkspace(workspace: String): Flow<Int>

    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.rowid = notes_fts.rowid
        WHERE notes_fts MATCH :query AND notes.deletedAt IS NULL
        ORDER BY notes.updatedAt DESC
    """)
    suspend fun searchNotes(query: String): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Query("UPDATE notes SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteNote(id: String, deletedAt: String)

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreNote(id: String)

    @Delete
    suspend fun deleteNotePermanently(note: Note)

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND title LIKE '%' || :query || '%' ORDER BY title ASC LIMIT 20")
    suspend fun searchNotesByTitle(query: String): List<Note>
}
