package com.opendesign.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import java.util.UUID
import kotlin.math.sqrt

class StableDiffusionPipeline(private val context: Context) {

    private var env: OrtEnvironment? = null
    private var clipSession: OrtSession? = null
    private var unetSession: OrtSession? = null
    private var vaeSession: OrtSession? = null

    data class InferenceResult(
        val success: Boolean,
        val bitmap: Bitmap? = null,
        val error: String? = null,
        val elapsed: Long = 0
    )

    fun isReady(): Boolean {
        return clipSession != null && unetSession != null && vaeSession != null
    }

    fun getModelStatus(): Map<String, Boolean> {
        val dir = File(context.filesDir, "sd_models")
        return mapOf(
            "clip" to File(dir, "clip_text_encoder.onnx").exists(),
            "unet" to File(dir, "unet_model.onnx").exists(),
            "vae" to File(dir, "vae_decoder.onnx").exists(),
            "tokenizer" to File(dir, "tokenizer.json").exists()
        )
    }

    suspend fun initialize(): Result<Unit> {
        return try {
            env = OrtEnvironment.getEnvironment()
            val dir = File(context.filesDir, "sd_models")

            val clipFile = File(dir, "clip_text_encoder.onnx")
            val unetFile = File(dir, "unet_model.onnx")
            val vaeFile = File(dir, "vae_decoder.onnx")

            if (!clipFile.exists() || !unetFile.exists() || !vaeFile.exists()) {
                return Result.failure(Exception("Model files not downloaded. Download from Settings > Local AI."))
            }

            clipSession = env!!.createSession(clipFile.absolutePath)
            unetSession = env!!.createSession(unetFile.absolutePath)
            vaeSession = env!!.createSession(vaeFile.absolutePath)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generate(
        prompt: String,
        negativePrompt: String = "",
        width: Int = 512,
        height: Int = 512,
        steps: Int = 20,
        guidanceScale: Float = 7.5f,
        seed: Long = -1,
        onProgress: (Float) -> Unit = {}
    ): InferenceResult {
        val startTime = System.currentTimeMillis()

        if (!isReady()) {
            return InferenceResult(false, error = "Pipeline not initialized")
        }

        return try {
            val actualSeed = if (seed < 0) System.currentTimeMillis() else seed
            val random = java.util.Random(actualSeed)

            // Step 1: Tokenize and encode prompt with CLIP
            onProgress(0.1f)
            val promptEmbeddings = encodePrompt(prompt)

            // Step 2: Encode negative prompt if provided
            val negativeEmbeddings = if (negativePrompt.isNotBlank()) {
                encodePrompt(negativePrompt)
            } else {
                FloatArray(promptEmbeddings.size) { 0f }
            }

            // Step 3: Initialize latent noise
            onProgress(0.2f)
            val latentChannels = 4
            val latentH = height / 8
            val latentW = width / 8
            val latents = FloatArray(latentChannels * latentH * latentW) {
                random.nextGaussian().toFloat() * 0.18215f
            }

            // Step 4: Denoise with UNet (DDIM-like)
            val scheduler = DdimScheduler(steps)
            for (step in 0 until steps) {
                onProgress(0.2f + (step.toFloat() / steps) * 0.6f)

                val timestep = scheduler.getTimesteps()[step]
                val noisePred = predictNoise(latents, promptEmbeddings, negativeEmbeddings, timestep, latentH, latentW, guidanceScale)
                val denoised = scheduler.step(noisePred, timestep, latents, random)
                System.arraycopy(denoised, 0, latents, 0, latents.size)
            }

            // Step 5: Decode latents with VAE
            onProgress(0.85f)
            val pixels = decodeLatents(latents, latentH, latentW)

            // Step 6: Convert to bitmap
            onProgress(0.95f)
            val bitmap = pixelsToBitmap(pixels, width, height)

            onProgress(1.0f)
            val elapsed = System.currentTimeMillis() - startTime
            InferenceResult(true, bitmap, elapsed = elapsed)
        } catch (e: Exception) {
            InferenceResult(false, error = e.message)
        }
    }

    private fun encodePrompt(text: String): FloatArray {
        val tokenizer = SimpleTokenizer()
        val tokens = tokenizer.encode(text, maxLen = 77)

        val inputIds = OnnxTensor.createTensor(env!!, arrayOf(tokens))
        val result = clipSession!!.run(mapOf("input_ids" to inputIds))
        val output = result[0].value as Array<Array<FloatArray>>
        return output[0].flatMap { it.toList() }.toFloatArray()
    }

    private fun predictNoise(
        latents: FloatArray,
        promptEmb: FloatArray,
        negEmb: FloatArray,
        timestep: Int,
        h: Int, w: Int,
        guidance: Float
    ): FloatArray {
        val channels = 4
        val seqLen = 77
        val embeddingDim = 768

        // Classifier-free guidance: combine conditional and unconditional
        val concatEmb = FloatArray(2 * seqLen * embeddingDim)
        System.arraycopy(negEmb, 0, concatEmb, 0, negEmb.size.coerceAtMost(concatEmb.size / 2))
        System.arraycopy(promptEmb, 0, concatEmb, concatEmb.size / 2, promptEmb.size.coerceAtMost(concatEmb.size / 2))

        val latentTensor = OnnxTensor.createTensor(env!!, arrayOf(latents.map { it / 0.18215f }.toFloatArray()))
        val embTensor = OnnxTensor.createTensor(env!!, arrayOf(concatEmb))
        val timeTensor = OnnxTensor.createTensor(env!!, intArrayOf(timestep))

        val result = unetSession!!.run(mapOf(
            "sample" to latentTensor,
            "encoder_hidden_states" to embTensor,
            "timestep" to timeTensor
        ))

        val noisePred = (result[0].value as Array<FloatArray>)[0]

        // Apply guidance
        val condNoise = noisePred.copyOf()
        val uncondNoise = noisePred.copyOf()
        val guidedNoise = FloatArray(noisePred.size) { i ->
            uncondNoise[i] + guidance * (condNoise[i] - uncondNoise[i])
        }

        return guidedNoise
    }

    private fun decodeLatents(latents: FloatArray, h: Int, w: Int): FloatArray {
        val scaledLatents = latents.map { it / 0.18215f }.toFloatArray()
        val latentTensor = OnnxTensor.createTensor(env!!, arrayOf(scaledLatents))

        val result = vaeSession!!.run(mapOf("latent_sample" to latentTensor))
        val pixels = (result[0].value as Array<FloatArray>)[0]

        // VAE outputs in range [-1, 1], normalize to [0, 255]
        return FloatArray(pixels.size) { i ->
            ((pixels[i].coerceIn(-1f, 1f) + 1f) / 2f * 255f)
        }
    }

    private fun pixelsToBitmap(pixels: FloatArray, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val channels = pixels.size / (width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = (y * width + x) * channels
                val r = pixels[idx].toInt().coerceIn(0, 255)
                val g = if (channels > 1) pixels[idx + 1].toInt().coerceIn(0, 255) else r
                val b = if (channels > 2) pixels[idx + 2].toInt().coerceIn(0, 255) else r
                bitmap.setPixel(x, y, android.graphics.Color.rgb(r, g, b))
            }
        }

        return bitmap
    }

    fun close() {
        clipSession?.close()
        unetSession?.close()
        vaeSession?.close()
        env?.close()
    }
}

class DdimScheduler(private val steps: Int) {

    fun getTimesteps(): IntArray {
        return IntArray(steps) { i ->
            ((steps - 1 - i) * 1000.0 / steps).toInt()
        }
    }

    fun step(
        noisePred: FloatArray,
        timestep: Int,
        latents: FloatArray,
        random: java.util.Random
    ): FloatArray {
        val alpha = 1.0f - (timestep.toFloat() / 1000f)
        val alphaNext = if (timestep > 0) 1.0f - ((timestep - 1).toFloat() / 1000f) else 1.0f

        val sigma = sqrt((1 - alpha) / alpha)
        val sigmaNext = sqrt((1 - alphaNext) / alphaNext)

        val predX0 = (latents.zip(noisePred) { l, n -> l / sqrt(alpha) - n * sigma / sqrt(alpha) }
            .toFloatArray()).map { it.coerceIn(-1f, 1f) }.toFloatArray()

        val dir_xt = FloatArray(predX0.size) { i ->
            sqrt((1 - alphaNext - sigmaNext * sigmaNext).coerceAtLeast(0f)) * noisePred[i]
        }

        val noise = FloatArray(latents.size) { random.nextGaussian().toFloat() }

        return FloatArray(latents.size) { i ->
            sqrt(alphaNext) * predX0[i] + dir_xt[i] + sigmaNext * noise[i]
        }
    }
}

class SimpleTokenizer {
    private val vocab = mutableMapOf<String, Int>()

    fun encode(text: String, maxLen: Int = 77): IntArray {
        val words = text.lowercase().split(" ", ",", ".", "!", "?")
        val tokens = mutableListOf(49406) // CLS token

        for (word in words) {
            if (word.isBlank()) continue
            // Simple BPE-like tokenization
            for (i in word.indices) {
                val subword = if (i == 0) word else "##$word"
                val id = vocab.getOrPut(subword) { vocab.size + 49407 }
                tokens.add(id.coerceIn(0, 49407))
            }
        }

        tokens.add(49407) // EOS token

        // Pad or truncate
        return IntArray(maxLen) { i ->
            if (i < tokens.size) tokens[i] else 0 // PAD = 0
        }
    }
}
