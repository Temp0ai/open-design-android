package com.opendesign.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opendesign.ai.LocalAiEngine
import com.opendesign.api.OpenDesignApi
import com.opendesign.ui.viewmodel.ConnectionStatus
import com.opendesign.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val config by viewModel.apiConfig.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val scope = rememberCoroutineScope()

    var apiEndpoint by remember(config.baseUrl) { mutableStateOf(config.baseUrl) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var selectedProvider by remember(config.provider) { mutableStateOf(config.provider) }
    var selectedModel by remember(config.model) { mutableStateOf(config.model) }
    var scanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }

    val providerModels = OpenDesignApi.PROVIDER_MODELS[selectedProvider] ?: emptyList()
    val providerName = selectedProvider.replaceFirstChar { it.uppercase() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose how AI runs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Provider Section
        item {
            SettingsSection(title = "AI PROVIDER") {
                SettingsSelectRow(
                    label = "Provider",
                    value = providerName,
                    options = listOf("Local AI", "Ollama", "MiMo", "Groq (Free)", "Pollinations (Free)", "Anthropic", "OpenAI", "Google"),
                    onSelect = { provider ->
                        val slug = when (provider) {
                            "Local AI" -> "local"
                            "Groq (Free)" -> "groq"
                            "Pollinations (Free)" -> "pollinations"
                            else -> provider.lowercase()
                        }
                        selectedProvider = slug
                        selectedModel = OpenDesignApi.PROVIDER_MODELS[slug]?.firstOrNull() ?: ""
                        apiEndpoint = OpenDesignApi.PROVIDER_DEFAULT_URLS[slug] ?: ""
                        viewModel.saveProvider(slug)
                        viewModel.saveModel(selectedModel)
                        viewModel.saveBaseUrl(apiEndpoint)
                    }
                )
                SettingsSelectRow(
                    label = "Model",
                    value = selectedModel,
                    options = providerModels,
                    onSelect = {
                        selectedModel = it
                        viewModel.saveModel(it)
                    }
                )
            }
        }

        // Local AI Models
        if (selectedProvider == "local") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "On-Device AI (Free)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "AI runs directly on your phone. No internet needed after download. No API key.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            items(LocalAiEngine.AVAILABLE_MODELS) { model ->
                ModelCard(
                    model = model,
                    isSelected = selectedModel == model.id,
                    onSelect = {
                        selectedModel = model.id
                        viewModel.saveModel(model.id)
                    }
                )
            }
        }

        // Ollama/MiMo Setup
        if (selectedProvider == "ollama" || selectedProvider == "mimo") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedProvider == "ollama") "Ollama Setup" else "MiMo Setup",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Install Ollama on a computer\n2. Run: ollama pull $selectedModel\n3. Run: ollama serve\n4. Both devices on same WiFi\n5. Tap Find below",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    scanning = true
                                    scanResult = null
                                    val found = viewModel.findOllama()
                                    scanning = false
                                    if (found != null) {
                                        apiEndpoint = found
                                        viewModel.saveBaseUrl(found)
                                        scanResult = "Found: $found"
                                    } else {
                                        scanResult = "Not found. Check Ollama is running."
                                    }
                                }
                            },
                            enabled = !scanning,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (scanning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scanning...")
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Find Ollama on Network")
                            }
                        }
                        if (scanResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = scanResult!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (scanResult!!.contains("Found")) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Connection for cloud providers
        if (selectedProvider != "local" && selectedProvider != "ollama" && selectedProvider != "mimo") {
            item {
                SettingsSection(title = "CONNECTION") {
                    SettingsInputRow(label = "Endpoint", value = apiEndpoint, onValueChange = {
                        apiEndpoint = it; viewModel.saveBaseUrl(it)
                    })
                    SettingsInputRow(label = "API Key", value = apiKey, onValueChange = {
                        apiKey = it; viewModel.saveApiKey(it)
                    }, isPassword = true)
                }
            }
        }

        // Status
        item {
            when (val status = connectionStatus) {
                is ConnectionStatus.Testing -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is ConnectionStatus.Success -> Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(status.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer) }
                is ConnectionStatus.Error -> Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(status.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
                else -> {}
            }
        }

        item {
            SettingsSection(title = "ABOUT") {
                SettingsInfoRow(label = "Version", value = "1.0.0")
                SettingsInfoRow(label = "Offline Mode", value = "Local AI - no internet needed")
            }
        }

        item {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { viewModel.resetDefaults() }, shape = RoundedCornerShape(12.dp)) {
                Text("Reset to Defaults")
            }
        }
    }
}

@Composable
fun ModelCard(model: LocalAiEngine.ModelInfo, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = model.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(text = model.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "~${model.sizeMb} MB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Card(shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsInputRow(label: String, value: String, onValueChange: (String) -> Unit, isPassword: Boolean = false) {
    var passwordVisible by remember { mutableStateOf(false) }
    
    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            visualTransformation = if (isPassword && !passwordVisible) {
                androidx.compose.ui.text.input.PasswordVisualTransformation()
            } else {
                androidx.compose.ui.text.input.VisualTransformation.None
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                }
            } else null
        )
    }
}

@Composable
fun SettingsSelectRow(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) }
    }
}

@Composable
fun SettingsInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
