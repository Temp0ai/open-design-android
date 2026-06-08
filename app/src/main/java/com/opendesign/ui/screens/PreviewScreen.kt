package com.opendesign.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.opendesign.export.Mp4Exporter
import com.opendesign.export.PptxExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(html: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExportMenu by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isExporting) {
                        CircularProgressIndicator(
                            progress = { exportProgress },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { showExportMenu = true }, enabled = !isExporting) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                    IconButton(onClick = { shareHtml(context, html) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { copyToClipboard(context, html) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy HTML")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )

        DropdownMenu(
            expanded = showExportMenu,
            onDismissRequest = { showExportMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Export as HTML") },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                onClick = {
                    showExportMenu = false
                    exportHtmlFile(context, html)
                }
            )
            DropdownMenuItem(
                text = { Text("Export as PDF") },
                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                onClick = {
                    showExportMenu = false
                    exportPdf(context, html)
                }
            )
            DropdownMenuItem(
                text = { Text("Export as PPTX") },
                leadingIcon = { Icon(Icons.Default.Slideshow, contentDescription = null) },
                onClick = {
                    showExportMenu = false
                    isExporting = true
                    scope.launch {
                        val exporter = PptxExporter(context)
                        exporter.exportToPptx(html) { result ->
                            isExporting = false
                            if (result.success) {
                                Toast.makeText(context, "PPTX saved to ${result.filePath}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "PPTX export failed: ${result.error}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("Export as MP4") },
                leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                onClick = {
                    showExportMenu = false
                    isExporting = true
                    scope.launch {
                        val exporter = Mp4Exporter(context)
                        exporter.exportToMp4(
                            html = html,
                            durationSeconds = 5,
                            fps = 30,
                            onProgress = { exportProgress = it },
                            onResult = { result ->
                                isExporting = false
                                exportProgress = 0f
                                if (result.success) {
                                    Toast.makeText(context, "MP4 saved to ${result.filePath}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "MP4 export failed: ${result.error}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Share HTML") },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    showExportMenu = false
                    shareHtml(context, html)
                }
            )
            DropdownMenuItem(
                text = { Text("Copy HTML") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = {
                    showExportMenu = false
                    copyToClipboard(context, html)
                }
            )
        }
    }
}

private fun shareHtml(context: Context, html: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/html"
        putExtra(Intent.EXTRA_TEXT, html)
        putExtra(Intent.EXTRA_SUBJECT, "Open Design - Generated Artifact")
    }
    context.startActivity(Intent.createChooser(intent, "Share Design"))
}

private fun copyToClipboard(context: Context, html: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("HTML Artifact", html)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "HTML copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun exportHtmlFile(context: Context, html: String) {
    try {
        val dir = java.io.File(context.getExternalFilesDir(null), "OpenDesign")
        if (!dir.exists()) dir.mkdirs()
        val file = java.io.File(dir, "design_${System.currentTimeMillis()}.html")
        file.writeText(html)
        Toast.makeText(context, "Saved to ${file.absolutePath}", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.fromFile(file), "text/html")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(Intent.createChooser(intent, "Open HTML"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun exportPdf(context: Context, html: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val webView = WebView(context).apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
        webView.postDelayed({
            try {
                val printAdapter = webView.createPrintDocumentAdapter("Open Design Export")
                printManager.print("Open Design Export", printAdapter, PrintAttributes.Builder().build())
                Toast.makeText(context, "Opening print dialog for PDF export...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "PDF export: use Print to save as PDF", Toast.LENGTH_LONG).show()
            }
        }, 1000)
    } catch (e: Exception) {
        Toast.makeText(context, "PDF export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
