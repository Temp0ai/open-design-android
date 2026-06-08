package com.opendesign.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DesignSystem(
    val id: String,
    val name: String,
    val slug: String,
    val description: String = "",
    val category: String = "",
    val color: String = "#6C5CE7",
    val designMd: String = "",
    val tokensCss: String = "",
    val manifest: Map<String, String> = emptyMap()
)

@Serializable
data class Skill(
    val id: String,
    val name: String,
    val slug: String,
    val mode: String = "prototype",
    val scenario: String = "design",
    val description: String = "",
    val skillMd: String = "",
    val defaultFor: String = ""
)

@Serializable
data class Project(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val designSystemId: String = ""
)

@Serializable
data class Artifact(
    val id: String,
    val projectId: String,
    val title: String,
    val skill: String = "",
    val mode: String = "prototype",
    val prompt: String = "",
    val htmlContent: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val designSystemName: String = ""
)

@Serializable
data class ChatMessage(
    val id: String,
    val projectId: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val artifactId: String? = null
)

@Serializable
data class ApiConfig(
    val provider: String = "local",
    val apiKey: String = "",
    val model: String = "gemma-2b-it-q4",
    val baseUrl: String = ""
)

@Serializable
data class GenerationRequest(
    val skill: String,
    val prompt: String,
    val designSystem: String = "linear",
    val style: String = "minimal"
)

@Serializable
data class Plugin(
    val id: String,
    val name: String,
    val description: String = "",
    val category: String = "",
    val skillMd: String = ""
)
