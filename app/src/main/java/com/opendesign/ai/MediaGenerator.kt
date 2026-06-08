package com.opendesign.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Free AI image and video generation using Pollinations.ai
 * No API key required - completely free
 */
class MediaGenerator(private val context: Context) {

    companion object {
        private const val POLLINATIONS_IMAGE_URL = "https://image.pollinations.ai/prompt/%s?width=%d&height=%d&seed=%d"
        private const val POLLINATIONS_VIDEO_URL = "https://video.pollinations.ai/prompt/%s"
        private const val POLLINATIONS_MUSIC_URL = "https://music.pollinations.ai/prompt/%s"
    }

    enum class ImageSize(val width: Int, val height: Int) {
        PORTRAIT(512, 768),
        LANDSCAPE(768, 512),
        SQUARE(512, 512),
        HD_PORTRAIT(768, 1024),
        HD_LANDSCAPE(1024, 768),
        HD_SQUARE(1024, 1024),
        WALLPAPER(1024, 576)
    }

    enum class ImageStyle(val promptPrefix: String) {
        REALISTIC("realistic photo, 8k, detailed, professional photography"),
        ARTISTIC("digital art, artistic, creative, vibrant colors"),
        ILLUSTRATION("illustration, flat design, clean lines, modern"),
        PIXEL_ART("pixel art, retro style, 8-bit"),
        ANIME("anime style, manga, detailed"),
        SKETCH("pencil sketch, hand drawn, black and white"),
        OIL_PAINTING("oil painting, classical art, detailed brush strokes"),
        WATERCOLOR("watercolor painting, soft colors, artistic"),
        THREE_D("3D render, detailed, realistic lighting"),
        MINIMALIST("minimalist design, clean, simple, modern"),
        CYBERPUNK("cyberpunk style, neon lights, futuristic"),
        FANTASY("fantasy art, magical, epic"),
        PIXEL_8BIT("pixel art, 8-bit retro game style"),
        vector("vector art, flat design, clean lines, logo style"),
        PHOTOGRAPHY("professional photograph, DSLR, high quality")
    }

    data class ImageGenerationResult(
        val success: Boolean,
        val bitmap: Bitmap? = null,
        val url: String? = null,
        val error: String? = null
    )

    data class VideoGenerationResult(
        val success: Boolean,
        val url: String? = null,
        val error: String? = null
    )

    /**
     * Generate AI image from text prompt
     */
    suspend fun generateImage(
        prompt: String,
        size: ImageSize = ImageSize.HD_SQUARE,
        style: ImageStyle = ImageStyle.REALISTIC,
        seed: Long = System.currentTimeMillis()
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        try {
            val fullPrompt = "${style.promptPrefix}, $prompt"
            val encodedPrompt = URLEncoder.encode(fullPrompt, "UTF-8")
            val url = String.format(
                POLLINATIONS_IMAGE_URL,
                encodedPrompt,
                size.width,
                size.height,
                seed
            )

            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 60000
            connection.readTimeout = 120000
            connection.connect()

            val inputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            connection.disconnect()

            if (bitmap != null) {
                ImageGenerationResult(
                    success = true,
                    bitmap = bitmap,
                    url = url
                )
            } else {
                ImageGenerationResult(
                    success = false,
                    error = "Failed to decode image"
                )
            }
        } catch (e: Exception) {
            ImageGenerationResult(
                success = false,
                error = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Generate AI video from text prompt
     * Pollinations.ai video generation: returns video URL directly
     */
    suspend fun generateVideo(
        prompt: String
    ): VideoGenerationResult = withContext(Dispatchers.IO) {
        try {
            val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
            val url = "https://video.pollinations.ai/prompt/${encodedPrompt}?n=8&model=sora"

            // Verify the URL is reachable by doing a GET (not HEAD)
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 120000
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode == 200) {
                VideoGenerationResult(
                    success = true,
                    url = url
                )
            } else {
                VideoGenerationResult(
                    success = false,
                    error = "Video generation returned code $responseCode"
                )
            }
        } catch (e: Exception) {
            VideoGenerationResult(
                success = false,
                error = e.message ?: "Failed to generate video"
            )
        }
    }

    /**
     * Generate AI music from text prompt
     * Pollinations.ai music generation: returns music URL directly
     */
    suspend fun generateMusic(
        prompt: String
    ): VideoGenerationResult = withContext(Dispatchers.IO) {
        try {
            val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
            val url = "https://music.pollinations.ai/prompt/${encodedPrompt}"

            // Verify the URL is reachable
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 120000
            connection.requestMethod = "GET"
            connection.connect()

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode == 200) {
                VideoGenerationResult(
                    success = true,
                    url = url
                )
            } else {
                VideoGenerationResult(
                    success = false,
                    error = "Music generation returned code $responseCode"
                )
            }
        } catch (e: Exception) {
            VideoGenerationResult(
                success = false,
                error = e.message ?: "Failed to generate music"
            )
        }
    }

    /**
     * Get image URL without downloading (for preview)
     */
    fun getImageUrl(
        prompt: String,
        size: ImageSize = ImageSize.HD_SQUARE,
        style: ImageStyle = ImageStyle.REALISTIC,
        seed: Long = System.currentTimeMillis()
    ): String {
        val fullPrompt = "${style.promptPrefix}, $prompt"
        val encodedPrompt = URLEncoder.encode(fullPrompt, "UTF-8")
        return String.format(
            POLLINATIONS_IMAGE_URL,
            encodedPrompt,
            size.width,
            size.height,
            seed
        )
    }
}
