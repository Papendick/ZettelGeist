package de.zettelgeist.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.zettelgeist.app.data.model.Note

@Composable
fun WikilinkDialog(
    notes: List<Note>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }

    val filtered = notes.filter {
        it.title.contains(query, ignoreCase = true)
    }.take(20)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link einfügen") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Notiz suchen…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(filtered) { note ->
                        Text(
                            text = note.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(note.title) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (filtered.isEmpty() && query.isNotBlank()) {
                        item {
                            Text(
                                text = "Keine Notizen gefunden",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
