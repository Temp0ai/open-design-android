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

            webView.postDelayed({
                try {
                    val bitmap = Bitmap.createBitmap(
                        webView.width.coerceAtLeast(1920),
                        webView.height.coerceAtLeast(1080),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    webView.draw(canvas)

                    val pptx = XMLSlideShow()
                    pptx.pageSize = org.apache.poi.util.Dimension(12192, 6858)

                    val titleSlide = pptx.createSlide()
                    val titleShape: XSLFTextShape = titleSlide.createTextBox()
                    titleShape.setAnchor(java.awt.Rectangle(50, 50, 11000, 2000))
                    val tf = titleShape.textParagraphs[0]
                    val run = tf.addNewTextRun()
                    run.text = title
                    run.setFontSize(32.0)
                    run.setBold(true)

                    val contentSlide = pptx.createSlide()

                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    val pictureData = IOUtils.toByteArray(baos.inputStream())

                    val idx = pptx.addPicture(pictureData, org.apache.poi.xslf.usermodel.XSLFPictureData.PictureType.PNG)
                    val pictureShape: XSLFPictureShape = contentSlide.createPicture(idx)

                    val slideWidth = 12192.0
                    val slideHeight = 6858.0
                    val scale = minOf(slideWidth / bitmap.width, slideHeight / bitmap.height)
                    val newWidth = (bitmap.width * scale).toInt()
                    val newHeight = (bitmap.height * scale).toInt()
                    val x = ((slideWidth - newWidth) / 2).toInt()
                    val y = ((slideHeight - newHeight) / 2).toInt()

                    pictureShape.setAnchor(java.awt.Rectangle(x, y, newWidth, newHeight))

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
            }, 1500)
        } catch (e: Exception) {
            onResult(ExportResult(false, error = e.message))
        }
    }

    fun exportMultipleToPptx(
        artifacts: List<Pair<String, String>>,
        onResult: (ExportResult) -> Unit
    ) {
        try {
            val pptx = XMLSlideShow()
            pptx.pageSize = org.apache.poi.util.Dimension(12192, 6858)

            val titleSlide = pptx.createSlide()
            val titleShape: XSLFTextShape = titleSlide.createTextBox()
            titleShape.setAnchor(java.awt.Rectangle(50, 50, 11000, 2000))
            val tf = titleShape.textParagraphs[0]
            val run = tf.addNewTextRun()
            run.text = "Open Design Export"
            run.setFontSize(36.0)
            run.setBold(true)

            artifacts.forEach { (title, html) ->
                val slide = pptx.createSlide()

                val textShape: XSLFTextShape = slide.createTextBox()
                textShape.setAnchor(java.awt.Rectangle(50, 200, 11000, 600))
                val textParagraph = textShape.textParagraphs[0]
                val textRun = textParagraph.addNewTextRun()
                textRun.text = title
                textRun.setFontSize(24.0)
                textRun.setBold(true)

                val notesShape: XSLFTextShape = slide.createTextBox()
                notesShape.setAnchor(java.awt.Rectangle(50, 900, 11000, 5000))
                val notesParagraph = notesShape.textParagraphs[0]
                val notesRun = notesParagraph.addNewTextRun()
                notesRun.text = html.take(2000)
                notesRun.setFontSize(10.0)
            }

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
