package com.opendesign.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opendesign.api.OpenDesignApi
import com.opendesign.data.model.*
import com.opendesign.data.preferences.SettingsManager
import com.opendesign.data.repository.DesignRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class CreateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DesignRepository(application)
    private val api = OpenDesignApi()
    private val settingsManager = SettingsManager(application)

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    val designSystems: StateFlow<List<DesignSystem>> = flow {
        emit(repository.getDesignSystems())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val skills: StateFlow<List<Skill>> = flow {
        emit(repository.getSkills())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updatePrompt(prompt: String) {
        _uiState.update { it.copy(prompt = prompt) }
    }

    fun selectSkill(skillId: String?) {
        _uiState.update { it.copy(selectedSkillId = skillId) }
    }

    fun selectStyle(styleId: String?) {
        _uiState.update { it.copy(selectedStyleId = styleId) }
    }

    fun selectDesignSystem(slug: String?) {
        _uiState.update { it.copy(selectedDesignSystemSlug = slug) }
    }

    fun generate() {
        val state = _uiState.value
        if (state.prompt.isBlank() || state.selectedSkillId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, generatedHtml = null) }

            try {
                val config = settingsManager.apiConfig.first()

                if (config.apiKey.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            error = "Please set your API key in Settings"
                        )
                    }
                    return@launch
                }

                val skill = repository.getSkill(state.selectedSkillId)
                val ds = repository.getDesignSystem(state.selectedDesignSystemSlug ?: "linear")

                val request = GenerationRequest(
                    skill = skill?.slug ?: "web-prototype",
                    prompt = state.prompt,
                    designSystem = state.selectedDesignSystemSlug ?: "linear",
                    style = state.selectedStyleId ?: "minimal"
                )

                val htmlBuilder = StringBuilder()
                repository.generateDesign(config, request).collect { chunk ->
                    htmlBuilder.append(chunk)
                    _uiState.update { s -> s.copy(streamingText = htmlBuilder.toString()) }
                }

                val finalHtml = htmlBuilder.toString()
                val title = state.prompt.take(50)

                val artifact = Artifact(
                    id = UUID.randomUUID().toString(),
                    projectId = "default",
                    title = title,
                    skill = skill?.slug ?: "web-prototype",
                    mode = skill?.mode ?: "prototype",
                    prompt = state.prompt,
                    htmlContent = finalHtml,
                    designSystemName = ds?.name ?: "Linear"
                )

                repository.saveArtifact(artifact)

                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generatedHtml = finalHtml,
                        streamingText = null,
                        lastArtifact = artifact
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = e.message ?: "Generation failed"
                    )
                }
            }
        }
    }

    fun clearResult() {
        _uiState.update {
            it.copy(generatedHtml = null, streamingText = null, error = null, lastArtifact = null)
        }
    }
}

data class CreateUiState(
    val prompt: String = "",
    val selectedSkillId: String? = null,
    val selectedStyleId: String? = null,
    val selectedDesignSystemSlug: String? = null,
    val isGenerating: Boolean = false,
    val generatedHtml: String? = null,
    val streamingText: String? = null,
    val error: String? = null,
    val lastArtifact: Artifact? = null
)
