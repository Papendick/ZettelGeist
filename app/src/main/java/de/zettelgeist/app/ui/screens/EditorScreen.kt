package de.zettelgeist.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.zettelgeist.app.data.model.Note
import de.zettelgeist.app.ui.components.DiffDialog
import de.zettelgeist.app.ui.components.TagSuggestionBar
import de.zettelgeist.app.ui.components.WikilinkDialog
import de.zettelgeist.app.ui.viewmodel.NoteViewModel
import de.zettelgeist.app.util.IdGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: NoteViewModel,
    noteId: String?,
    initialWorkspace: String,
    onBack: () -> Unit
) {
    val currentNote by viewModel.currentNote.collectAsState()
    val currentContent by viewModel.currentNoteContent.collectAsState()
    val backlinks by viewModel.backlinks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val aiFormattedText by viewModel.aiFormattedText.collectAsState()
    val aiTagSuggestions by viewModel.aiTagSuggestions.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    var title by remember { mutableStateOf("") }
    var bodyField by remember { mutableStateOf(TextFieldValue("")) }
    var workspace by remember { mutableStateOf(initialWorkspace) }
    var showWorkspaceMenu by remember { mutableStateOf(false) }
    var showWikilinkDialog by remember { mutableStateOf(false) }
    var allNotes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var noteIdState by remember { mutableStateOf(noteId ?: IdGenerator.generate()) }

    // Source metadata
    var sourceType by remember { mutableStateOf("") }
    var sourceAuthor by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    var autoSaveJob by remember { mutableStateOf<Job?>(null) }

    // Load note on start
    LaunchedEffect(noteId) {
        if (noteId != null) {
            viewModel.loadNote(noteId)
        } else {
            viewModel.clearCurrentNote()
        }
        allNotes = viewModel.getAllActiveNotesOnce()
    }

    // Populate fields when note loads
    LaunchedEffect(currentNote, currentContent) {
        if (noteId != null && currentNote != null) {
            title = currentNote!!.title
            bodyField = TextFieldValue(currentContent)
            workspace = currentNote!!.workspace
            noteIdState = currentNote!!.id
            sourceType = currentNote!!.sourceType ?: ""
            sourceAuthor = currentNote!!.sourceAuthor ?: ""
            sourceUrl = currentNote!!.sourceUrl ?: ""
        }
    }

    // Auto-save function
    fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(2000)
            if (title.isNotBlank()) {
                viewModel.saveNote(
                    id = noteIdState,
                    title = title,
                    body = bodyField.text,
                    workspace = workspace,
                    sourceType = sourceType.ifBlank { null },
                    sourceAuthor = sourceAuthor.ifBlank { null },
                    sourceUrl = sourceUrl.ifBlank { null }
                )
            }
        }
    }

    // Show diff dialog
    if (aiFormattedText != null) {
        DiffDialog(
            original = bodyField.text,
            suggested = aiFormattedText!!,
            onAccept = {
                bodyField = TextFieldValue(aiFormattedText!!)
                viewModel.clearAiFormattedText()
                triggerAutoSave()
            },
            onDismiss = { viewModel.clearAiFormattedText() }
        )
    }

    // Wikilink dialog
    if (showWikilinkDialog) {
        WikilinkDialog(
            notes = allNotes.filter { it.id != noteIdState },
            onSelect = { noteTitle ->
                val insertText = "[[${noteTitle}]]"
                val cursorPos = bodyField.selection.start
                val newText = bodyField.text.substring(0, cursorPos) + insertText + bodyField.text.substring(cursorPos)
                bodyField = TextFieldValue(
                    text = newText,
                    selection = TextRange(cursorPos + insertText.length)
                )
                showWikilinkDialog = false
                triggerAutoSave()
            },
            onDismiss = { showWikilinkDialog = false }
        )
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(aiError) {
        aiError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAiError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "Neue Notiz" else "Bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Save before leaving
                        if (title.isNotBlank()) {
                            viewModel.saveNote(
                                id = noteIdState,
                                title = title,
                                body = bodyField.text,
                                workspace = workspace,
                                sourceType = sourceType.ifBlank { null },
                                sourceAuthor = sourceAuthor.ifBlank { null },
                                sourceUrl = sourceUrl.ifBlank { null }
                            )
                        }
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    // Workspace dropdown
                    Box {
                        TextButton(onClick = { showWorkspaceMenu = true }) {
                            Text(
                                when (workspace) {
                                    "inbox" -> "Inbox"
                                    "zettel" -> "Zettel"
                                    "source" -> "Quellen"
                                    else -> workspace
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = showWorkspaceMenu,
                            onDismissRequest = { showWorkspaceMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Inbox") },
                                onClick = { workspace = "inbox"; showWorkspaceMenu = false; triggerAutoSave() }
                            )
                            DropdownMenuItem(
                                text = { Text("Zettel") },
                                onClick = { workspace = "zettel"; showWorkspaceMenu = false; triggerAutoSave() }
                            )
                            DropdownMenuItem(
                                text = { Text("Quellen") },
                                onClick = { workspace = "source"; showWorkspaceMenu = false; triggerAutoSave() }
                            )
                        }
                    }

                    // Delete
                    if (noteId != null) {
                        IconButton(onClick = {
                            viewModel.softDeleteNote(noteIdState)
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Löschen")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    triggerAutoSave()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Titel", style = MaterialTheme.typography.titleLarge) },
                textStyle = MaterialTheme.typography.titleLarge,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Bold
                IconButton(onClick = {
                    val sel = bodyField.selection
                    val selected = bodyField.text.substring(sel.start, sel.end)
                    val replacement = "**$selected**"
                    val newText = bodyField.text.replaceRange(sel.start, sel.end, replacement)
                    bodyField = TextFieldValue(newText, TextRange(sel.start + replacement.length))
                    triggerAutoSave()
                }, modifier = Modifier.size(40.dp)) {
                    Text("B", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Italic
                IconButton(onClick = {
                    val sel = bodyField.selection
                    val selected = bodyField.text.substring(sel.start, sel.end)
                    val replacement = "*$selected*"
                    val newText = bodyField.text.replaceRange(sel.start, sel.end, replacement)
                    bodyField = TextFieldValue(newText, TextRange(sel.start + replacement.length))
                    triggerAutoSave()
                }, modifier = Modifier.size(40.dp)) {
                    Text("I", fontFamily = FontFamily.Serif, fontSize = 16.sp)
                }

                // Wikilink
                IconButton(onClick = { showWikilinkDialog = true }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Link, contentDescription = "Link einfügen", modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // AI buttons (only visible when LLM configured)
                if (viewModel.isLlmAvailable) {
                    TextButton(
                        onClick = { viewModel.formatWithAi(bodyField.text) },
                        enabled = !isLoading && bodyField.text.isNotBlank()
                    ) {
                        Text("✨ Formatieren", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = { viewModel.suggestTagsWithAi(bodyField.text) },
                        enabled = !isLoading && bodyField.text.isNotBlank()
                    ) {
                        Text("\uD83C\uDFF7\uFE0F Tags", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // AI Tag suggestions
            TagSuggestionBar(
                suggestions = aiTagSuggestions,
                onAccept = { tag ->
                    val tagText = " #$tag"
                    bodyField = TextFieldValue(
                        text = bodyField.text + tagText,
                        selection = TextRange(bodyField.text.length + tagText.length)
                    )
                    viewModel.dismissTagSuggestion(tag)
                    triggerAutoSave()
                },
                onDismiss = { tag -> viewModel.dismissTagSuggestion(tag) }
            )

            // Source metadata (only for source workspace)
            if (workspace == "source") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sourceType,
                        onValueChange = { sourceType = it; triggerAutoSave() },
                        modifier = Modifier.weight(1f),
                        label = { Text("Typ") },
                        placeholder = { Text("Buch/Artikel/URL…") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sourceAuthor,
                        onValueChange = { sourceAuthor = it; triggerAutoSave() },
                        modifier = Modifier.weight(1f),
                        label = { Text("Autor") },
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it; triggerAutoSave() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    label = { Text("URL / ISBN") },
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body
            OutlinedTextField(
                value = bodyField,
                onValueChange = {
                    bodyField = it
                    triggerAutoSave()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 300.dp)
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Schreibe hier…", style = MaterialTheme.typography.bodyLarge) },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            )

            // Char count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${bodyField.text.length} Zeichen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (bodyField.text.length > 1500) {
                Text(
                    text = "Tipp: Diese Notiz ist recht lang. Vielleicht aufteilen?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Backlinks
            if (backlinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Backlinks (${backlinks.size})",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                backlinks.forEach { link ->
                    Text(
                        text = "← ${link.linkText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
