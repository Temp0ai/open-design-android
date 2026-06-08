package com.opendesign.figma

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.concurrent.TimeUnit

class FigmaApi(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class FigmaFile(
        val name: String,
        val pages: List<FigmaPage>
    )

    data class FigmaPage(
        val id: String,
        val name: String,
        val nodes: List<FigmaNode>
    )

    data class FigmaNode(
        val id: String,
        val name: String,
        val type: String,
        val fills: List<FigmaFill> = emptyList(),
        val style: FigmaStyle? = null,
        val absoluteBoundingBox: FigmaRect? = null,
        val children: List<FigmaNode> = emptyList()
    )

    data class FigmaFill(
        val type: String,
        val color: FigmaColor? = null,
        val opacity: Double = 1.0
    )

    data class FigmaColor(
        val r: Double,
        val g: Double,
        val b: Double,
        val a: Double = 1.0
    ) {
        fun toHex(): String {
            val ri = (r * 255).toInt().coerceIn(0, 255)
            val gi = (g * 255).toInt().coerceIn(0, 255)
            val bi = (b * 255).toInt().coerceIn(0, 255)
            return String.format("#%02X%02X%02X", ri, gi, bi)
        }
    }

    data class FigmaStyle(
        val fontFamily: String? = null,
        val fontSize: Double? = null,
        val fontWeight: Int? = null,
        val letterSpacing: Double? = null,
        val lineHeightPx: Double? = null
    )

    data class FigmaRect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double
    )

    data class FigmaImageResult(
        val success: Boolean,
        val bitmap: Bitmap? = null,
        val error: String? = null
    )

    /**
     * Get file structure from Figma API
     */
    suspend fun getFile(
        fileKey: String,
        token: String
    ): FigmaFile? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.figma.com/v1/files/$fileKey?depth=3")
                .addHeader("X-Figma-Token", token)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body?.string() ?: return@withContext null
            val root = json.parseToJsonElement(body).jsonObject
            val document = root["document"]?.jsonObject ?: return@withContext null

            val pages = mutableListOf<FigmaPage>()
            document["children"]?.jsonArray?.forEach { pageEl ->
                val pageObj = pageEl.jsonObject
                val page = FigmaPage(
                    id = pageObj["id"]?.jsonPrimitive?.content ?: "",
                    name = pageObj["name"]?.jsonPrimitive?.content ?: "",
                    nodes = parseNodes(pageObj["children"]?.jsonArray ?: buildJsonArray {})
                )
                pages.add(page)
            }

            FigmaFile(
                name = root["name"]?.jsonPrimitive?.content ?: "Untitled",
                pages = pages
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNodes(nodes: JsonArray): List<FigmaNode> {
        return nodes.mapNotNull { el ->
            try {
                val obj = el.jsonObject
                FigmaNode(
                    id = obj["id"]?.jsonPrimitive?.content ?: "",
                    name = obj["name"]?.jsonPrimitive?.content ?: "",
                    type = obj["type"]?.jsonPrimitive?.content ?: "",
                    fills = parseFills(obj["fills"]?.jsonArray ?: buildJsonArray {}),
                    style = parseStyle(obj["style"]?.jsonObject),
                    absoluteBoundingBox = parseRect(obj["absoluteBoundingBox"]?.jsonObject),
                    children = parseNodes(obj["children"]?.jsonArray ?: buildJsonArray {})
                )
            } catch (_: Exception) { null }
        }
    }

    private fun parseFills(fills: JsonArray): List<FigmaFill> {
        return fills.mapNotNull { el ->
            try {
                val obj = el.jsonObject
                FigmaFill(
                    type = obj["type"]?.jsonPrimitive?.content ?: "",
                    color = parseColor(obj["color"]?.jsonObject),
                    opacity = obj["opacity"]?.jsonPrimitive?.double ?: 1.0
                )
            } catch (_: Exception) { null }
        }
    }

    private fun parseColor(color: JsonObject?): FigmaColor? {
        color ?: return null
        return FigmaColor(
            r = color["r"]?.jsonPrimitive?.double ?: 0.0,
            g = color["g"]?.jsonPrimitive?.double ?: 0.0,
            b = color["b"]?.jsonPrimitive?.double ?: 0.0,
            a = color["a"]?.jsonPrimitive?.double ?: 1.0
        )
    }

    private fun parseStyle(style: JsonObject?): FigmaStyle? {
        style ?: return null
        return FigmaStyle(
            fontFamily = style["fontFamily"]?.jsonPrimitive?.content,
            fontSize = style["fontSize"]?.jsonPrimitive?.double,
            fontWeight = style["fontWeight"]?.jsonPrimitive?.int,
            letterSpacing = style["letterSpacing"]?.jsonPrimitive?.double,
            lineHeightPx = style["lineHeightPx"]?.jsonPrimitive?.double
        )
    }

    private fun parseRect(rect: JsonObject?): FigmaRect? {
        rect ?: return null
        return FigmaRect(
            x = rect["x"]?.jsonPrimitive?.double ?: 0.0,
            y = rect["y"]?.jsonPrimitive?.double ?: 0.0,
            width = rect["width"]?.jsonPrimitive?.double ?: 0.0,
            height = rect["height"]?.jsonPrimitive?.double ?: 0.0
        )
    }

    /**
     * Export node as image from Figma
     */
    suspend fun exportImage(
        fileKey: String,
        nodeIds: List<String>,
        token: String,
        format: String = "png",
        scale: Double = 2.0
    ): FigmaImageResult = withContext(Dispatchers.IO) {
        try {
            val idsParam = nodeIds.joinToString(",")
            val request = Request.Builder()
                .url("https://api.figma.com/v1/images/$fileKey?ids=$idsParam&format=$format&scale=$scale")
                .addHeader("X-Figma-Token", token)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext FigmaImageResult(false, error = "API error: ${response.code}")
            }

            val body = response.body?.string() ?: return@withContext FigmaImageResult(false, error = "Empty response")
            val root = json.parseToJsonElement(body).jsonObject
            val images = root["images"]?.jsonObject

            val firstUrl = images?.values?.firstOrNull()?.jsonPrimitive?.content
            if (firstUrl.isNullOrBlank()) {
                return@withContext FigmaImageResult(false, error = "No image URL returned")
            }

            val imageRequest = Request.Builder().url(firstUrl).build()
            val imageResponse = client.newCall(imageRequest).execute()
            val inputStream = imageResponse.body?.byteStream() ?: return@withContext FigmaImageResult(false, error = "No image data")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                FigmaImageResult(true, bitmap)
            } else {
                FigmaImageResult(false, error = "Failed to decode image")
            }
        } catch (e: Exception) {
            FigmaImageResult(false, error = e.message)
        }
    }

    /**
     * Convert Figma node tree to HTML/CSS
     */
    fun figmaToHtml(node: FigmaNode, indent: Int = 0): String {
        val pad = "  ".repeat(indent)
        val tag = when (node.type) {
            "FRAME", "GROUP", "COMPONENT", "COMPONENT_SET" -> "div"
            "TEXT" -> "p"
            "RECTANGLE" -> "div"
            "ELLIPSE" -> "div"
            "VECTOR" -> "svg"
            else -> "div"
        }

        val style = StringBuilder()
        style.append("position: relative; ")

        node.absoluteBoundingBox?.let { rect ->
            style.append("width: ${rect.width.toInt()}px; ")
            style.append("height: ${rect.height.toInt()}px; ")
        }

        node.fills.firstOrNull { it.type == "SOLID" }?.let { fill ->
            fill.color?.let { color ->
                style.append("background-color: ${color.toHex()}; ")
                if (fill.opacity < 1.0) {
                    style.append("opacity: ${fill.opacity}; ")
                }
            }
        }

        node.style?.let { s ->
            s.fontFamily?.let { style.append("font-family: '$it', sans-serif; ") }
            s.fontSize?.let { style.append("font-size: ${it.toInt()}px; ") }
            s.fontWeight?.let { style.append("font-weight: $it; ") }
            s.letterSpacing?.let { style.append("letter-spacing: ${it}px; ") }
            s.lineHeightPx?.let { style.append("line-height: ${it}px; ") }
        }

        val children = node.children.joinToString("\n") { figmaToHtml(it, indent + 1) }

        return if (node.children.isEmpty()) {
            "$pad<$tag style=\"${style}\">${node.name}</$tag>"
        } else {
            "$pad<$tag style=\"${style}\">\n$children\n$pad</$tag>"
        }
    }

    /**
     * Convert Figma file to full HTML document
     */
    fun figmaFileToHtml(file: FigmaFile): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html><head>")
        sb.appendLine("<meta charset=\"UTF-8\">")
        sb.appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
        sb.appendLine("<title>${file.name}</title>")
        sb.appendLine("<style>* { margin: 0; padding: 0; box-sizing: border-box; } body { font-family: system-ui, -apple-system, sans-serif; }</style>")
        sb.appendLine("</head><body>")

        file.pages.forEach { page ->
            sb.appendLine("<div class=\"page\" style=\"padding: 20px;\">")
            sb.appendLine("<h2>${page.name}</h2>")
            page.nodes.forEach { node ->
                sb.appendLine(figmaToHtml(node))
            }
            sb.appendLine("</div>")
        }

        sb.appendLine("</body></html>")
        return sb.toString()
    }
}
