package de.zettelgeist.app.ui.screens

import androidx.compose.foundation.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.zettelgeist.app.ui.components.EmptyState
import de.zettelgeist.app.ui.components.NoteListItem
import de.zettelgeist.app.ui.components.TagChip
import de.zettelgeist.app.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNoteClick: (String) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val tagsWithCount by viewModel.tagsWithCount.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.search(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Notizen durchsuchen…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        // Tag cloud
        if (tagsWithCount.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TagCloudLayout(modifier = Modifier.fillMaxWidth()) {
                    tagsWithCount.forEach { tagWithCount ->
                        TagChip(
                            tag = tagWithCount.name,
                            count = tagWithCount.count,
                            selected = selectedTag == tagWithCount.name,
                            onClick = { viewModel.filterByTag(tagWithCount.name) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // Results
        if (results.isEmpty() && (query.isNotBlank() || selectedTag != null)) {
            EmptyState(
                emoji = "\uD83D\uDD0D",
                title = "Keine Ergebnisse",
                subtitle = "Versuche andere Suchbegriffe oder Tags."
            )
        } else if (results.isEmpty()) {
            EmptyState(
                emoji = "\uD83D\uDD0D",
                title = "Suche",
                subtitle = "Durchsuche all deine Notizen\nnach Stichwörtern oder Tags."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { note ->
                    val preview = viewModel.readNoteContent(note.filePath).take(100)
                    NoteListItem(
                        note = note,
                        preview = preview,
                        onClick = { onNoteClick(note.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCloudLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Simple wrapping flow layout using built-in Row with wrapping
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
    }
}
