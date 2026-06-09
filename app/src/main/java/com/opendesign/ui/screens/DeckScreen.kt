package com.opendesign.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

data class Slide(
    val id: Int,
    val title: String,
    val html: String,
    val notes: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    slides: List<Slide>,
    title: String = "Presentation",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentSlide by remember { mutableIntStateOf(0) }
    var showNotes by remember { mutableStateOf(false) }
    var showThumbnails by remember { mutableStateOf(false) }

    val slide = slides.getOrNull(currentSlide)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "${currentSlide + 1}/${slides.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { showNotes = !showNotes }) {
                        Icon(
                            if (showNotes) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Notes"
                        )
                    }
                    IconButton(onClick = { showThumbnails = !showThumbnails }) {
                        Icon(Icons.Default.GridView, contentDescription = "Slides")
                    }
                    IconButton(onClick = {
                        slide?.let { copyToClipboard(context, it.html) }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = {
                        slide?.let { shareHtml(context, it.html) }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Thumbnail strip
            if (showThumbnails) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(slides) { index, s ->
                        Card(
                            modifier = Modifier
                                .size(120.dp, 72.dp)
                                .clickable {
                                    currentSlide = index
                                    showThumbnails = false
                                },
                            shape = RoundedCornerShape(6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (index == currentSlide)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (index == currentSlide) 4.dp else 1.dp
                            )
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (index == currentSlide)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = s.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Main slide content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount < -50 && currentSlide < slides.lastIndex) {
                                currentSlide++
                            } else if (dragAmount > 50 && currentSlide > 0) {
                                currentSlide--
                            }
                        }
                    }
            ) {
                slide?.let { s ->
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                loadDataWithBaseURL(null, s.html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Speaker notes
            if (showNotes && slide?.notes?.isNotBlank() == true) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Speaker Notes",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = slide.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Navigation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentSlide > 0) currentSlide-- },
                    enabled = currentSlide > 0
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                }

                // Progress dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    slides.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentSlide) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { currentSlide = index }
                        )
                    }
                }

                IconButton(
                    onClick = { if (currentSlide < slides.lastIndex) currentSlide++ },
                    enabled = currentSlide < slides.lastIndex
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}

private fun shareHtml(context: Context, html: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/html"
        putExtra(Intent.EXTRA_TEXT, html)
    }
    context.startActivity(Intent.createChooser(intent, "Share Slide"))
}

private fun copyToClipboard(context: Context, html: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Slide HTML", html))
    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
}
