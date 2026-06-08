package com.opendesign.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opendesign.ai.LocalMnnEngine
import com.opendesign.ui.viewmodel.GeneratedType
import com.opendesign.ui.viewmodel.MediaViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(
    viewModel: MediaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val models by viewModel.models.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(0) }
    var prompt by remember { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf<LocalMnnEngine.MnnModel?>(null) }

    val tabs = listOf("Images", "Videos", "Music", "Models")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Media Studio",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Image
                                    1 -> Icons.Default.Videocam
                                    2 -> Icons.Default.MusicNote
                                    3 -> Icons.Default.Storage
                                    else -> Icons.Default.Image
                                },
                                contentDescription = title
                            )
                        }
                    )
                }
            }

            // Prompt Input (not for Models tab)
            if (selectedTab < 3) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    label = {
                        Text(
                            when (selectedTab) {
                                0 -> "Describe your image..."
                                1 -> "Describe your video..."
                                2 -> "Describe your music..."
                                else -> "Enter prompt..."
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    },
                    minLines = 2,
                    maxLines = 4
                )

                // Model selector for images
                if (selectedTab == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { showModelPicker = true },
                            label = { 
                                Text(selectedModel?.name ?: "Default Model") 
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Cpu, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = showModelPicker,
                        onDismissRequest = { showModelPicker = false }
                    ) {
                        models.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(model.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            model.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedModel = model
                                    showModelPicker = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Cpu, contentDescription = null)
                                },
                                trailingIcon = {
                                    if (viewModel.isModelDownloaded(model.id)) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Downloaded",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Generate Button (not for Models tab)
            if (selectedTab < 3) {
                Button(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            when (selectedTab) {
                                0 -> viewModel.generateImage(prompt)
                                1 -> viewModel.generateVideo(prompt)
                                2 -> viewModel.generateMusic(prompt)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = prompt.isNotBlank() && !uiState.isGenerating,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating...", color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (selectedTab) {
                                0 -> "Generate Image"
                                1 -> "Generate Video"
                                2 -> "Generate Music"
                                else -> "Generate"
                            }
                        )
                    }
                }
            }

            // Error Display
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearResult() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            // Models Tab Content
            if (selectedTab == 3) {
                ModelsTab(
                    models = models,
                    downloadProgress = downloadProgress,
                    isModelDownloaded = { viewModel.isModelDownloaded(it) },
                    onDownloadModel = { viewModel.downloadModel(it) }
                )
            }

            // Generated Content Display
            uiState.generatedBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Generated Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // Generation info
                        if (uiState.generationTimeMs > 0) {
                            Text(
                                text = "Generated in ${uiState.generationTimeMs / 1000.0}s",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Copy Prompt
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("prompt", uiState.prompt)
                                clipboard.setPrimaryClip(clip)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Prompt copied!")
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Prompt")
                            }

                            // Share
                            IconButton(onClick = {
                                uiState.generatedUrl?.let { url ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Generated with Open Design AI: $url")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }

                            // Save to Gallery
                            IconButton(onClick = {
                                saveBitmapToGallery(context, bitmap, "open_design_${System.currentTimeMillis()}")
                                scope.launch {
                                    snackbarHostState.showSnackbar("Saved to gallery!")
                                }
                            }) {
                                Icon(Icons.Default.SaveAlt, contentDescription = "Save")
                            }

                            // Regenerate
                            IconButton(onClick = {
                                viewModel.generateImage(prompt)
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                            }
                        }
                    }
                }
            }

            // Video/Music URL Display
            if (uiState.generatedUrl != null && uiState.generatedBitmap == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = when (uiState.generatedType) {
                                GeneratedType.VIDEO -> Icons.Default.Videocam
                                GeneratedType.MUSIC -> Icons.Default.MusicNote
                                else -> Icons.Default.Image
                            },
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = when (uiState.generatedType) {
                                GeneratedType.VIDEO -> "Video Ready!"
                                GeneratedType.MUSIC -> "Music Ready!"
                                else -> "Content Ready!"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Click below to open in browser",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    uiState.generatedUrl?.let { url ->
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open")
                            }

                            Button(
                                onClick = {
                                    uiState.generatedUrl?.let { url ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("url", url)
                                        clipboard.setPrimaryClip(clip)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("URL copied!")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy URL")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { viewModel.clearResult() }
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            // Quick Prompts (only when no result)
            if (uiState.generatedBitmap == null && uiState.generatedUrl == null && !uiState.isGenerating && selectedTab < 3) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick Ideas",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val quickPrompts = when (selectedTab) {
                            0 -> listOf(
                                "A futuristic city at sunset with flying cars",
                                "Beautiful mountain landscape with aurora borealis",
                                "Cute robot assistant helping in a modern office",
                                "Abstract geometric patterns in vibrant colors",
                                "Cyberpunk character with neon lights",
                                "Minimalist logo for a tech startup"
                            )
                            1 -> listOf(
                                "Time-lapse of a flower blooming in spring",
                                "Ocean waves crashing on rocky shore at sunset",
                                "Northern lights dancing over snowy mountains",
                                "Busy city street at night with neon signs",
                                "Butterfly flying through a magical forest",
                                "Abstract particle animation with smooth motion"
                            )
                            2 -> listOf(
                                "Upbeat electronic dance music for workout",
                                "Relaxing ambient music for meditation",
                                "Epic cinematic orchestral music for adventure",
                                "Chill lo-fi hip hop beats for studying",
                                "Smooth jazz piano for a coffee shop",
                                "Inspiring corporate background music"
                            )
                            else -> emptyList()
                        }

                        quickPrompts.forEach { quickPrompt ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        prompt = quickPrompt
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = quickPrompt,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ModelsTab(
    models: List<LocalMnnEngine.MnnModel>,
    downloadProgress: Map<String, Float>,
    isModelDownloaded: (String) -> Boolean,
    onDownloadModel: (LocalMnnEngine.MnnModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Download AI Models",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Download models to run AI completely offline on your device. No internet required after download.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        models.forEach { model ->
            val progress = downloadProgress[model.id]
            val downloaded = isModelDownloaded(model.id)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Cpu,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = model.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Size: ${model.sizeMB}MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (downloaded) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        } else if (progress != null) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            IconButton(
                                onClick = { onDownloadModel(model) }
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    
                    if (progress != null && progress < 1.0f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, filename: String) {
    val contentValues = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OpenDesign")
    }

    val uri = context.contentResolver.insert(
        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )

    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
        }
    }
}
