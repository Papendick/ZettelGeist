package de.zettelgeist.app.data.model

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val workspace: String, // "inbox", "source", "zettel"
    val contentHash: String,
    val charCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val sourceType: String? = null,
    val sourceAuthor: String? = null,
    val sourceUrl: String? = null
)

@Fts4(contentEntity = Note::class)
@Entity(tableName = "notes_fts")
data class NoteFts(
    val title: String
)
