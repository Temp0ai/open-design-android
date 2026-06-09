package com.opendesign.ai

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class LocalAiEngine(private val context: Context) {

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
                id = "qwen2.5-1.5b-q4",
                name = "Qwen 2.5 1.5B (Tiny)",
                description = "Alibaba's tiny model. ~1GB download. Runs fast on any phone.",
                url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
                fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
                sizeMb = 1000
            ),
            ModelInfo(
                id = "tinyllama-1.1b-q4",
                name = "TinyLlama 1.1B (Ultra Fast)",
                description = "Smallest model. ~700MB download. Instant responses.",
                url = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                fileName = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
                sizeMb = 700
            ),
            ModelInfo(
                id = "llama3.1-8b-q4",
                name = "Llama 3.1 8B (Best)",
                description = "Meta's best open model. ~4.5GB download. Best quality.",
                url = "https://huggingface.co/bartowski/Meta-Llama-3.1-8B-Instruct-GGUF/resolve/main/Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf",
                fileName = "Meta-Llama-3.1-8B-Instruct-Q4_K_M.gguf",
                sizeMb = 4500
            ),
            ModelInfo(
                id = "mistral-7b-q4",
                name = "Mistral 7B (Balanced)",
                description = "Mistral's instruct model. ~4GB download. Great balance.",
                url = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf",
                fileName = "mistral-7b-instruct-v0.2.Q4_K_M.gguf",
                sizeMb = 4000
            ),
            ModelInfo(
                id = "codellama-7b-q4",
                name = "CodeLlama 7B (Code)",
                description = "Meta's code model. ~4GB download. Best for code generation.",
                url = "https://huggingface.co/TheBloke/CodeLlama-7B-Instruct-GGUF/resolve/main/codellama-7b-instruct.Q4_K_M.gguf",
                fileName = "codellama-7b-instruct.Q4_K_M.gguf",
                sizeMb = 4000
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

    fun deleteModel(modelId: String): Boolean {
        val model = AVAILABLE_MODELS.find { it.id == modelId } ?: return false
        val file = File(getModelDir(), model.fileName)
        val tmp = File(getModelDir(), "${model.fileName}.tmp")
        if (tmp.exists()) tmp.delete()
        return file.delete()
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
            val connection = URL(model.url).openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "OpenDesign-Android/1.0")

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                emit(DownloadState.Error("Download failed: HTTP $responseCode. Check your internet connection."))
                return@flow
            }

            val totalSize = connection.contentLength.toLong()

            connection.inputStream.use { input ->
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

    sealed class DownloadState {
        data class Downloading(val progress: Float) : DownloadState()
        object Done : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}
