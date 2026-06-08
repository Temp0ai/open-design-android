package com.opendesign.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opendesign.ui.viewmodel.ConnectionStatus
import com.opendesign.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val config by viewModel.apiConfig.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    var apiEndpoint by remember(config.baseUrl) { mutableStateOf(config.baseUrl.ifEmpty { "https://api.anthropic.com" }) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var selectedProvider by remember(config.provider) { mutableStateOf(config.provider.replaceFirstChar { it.uppercase() }) }
    var selectedModel by remember(config.model) { mutableStateOf(config.model) }
    var defaultFormat by remember { mutableStateOf("HTML") }

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
                    text = "Configure your Open Design connection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Connection Section
        item {
            SettingsSection(title = "CONNECTION") {
                SettingsInputRow(
                    label = "API Endpoint",
                    value = apiEndpoint,
                    onValueChange = {
                        apiEndpoint = it
                        viewModel.saveBaseUrl(it)
                    }
                )
                SettingsInputRow(
                    label = "API Key",
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        viewModel.saveApiKey(it)
                    },
                    isPassword = true
                )
            }
        }

        // AI Provider Section
        item {
            SettingsSection(title = "AI PROVIDER") {
                SettingsSelectRow(
                    label = "Provider",
                    value = selectedProvider,
                    options = listOf("Anthropic", "OpenAI", "Google", "Ollama"),
                    onSelect = {
                        selectedProvider = it
                        viewModel.saveProvider(it.lowercase())
                    }
                )
                SettingsSelectRow(
                    label = "Model",
                    value = selectedModel,
                    options = listOf("claude-3-5-sonnet-20241022", "claude-3-opus-20240229", "gpt-4o", "gemini-pro"),
                    onSelect = {
                        selectedModel = it
                        viewModel.saveModel(it)
                    }
                )
            }
        }

        // Export Section
        item {
            SettingsSection(title = "EXPORT") {
                SettingsSelectRow(
                    label = "Default Format",
                    value = defaultFormat,
                    options = listOf("HTML", "PDF", "PPTX", "ZIP"),
                    onSelect = { defaultFormat = it }
                )
            }
        }

        // About Section
        item {
            SettingsSection(title = "ABOUT") {
                SettingsInfoRow(label = "Version", value = "1.0.0")
                SettingsInfoRow(label = "Open Design", value = "github.com/nexu-io/open-design")
            }
        }

        // Connection status
        item {
            when (val status = connectionStatus) {
                is ConnectionStatus.Testing -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is ConnectionStatus.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = status.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                is ConnectionStatus.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = status.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {}
            }
        }

        item {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.resetDefaults() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reset to Defaults")
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingsSelectRow(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option) },
                onClick = {
                    onSelect(option)
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
