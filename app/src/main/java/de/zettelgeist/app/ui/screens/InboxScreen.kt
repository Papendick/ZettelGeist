package de.zettelgeist.app.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.zettelgeist.app.ui.components.EmptyState
import de.zettelgeist.app.ui.components.NoteListItem
import de.zettelgeist.app.ui.theme.InboxAmber
import de.zettelgeist.app.ui.viewmodel.NoteViewModel
import de.zettelgeist.app.util.IdGenerator
import java.util.Locale

@Composable
fun InboxScreen(
    viewModel: NoteViewModel,
    onNoteClick: (String) -> Unit,
    onNewNote: () -> Unit
) {
    val notes by viewModel.inboxNotes.collectAsState()
    val context = LocalContext.current

    var isRecording by remember { mutableStateOf(false) }
    var transcript by remember { mutableStateOf("") }
    var hasAudioPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) isRecording = true
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            transcript = ""
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    isRecording = false
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        val noteId = IdGenerator.generate()
                        val title = text.take(50).let { if (text.length > 50) "$it…" else it }
                        viewModel.saveNote(
                            id = noteId,
                            title = title,
                            body = text,
                            workspace = "inbox"
                        )
                    }
                    isRecording = false
                    transcript = ""
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    transcript = matches?.firstOrNull() ?: transcript
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer.startListening(intent)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty() && !isRecording) {
            EmptyState(
                emoji = "\uD83D\uDCE5",
                title = "Deine Inbox ist leer",
                subtitle = "Erfasse schnelle Gedanken hier.\nTippe + oder nutze das Mikrofon."
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

        // Recording overlay
        if (isRecording) {
            RecordingOverlay(
                transcript = transcript,
                onStop = {
                    speechRecognizer.stopListening()
                    isRecording = false
                }
            )
        }

        // FAB Row
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mic FAB
            FloatingActionButton(
                onClick = {
                    if (isRecording) {
                        speechRecognizer.stopListening()
                        isRecording = false
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                containerColor = if (isRecording) Color.Red else InboxAmber,
                contentColor = Color.White
            ) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stopp" else "Sprachaufnahme"
                )
            }

            // New note FAB
            FloatingActionButton(
                onClick = onNewNote,
                containerColor = InboxAmber,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Neue Notiz")
            }
        }
    }
}

@Composable
private fun RecordingOverlay(
    transcript: String,
    onStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(20.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = Color.Red
                ) {}
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Aufnahme läuft…",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (transcript.isNotBlank()) {
                    Text(
                        text = transcript,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stopp")
                }
            }
        }
    }
}
