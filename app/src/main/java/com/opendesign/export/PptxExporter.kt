package com.opendesign.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import android.webkit.WebViewClient
import org.apache.poi.xslf.usermodel.XMLSlideShow
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
                    val titleSlide = pptx.createSlide()
                    val titleShape: XSLFTextShape = titleSlide.createTextBox()
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
                    pictureShape.setOrigin(0f, 0f)
                    pictureShape.resize()

                    val dir = File(context.getExternalFilesDir(null), "OpenDesign")
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, "design_${System.currentTimeMillis()}.pptx")
                    FileOutputStream(file).use { fos -> pptx.write(fos) }
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

            val titleSlide = pptx.createSlide()
            val titleShape: XSLFTextShape = titleSlide.createTextBox()
            val tf = titleShape.textParagraphs[0]
            val run = tf.addNewTextRun()
            run.text = "Open Design Export"
            run.setFontSize(36.0)
            run.setBold(true)

            artifacts.forEach { (title, html) ->
                val slide = pptx.createSlide()
                val textShape: XSLFTextShape = slide.createTextBox()
                val textParagraph = textShape.textParagraphs[0]
                val textRun = textParagraph.addNewTextRun()
                textRun.text = title
                textRun.setFontSize(24.0)
                textRun.setBold(true)
            }

            val dir = File(context.getExternalFilesDir(null), "OpenDesign")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "design_collection_${System.currentTimeMillis()}.pptx")
            FileOutputStream(file).use { fos -> pptx.write(fos) }
            pptx.close()

            onResult(ExportResult(true, file.absolutePath))
        } catch (e: Exception) {
            onResult(ExportResult(false, error = e.message))
        }
    }
}
