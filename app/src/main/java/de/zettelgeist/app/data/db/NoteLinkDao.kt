package de.zettelgeist.app.data.db

import androidx.room.*
import de.zettelgeist.app.data.model.NoteLink
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteLinkDao {

    @Query("SELECT * FROM note_links WHERE sourceNoteId = :noteId")
    suspend fun getLinksFromNote(noteId: String): List<NoteLink>

    @Query("SELECT * FROM note_links WHERE targetNoteId = :noteId")
    suspend fun getBacklinksToNote(noteId: String): List<NoteLink>

    @Query("SELECT * FROM note_links")
    fun getAllLinks(): Flow<List<NoteLink>>

    @Query("SELECT * FROM note_links")
    suspend fun getAllLinksOnce(): List<NoteLink>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLink)

    @Query("DELETE FROM note_links WHERE sourceNoteId = :noteId")
    suspend fun deleteLinksFromNote(noteId: String)
}
