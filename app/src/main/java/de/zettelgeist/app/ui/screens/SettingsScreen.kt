package de.zettelgeist.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.zettelgeist.app.ai.LlmProvider
import de.zettelgeist.app.ui.viewmodel.ConnectionStatus
import de.zettelgeist.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val selectedProvider by viewModel.selectedProvider.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isTesting by viewModel.isTesting.collectAsState()

    var showApiKey by remember { mutableStateOf(false) }
    var showProviderMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "KI-Einstellungen",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Provider selection
            Text(
                text = "Anbieter",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box {
                OutlinedButton(
                    onClick = { showProviderMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (selectedProvider) {
                            null -> "Keiner (KI deaktiviert)"
                            LlmProvider.OPENAI -> "OpenAI"
                            LlmProvider.GEMINI -> "Google Gemini"
                            LlmProvider.ANTHROPIC -> "Anthropic Claude"
                        }
                    )
                }
                DropdownMenu(
                    expanded = showProviderMenu,
                    onDismissRequest = { showProviderMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Keiner (KI deaktiviert)") },
                        onClick = {
                            viewModel.selectProvider(null)
                            showProviderMenu = false
                        }
                    )
                    LlmProvider.entries.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.displayName) },
                            onClick = {
                                viewModel.selectProvider(provider)
                                showProviderMenu = false
                            }
                        )
                    }
                }
            }

            if (selectedProvider != null) {
                Spacer(modifier = Modifier.height(24.dp))

                // API Key
                Text(
                    text = "API-Key",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("API-Key eingeben…") },
                    singleLine = true,
                    visualTransformation = if (showApiKey)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "Verbergen" else "Anzeigen"
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Test connection
                Button(
                    onClick = { viewModel.testConnection() },
                    enabled = apiKey.isNotBlank() && !isTesting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Verbindung testen")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status
                when (val status = connectionStatus) {
                    is ConnectionStatus.Unknown -> {}
                    is ConnectionStatus.Testing -> {
                        Text(
                            text = "Teste Verbindung…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is ConnectionStatus.Connected -> {
                        Text(
                            text = "✅ Verbunden",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val model = viewModel.getModelName()
                        if (model.isNotBlank()) {
                            Text(
                                text = "Modell: $model",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is ConnectionStatus.Error -> {
                        Text(
                            text = "❌ Fehler: ${status.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Info
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = "Dein API-Key wird verschlüsselt auf dem Gerät gespeichert und nur für direkte API-Anfragen an den gewählten Anbieter verwendet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
