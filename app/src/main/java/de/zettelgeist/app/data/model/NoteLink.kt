package de.zettelgeist.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_links",
    indices = [Index("sourceNoteId"), Index("targetNoteId")]
)
data class NoteLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceNoteId: String,
    val targetNoteId: String? = null,
    val linkText: String,
    val createdAt: String
)
