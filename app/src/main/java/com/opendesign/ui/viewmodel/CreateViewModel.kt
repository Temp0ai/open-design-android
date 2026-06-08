package com.opendesign.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opendesign.ai.LocalAiEngine
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
    private val localAi = LocalAiEngine(application)
    private val settingsManager = SettingsManager(application)

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    val designSystems: StateFlow<List<DesignSystem>> = flow {
        emit(repository.getDesignSystems())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val skills: StateFlow<List<Skill>> = flow {
        emit(repository.getSkills())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedModels: StateFlow<List<LocalAiEngine.ModelInfo>> = flow {
        emit(localAi.getDownloadedModels())
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

    fun downloadModel(model: LocalAiEngine.ModelInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadState = LocalAiEngine.DownloadState.Downloading(0f)) }
            localAi.downloadModel(model) { progress ->
                _uiState.update { it.copy(downloadState = LocalAiEngine.DownloadState.Downloading(progress)) }
            }.collect { state ->
                _uiState.update { it.copy(downloadState = state) }
                if (state is LocalAiEngine.DownloadState.Done) {
                    downloadedModels as MutableStateFlow
                    _uiState.update { it.copy(downloadState = null) }
                }
            }
        }
    }

    fun generate() {
        val state = _uiState.value
        if (state.prompt.isBlank() || state.selectedSkillId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, generatedHtml = null) }

            try {
                val config = settingsManager.apiConfig.first()
                val skill = repository.getSkill(state.selectedSkillId)
                val ds = repository.getDesignSystem(state.selectedDesignSystemSlug ?: "linear")

                val finalHtml: String

                when (config.provider) {
                    "local" -> {
                        // On-device AI
                        val modelReady = localAi.isModelDownloaded(config.model)
                        if (!modelReady) {
                            _uiState.update {
                                it.copy(
                                    isGenerating = false,
                                    error = "Please download a model first in Settings"
                                )
                            }
                            return@launch
                        }

                        if (localAi.generate("test").firstOrNull() == null) {
                            localAi.initializeModel(config.model)
                        }

                        val dsMd = ds?.designMd ?: ""
                        val systemPrompt = buildLocalSystemPrompt(skill?.name ?: "design", dsMd)
                        val htmlBuilder = StringBuilder()
                        localAi.generate(state.prompt, systemPrompt).collect { chunk ->
                            htmlBuilder.append(chunk)
                            _uiState.update { s -> s.copy(streamingText = htmlBuilder.toString()) }
                        }
                        finalHtml = extractHtml(htmlBuilder.toString())
                    }
                    config.provider -> {
                        // Cloud API
                        if (config.apiKey.isBlank()) {
                            // Offline mode - generate local sample HTML
                            finalHtml = generateLocalHtml(
                                prompt = state.prompt,
                                skillName = skill?.name ?: "Prototype",
                                style = state.selectedStyleId ?: "minimal",
                                dsName = ds?.name ?: "Linear",
                                dsColor = ds?.color ?: "#6C5CE7"
                            )
                        } else {
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
                            finalHtml = htmlBuilder.toString()
                        }
                    }
                    else -> {
                        finalHtml = generateLocalHtml(
                            prompt = state.prompt,
                            skillName = skill?.name ?: "Prototype",
                            style = state.selectedStyleId ?: "minimal",
                            dsName = ds?.name ?: "Linear",
                            dsColor = ds?.color ?: "#6C5CE7"
                        )
                    }
                }

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

    private fun buildLocalSystemPrompt(skillName: String, designSystemMd: String): String {
        return """You are an expert UI/UX designer. Create complete HTML designs.

Skill: $skillName
Design System: $designSystemMd

RULES:
1. Output ONLY valid HTML code
2. Include all CSS inline in <style> tags
3. Make it responsive and mobile-first
4. Use modern CSS (Grid, Flexbox, custom properties)
5. Include smooth animations
6. Use the design system colors and typography

Output a complete HTML document:"""
    }

    private fun extractHtml(text: String): String {
        val start = text.indexOf("<!DOCTYPE")
        if (start == -1) {
            val htmlStart = text.indexOf("<html")
            if (htmlStart == -1) return text
            val htmlEnd = text.lastIndexOf("</html>")
            return if (htmlEnd > htmlStart) text.substring(htmlStart, htmlEnd + 7) else text.substring(htmlStart)
        }
        val end = text.lastIndexOf("</html>")
        return if (end > start) text.substring(start, end + 7) else text.substring(start)
    }

    private fun generateLocalHtml(
        prompt: String,
        skillName: String,
        style: String,
        dsName: String,
        dsColor: String
    ): String {
        val gradient = when (style) {
            "bold" -> "linear-gradient(135deg, $dsColor 0%, #E74C3C 100%)"
            "gradient" -> "linear-gradient(135deg, $dsColor 0%, #00D2FF 100%)"
            "neon" -> "linear-gradient(135deg, #000000 0%, $dsColor 50%, #00D2FF 100%)"
            "elegant" -> "linear-gradient(135deg, #1A1A1A 0%, #C4A882 100%)"
            "playful" -> "linear-gradient(135deg, #FF6B6B 0%, $dsColor 50%, #FFC107 100%)"
            else -> "linear-gradient(135deg, $dsColor 0%, #00D2FF 100%)"
        }

        val accent = when (style) {
            "bold" -> "#E74C3C"
            "gradient" -> "#00D2FF"
            "neon" -> "#00D2FF"
            "elegant" -> "#C4A882"
            "playful" -> "#FF6B6B"
            else -> dsColor
        }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$prompt</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #FAFAFA; color: #1A1A1A; line-height: 1.6; }
        .hero { background: $gradient; padding: 80px 24px; text-align: center; color: white; }
        .hero h1 { font-size: 36px; font-weight: 800; margin-bottom: 16px; }
        .hero p { font-size: 18px; opacity: 0.9; margin-bottom: 32px; max-width: 600px; margin-left: auto; margin-right: auto; }
        .hero .cta { background: white; color: $dsColor; border: none; padding: 16px 40px; border-radius: 12px; font-size: 16px; font-weight: 700; cursor: pointer; }
        .section { padding: 60px 24px; max-width: 1000px; margin: 0 auto; }
        .section h2 { font-size: 28px; font-weight: 700; margin-bottom: 12px; text-align: center; }
        .section .subtitle { color: #666; text-align: center; margin-bottom: 40px; }
        .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px; }
        .card { background: white; border-radius: 16px; padding: 32px; border: 1px solid #E8E8E8; }
        .card h3 { font-size: 18px; font-weight: 600; margin-bottom: 8px; }
        .card p { font-size: 14px; color: #666; }
        .badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; background: ${accent}15; color: $dsColor; margin-bottom: 16px; }
        footer { padding: 40px 24px; text-align: center; color: #999; font-size: 14px; border-top: 1px solid #E8E8E8; }
    </style>
</head>
<body>
    <div class="hero">
        <div class="badge">$skillName</div>
        <h1>$prompt</h1>
        <p>Built with Open Design - $dsName Design System</p>
        <button class="cta">Get Started</button>
    </div>
    <div class="section">
        <h2>Features</h2>
        <p class="subtitle">Everything you need</p>
        <div class="grid">
            <div class="card"><h3>Design</h3><p>Professional design with $dsName system</p></div>
            <div class="card"><h3>Fast</h3><p>Generated locally on your device</p></div>
            <div class="card"><h3>Free</h3><p>No API key required</p></div>
        </div>
    </div>
    <footer><p>Generated by Open Design Mobile</p></footer>
</body>
</html>
        """.trimIndent()
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
    val lastArtifact: Artifact? = null,
    val downloadState: com.opendesign.ai.LocalAiEngine.DownloadState? = null
)
