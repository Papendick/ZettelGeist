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
import de.zettelgeist.app.ui.theme.SourceBlue
import de.zettelgeist.app.ui.viewmodel.NoteViewModel

@Composable
fun SourceScreen(
    viewModel: NoteViewModel,
    onNoteClick: (String) -> Unit,
    onNewNote: () -> Unit
) {
    val notes by viewModel.sourceNotes.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            EmptyState(
                emoji = "\uD83D\uDCDA",
                title = "Keine Quellen",
                subtitle = "Speichere hier Literaturnotizen.\nBücher, Artikel, Podcasts, Videos."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    val preview = buildString {
                        note.sourceType?.let { append("[$it] ") }
                        note.sourceAuthor?.let { append("von $it ") }
                        append(viewModel.readNoteContent(note.filePath).take(80))
                    }
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
            containerColor = SourceBlue,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Neue Quelle")
        }
    }
}
