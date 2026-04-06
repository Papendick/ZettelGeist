package de.zettelgeist.app.data.db

import androidx.room.*
import de.zettelgeist.app.data.model.NoteTag
import de.zettelgeist.app.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsOnce(): List<Tag>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTag(noteTag: NoteTag)

    @Query("DELETE FROM note_tags WHERE noteId = :noteId")
    suspend fun deleteNoteTagsByNoteId(noteId: String)

    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN note_tags ON tags.id = note_tags.tagId
        WHERE note_tags.noteId = :noteId
        ORDER BY tags.name ASC
    """)
    suspend fun getTagsForNote(noteId: String): List<Tag>

    @Query("""
        SELECT tags.name, COUNT(note_tags.noteId) as count FROM tags
        INNER JOIN note_tags ON tags.id = note_tags.tagId
        INNER JOIN notes ON note_tags.noteId = notes.id
        WHERE notes.deletedAt IS NULL
        GROUP BY tags.id
        ORDER BY count DESC
    """)
    fun getTagsWithCount(): Flow<List<TagWithCount>>

    @Query("""
        SELECT notes.id FROM notes
        INNER JOIN note_tags ON notes.id = note_tags.noteId
        INNER JOIN tags ON note_tags.tagId = tags.id
        WHERE tags.name = :tagName AND notes.deletedAt IS NULL
    """)
    suspend fun getNoteIdsByTag(tagName: String): List<String>
}

data class TagWithCount(
    val name: String,
    val count: Int
)
