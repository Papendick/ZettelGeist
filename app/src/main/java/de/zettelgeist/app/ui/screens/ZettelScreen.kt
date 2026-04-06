package de.zettelgeist.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.zettelgeist.app.ui.components.EmptyState
import de.zettelgeist.app.ui.components.NoteListItem
import de.zettelgeist.app.ui.theme.ZettelEmerald
import de.zettelgeist.app.ui.viewmodel.NoteViewModel

@Composable
fun ZettelScreen(
    viewModel: NoteViewModel,
    onNoteClick: (String) -> Unit,
    onNewNote: () -> Unit
) {
    val notes by viewModel.zettelNotes.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            EmptyState(
                emoji = "\uD83E\uDDE0",
                title = "Noch keine Zettel",
                subtitle = "Zettel sind atomare Gedanken.\nJeder Zettel enthält genau eine Idee."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val preview = viewModel.readNoteContent(note.filePath).take(100)
                    NoteListItem(
                        note = note,
                        preview = preview,
                        onClick = { onNoteClick(note.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onNewNote,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = ZettelEmerald,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Neuer Zettel")
        }
    }
}
