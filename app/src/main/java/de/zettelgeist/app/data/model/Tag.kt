package de.zettelgeist.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String
)

@Entity(tableName = "note_tags", primaryKeys = ["noteId", "tagId"])
data class NoteTag(
    val noteId: String,
    val tagId: Long
)
