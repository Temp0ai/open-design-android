package com.opendesign.ai

import android.content.Context
import android.os.Environment
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class LocalAiEngine(private val context: Context) {

    private var llmInference: LlmInference? = null

    data class ModelInfo(
        val id: String,
        val name: String,
        val description: String,
        val url: String,
        val fileName: String,
        val sizeMb: Int
    )

    companion object {
        val AVAILABLE_MODELS = listOf(
            ModelInfo(
                id = "gemma-2b-it-q4",
                name = "Gemma 2B (Fast)",
                description = "Google's small model. Fast, good for design prompts.",
                url = "https://huggingface.co/nicoboss/gemma-2b-it-gguf/resolve/main/gemma-2b-it-q4_k_m.gguf",
                fileName = "gemma-2b-it-q4_k_m.gguf",
                sizeMb = 1500
            ),
            ModelInfo(
                id = "phi-3-mini-q4",
                name = "Phi-3 Mini (Balanced)",
                description = "Microsoft's compact model. Great code generation.",
                url = "https://huggingface.co/nicoboss/Phi-3-mini-4k-instruct-gguf/resolve/main/phi-3-mini-4k-instruct-q4_k_m.gguf",
                fileName = "phi-3-mini-4k-instruct-q4_k_m.gguf",
                sizeMb = 2400
            ),
            ModelInfo(
                id = "qwen2.5-1.5b-q4",
                name = "Qwen 2.5 1.5B (Tiny)",
                description = "Alibaba's tiny model. Very fast, basic designs.",
                url = "https://huggingface.co/nicoboss/qwen2.5-1.5b-instruct-gguf/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
                fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
                sizeMb = 1000
            )
        )

        private const val MODEL_DIR = "local_models"
    }

    private fun getModelDir(): File {
        val dir = File(context.filesDir, MODEL_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getDownloadedModels(): List<ModelInfo> {
        val dir = getModelDir()
        return AVAILABLE_MODELS.filter { File(dir, it.fileName).exists() }
    }

    fun isModelDownloaded(modelId: String): Boolean {
        val model = AVAILABLE_MODELS.find { it.id == modelId } ?: return false
        return File(getModelDir(), model.fileName).exists()
    }

    fun downloadModel(
        model: ModelInfo,
        onProgress: (Float) -> Unit = {}
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Downloading(0f))
        try {
            val dir = getModelDir()
            val file = File(dir, model.fileName)

            if (file.exists()) {
                emit(DownloadState.Done)
                return@flow
            }

            val tempFile = File(dir, "${model.fileName}.tmp")
            val connection = URL(model.url).openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            val totalSize = connection.contentLength.toLong()

            connection.getInputStream().use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalSize > 0) {
                            val progress = totalRead.toFloat() / totalSize
                            onProgress(progress)
                            emit(DownloadState.Downloading(progress))
                        }
                    }
                }
            }

            tempFile.renameTo(file)
            emit(DownloadState.Done)
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Download failed"))
        }
    }

    suspend fun initializeModel(modelId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val model = AVAILABLE_MODELS.find { it.id == modelId } ?: return@withContext false
                val file = File(getModelDir(), model.fileName)
                if (!file.exists()) return@withContext false

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(file.absolutePath)
                    .setResultListener { partialResult, _ ->
                        // Streaming callback handled in generate()
                    }
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun generate(
        prompt: String,
        systemPrompt: String = "You are an expert UI/UX designer. Create HTML designs."
    ): Flow<String> = flow {
        val engine = llmInference ?: throw IllegalStateException("Model not initialized")

        val fullPrompt = """$systemPrompt

User: $prompt

Assistant: """

        try {
            val response = withContext(Dispatchers.IO) {
                engine.generateResponse(fullPrompt)
            }
            emit(response)
        } catch (e: Exception) {
            throw e
        }
    }

    fun generateStreaming(
        prompt: String,
        systemPrompt: String = "You are an expert UI/UX designer. Create HTML designs."
    ): Flow<String> = flow {
        val engine = llmInference ?: throw IllegalStateException("Model not initialized")

        val fullPrompt = """$systemPrompt

User: $prompt

Assistant: """

        try {
            val partialResults = mutableListOf<String>()

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(
                    File(getModelDir(), AVAILABLE_MODELS.firstOrNull()?.fileName ?: "")
                        .absolutePath
                )
                .setResultListener { partialResult, done ->
                    partialResults.add(partialResult)
                }
                .build()

            withContext(Dispatchers.IO) {
                engine.generateResponseAsync(fullPrompt)
            }

            // Emit collected partial results
            for (result in partialResults) {
                emit(result)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun close() {
        llmInference?.close()
        llmInference = null
    }

    sealed class DownloadState {
        data class Downloading(val progress: Float) : DownloadState()
        object Done : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}
