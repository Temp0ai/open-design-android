package com.opendesign.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opendesign.ai.LocalMnnEngine
import com.opendesign.ai.MnnModel
import com.opendesign.ai.OnnxInferenceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val localEngine = LocalMnnEngine(application)
    private val onnxEngine = OnnxInferenceEngine(application)

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _models = MutableStateFlow<List<MnnModel>>(emptyList())
    val models: StateFlow<List<MnnModel>> = _models.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    init {
        loadModels()
        onnxEngine.initialize()
    }

    private fun loadModels() {
        _models.value = localEngine.getAvailableModels()
    }

    fun generateImage(
        prompt: String,
        width: Int = 512,
        height: Int = 512,
        steps: Int = 20
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null,
                generatedType = GeneratedType.IMAGE
            )

            try {
                // Try local ONNX inference first, fall back to cloud
                val result = onnxEngine.generateImage(prompt, width, height, steps)

                if (result.success && result.bitmap != null) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedBitmap = result.bitmap,
                        prompt = prompt,
                        generationTimeMs = result.inferenceTimeMs
                    )
                } else {
                    // Fall back to cloud
                    val cloudResult = localEngine.generateImage(prompt, width, height, steps)
                    if (cloudResult.success && cloudResult.bitmap != null) {
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            generatedBitmap = cloudResult.bitmap,
                            prompt = prompt,
                            generationTimeMs = cloudResult.generationTimeMs
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            error = cloudResult.error ?: "Failed to generate image"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun generateVideo(prompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null,
                generatedType = GeneratedType.VIDEO
            )

            try {
                val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
                val videoUrl = "https://video.pollinations.ai/prompt/$encodedPrompt"

                val connection = java.net.URL(videoUrl).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.requestMethod = "HEAD"
                connection.connect()
                connection.disconnect()

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generatedUrl = videoUrl,
                    prompt = prompt
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Failed to generate video"
                )
            }
        }
    }

    fun generateMusic(prompt: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null,
                generatedType = GeneratedType.MUSIC
            )

            try {
                val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
                val musicUrl = "https://music.pollinations.ai/prompt/$encodedPrompt"

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generatedUrl = musicUrl,
                    prompt = prompt
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Failed to generate music"
                )
            }
        }
    }

    fun downloadModel(model: MnnModel) {
        viewModelScope.launch {
            _downloadProgress.value = _downloadProgress.value + (model.id to 0f)

            val success = localEngine.downloadModel(model) { progress ->
                _downloadProgress.value = _downloadProgress.value + (model.id to progress)
            }

            if (success) {
                _uiState.value = _uiState.value.copy(
                    downloadedModelId = model.id
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to download ${model.name}"
                )
            }

            _downloadProgress.value = _downloadProgress.value - model.id
        }
    }

    fun isModelDownloaded(modelId: String): Boolean {
        return localEngine.isModelDownloaded(modelId)
    }

    fun clearResult() {
        _uiState.value = MediaUiState()
    }

    fun setError(error: String) {
        _uiState.value = _uiState.value.copy(error = error)
    }

    override fun onCleared() {
        super.onCleared()
        onnxEngine.release()
    }
}

data class MediaUiState(
    val isGenerating: Boolean = false,
    val generatedBitmap: Bitmap? = null,
    val generatedUrl: String? = null,
    val prompt: String = "",
    val error: String? = null,
    val generatedType: GeneratedType = GeneratedType.IMAGE,
    val generationTimeMs: Long = 0,
    val downloadedModelId: String? = null
)

enum class GeneratedType {
    IMAGE,
    VIDEO,
    MUSIC
}

