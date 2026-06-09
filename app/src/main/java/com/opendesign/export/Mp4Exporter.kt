package com.opendesign.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import java.io.FileOutputStream

class Mp4Exporter(private val context: Context) {

    data class ExportResult(
        val success: Boolean,
        val filePath: String? = null,
        val error: String? = null
    )

    fun exportToMp4(
        html: String,
        title: String = "Open Design Video",
        durationSeconds: Int = 5,
        fps: Int = 30,
        width: Int = 1920,
        height: Int = 1080,
        onProgress: (Float) -> Unit = {},
        onResult: (ExportResult) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post {
            try {
                val webView = WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }

                webView.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
                )
                webView.layout(0, 0, width, height)

                webView.postDelayed({
                    try {
                        val totalFrames = durationSeconds * fps
                        val frameInterval = 1000L / fps

                        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                            setInteger(MediaFormat.KEY_BIT_RATE, 8_000_000)
                            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                        }

                        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                        val inputSurface = codec.createInputSurface()
                        codec.start()

                        val dir = File(context.getExternalFilesDir(null), "OpenDesign")
                        if (!dir.exists()) dir.mkdirs()
                        val outputFile = File(dir, "design_${System.currentTimeMillis()}.mp4")
                        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                        var muxerTrackIndex = -1
                        var muxerStarted = false

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        val bufferInfo = MediaCodec.BufferInfo()

                        for (frame in 0 until totalFrames) {
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            webView.draw(canvas)

                            val surfaceCanvas = inputSurface.lockCanvas(null)
                            surfaceCanvas?.drawBitmap(bitmap, 0f, 0f, null)
                            inputSurface.unlockCanvasAndPost(surfaceCanvas)

                            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                            if (outputIndex >= 0) {
                                val outputBuffer = codec.getOutputBuffer(outputIndex)
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                    bufferInfo.size = 0
                                }
                                if (bufferInfo.size > 0 && outputBuffer != null) {
                                    if (!muxerStarted) {
                                        muxerTrackIndex = muxer.addTrack(codec.outputFormat)
                                        muxer.start()
                                        muxerStarted = true
                                    }
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(muxerTrackIndex, outputBuffer, bufferInfo)
                                }
                                codec.releaseOutputBuffer(outputIndex, false)
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                            }

                            onProgress((frame + 1).toFloat() / totalFrames)
                            Thread.sleep(frameInterval)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            codec.signalEndOfStream()
                        } else {
                            codec.stop()
                        }

                        var drained = false
                        while (!drained) {
                            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                            if (outputIndex >= 0) {
                                val outputBuffer = codec.getOutputBuffer(outputIndex)
                                if (bufferInfo.size > 0 && outputBuffer != null) {
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(muxerTrackIndex, outputBuffer, bufferInfo)
                                }
                                codec.releaseOutputBuffer(outputIndex, false)
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) drained = true
                            }
                        }

                        bitmap.recycle()
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            codec.stop()
                        }
                        codec.release()
                        if (muxerStarted) muxer.stop()
                        muxer.release()

                        onResult(ExportResult(true, outputFile.absolutePath))
                    } catch (e: Exception) {
                        onResult(ExportResult(false, error = e.message))
                    }
                }, 2000)
            } catch (e: Exception) {
                onResult(ExportResult(false, error = e.message))
            }
        }
    }

    fun exportWithAnimation(
        html: String,
        animationScript: String,
        durationSeconds: Int = 10,
        fps: Int = 30,
        width: Int = 1920,
        height: Int = 1080,
        onResult: (ExportResult) -> Unit
    ) {
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            try {
                val enhancedHtml = html.replace("</body>", """
                    <script>
                    $animationScript
                    window.__animationDuration = ${durationSeconds * 1000};
                    window.__startTime = Date.now();
                    </script>
                    </body>
                """.trimIndent())
                exportToMp4(enhancedHtml, durationSeconds = durationSeconds, fps = fps, width = width, height = height, onResult = onResult)
            } catch (e: Exception) {
                onResult(ExportResult(false, error = e.message))
            }
        }
    }
}
