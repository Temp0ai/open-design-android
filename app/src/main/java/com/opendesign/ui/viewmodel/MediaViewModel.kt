package com.opendesign.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opendesign.ai.MediaGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaGenerator = MediaGenerator(application)

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    fun generateImage(
        prompt: String,
        size: MediaGenerator.ImageSize = MediaGenerator.ImageSize.HD_SQUARE,
        style: MediaGenerator.ImageStyle = MediaGenerator.ImageStyle.REALISTIC
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                error = null,
                generatedType = GeneratedType.IMAGE
            )

            try {
                val result = mediaGenerator.generateImage(prompt, size, style)

                if (result.success && result.bitmap != null) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedBitmap = result.bitmap,
                        generatedUrl = result.url,
                        prompt = prompt
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = result.error ?: "Failed to generate image"
                    )
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
                val result = mediaGenerator.generateVideo(prompt)

                if (result.success) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedUrl = result.url,
                        prompt = prompt
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = result.error ?: "Failed to generate video"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Unknown error"
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
                val result = mediaGenerator.generateMusic(prompt)

                if (result.success) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generatedUrl = result.url,
                        prompt = prompt
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = result.error ?: "Failed to generate music"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = MediaUiState()
    }

    fun setError(error: String) {
        _uiState.value = _uiState.value.copy(error = error)
    }
}

data class MediaUiState(
    val isGenerating: Boolean = false,
    val generatedBitmap: Bitmap? = null,
    val generatedUrl: String? = null,
    val prompt: String = "",
    val error: String? = null,
    val generatedType: GeneratedType = GeneratedType.IMAGE
)

enum class GeneratedType {
    IMAGE,
    VIDEO,
    MUSIC
}
