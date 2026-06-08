package com.opendesign.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import android.webkit.WebViewClient
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.apache.poi.xslf.usermodel.XSLFPictureShape
import org.apache.poi.util.IOUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class PptxExporter(private val context: Context) {

    data class ExportResult(
        val success: Boolean,
        val filePath: String? = null,
        val error: String? = null
    )

    /**
     * Export HTML content to a PPTX file
     * Creates a single slide with the design rendered as an image
     */
    fun exportToPptx(
        html: String,
        title: String = "Open Design Export",
        onResult: (ExportResult) -> Unit
    ) {
        try {
            val webView = WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }

            // Wait for WebView to load, then capture
            webView.postDelayed({
                try {
                    // Capture WebView as bitmap
                    val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    webView.draw(canvas)

                    // Create PPTX
                    val pptx = XMLSlideShow()

                    // Set 16:9 slide size
                    pptx.pageSize = org.apache.poi.util.Dimension(12192, 6858) // 10" x 5.63" in EMU

                    // Add title slide
                    val titleSlide = pptx.createSlide()
                    val titleShape: XSLFTextShape = titleSlide.createTextBox()
                    titleShape.anchor = org.apache.poi.util.Rectangle2D.Double(50.0, 50.0, 11000.0, 2000.0)
                    val tf = titleShape.textParagraphs[0]
                    tf.text = title
                    tf.fontSize = 32.0
                    tf.isBold = true

                    // Add content slide with the rendered image
                    val contentSlide = pptx.createSlide()

                    // Convert bitmap to bytes
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val pictureData = IOUtils.toByteArray(baos.inputStream())

                    // Add picture to slide
                    val idx = pptx.addPicture(pictureData, org.apache.poi.xslf.usermodel.XSLFPictureData.PictureType.PNG)
                    val pictureShape: XSLFPictureShape = contentSlide.createPicture(idx)
                    
                    // Scale to fit slide (16:9)
                    val slideWidth = 12192.0
                    val slideHeight = 6858.0
                    val scale = minOf(slideWidth / bitmap.width, slideHeight / bitmap.height)
                    val newWidth = bitmap.width * scale
                    val newHeight = bitmap.height * scale
                    val x = (slideWidth - newWidth) / 2
                    val y = (slideHeight - newHeight) / 2

                    pictureShape.anchor = org.apache.poi.util.Rectangle2D.Double(x, y, newWidth, newHeight)

                    // Save to file
                    val dir = File(context.getExternalFilesDir(null), "OpenDesign")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "design_${System.currentTimeMillis()}.pptx")
                    FileOutputStream(file).use { fos ->
                        pptx.write(fos)
                    }

                    pptx.close()
                    bitmap.recycle()

                    onResult(ExportResult(true, file.absolutePath))
                } catch (e: Exception) {
                    onResult(ExportResult(false, error = e.message))
                }
            }, 1500) // Wait for WebView to render
        } catch (e: Exception) {
            onResult(ExportResult(false, error = e.message))
        }
    }

    /**
     * Export multiple HTML artifacts to a multi-slide PPTX
     */
    fun exportMultipleToPptx(
        artifacts: List<Pair<String, String>>, // (title, html) pairs
        onResult: (ExportResult) -> Unit
    ) {
        try {
            val pptx = XMLSlideShow()
            pptx.pageSize = org.apache.poi.util.Dimension(12192, 6858)

            // Add title slide
            val titleSlide = pptx.createSlide()
            val titleShape: XSLFTextShape = titleSlide.createTextBox()
            titleShape.anchor = org.apache.poi.util.Rectangle2D.Double(50.0, 50.0, 11000.0, 2000.0)
            val tf = titleShape.textParagraphs[0]
            tf.text = "Open Design Export"
            tf.fontSize = 36.0
            tf.isBold = true

            // Add each artifact as a slide
            artifacts.forEach { (title, html) ->
                val slide = pptx.createSlide()

                // Add title text
                val textShape: XSLFTextShape = slide.createTextBox()
                textShape.anchor = org.apache.poi.util.Rectangle2D.Double(50.0, 200.0, 11000.0, 600.0)
                val textParagraph = textShape.textParagraphs[0]
                textParagraph.text = title
                textParagraph.fontSize = 24.0
                textParagraph.isBold = true

                // Add HTML content as note
                val notesShape: XSLFTextShape = slide.createTextBox()
                notesShape.anchor = org.apache.poi.util.Rectangle2D.Double(50.0, 900.0, 11000.0, 5000.0)
                val notesParagraph = notesShape.textParagraphs[0]
                notesParagraph.text = html.take(2000) // Truncate for notes
                notesParagraph.fontSize = 10.0
            }

            // Save
            val dir = File(context.getExternalFilesDir(null), "OpenDesign")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "design_collection_${System.currentTimeMillis()}.pptx")
            FileOutputStream(file).use { fos ->
                pptx.write(fos)
            }
            pptx.close()

            onResult(ExportResult(true, file.absolutePath))
        } catch (e: Exception) {
            onResult(ExportResult(false, error = e.message))
        }
    }
}
