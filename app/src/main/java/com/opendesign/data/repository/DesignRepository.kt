package com.opendesign.data.repository

import android.content.Context
import com.opendesign.api.OpenDesignApi
import com.opendesign.data.db.AppDatabase
import com.opendesign.data.db.ArtifactEntity
import com.opendesign.data.db.ProjectEntity
import com.opendesign.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import java.util.UUID

class DesignRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val projectDao = db.projectDao()
    private val artifactDao = db.artifactDao()
    private val api = OpenDesignApi()

    // Projects
    fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getProject(id: String): Project? {
        return projectDao.getProject(id)?.toDomain()
    }

    suspend fun createProject(name: String, designSystemId: String = ""): Project {
        val project = Project(
            id = api.generateId(),
            name = name,
            designSystemId = designSystemId
        )
        projectDao.insertProject(project.toEntity())
        return project
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProject(project.toEntity())
    }

    // Artifacts
    fun getArtifactsForProject(projectId: String): Flow<List<Artifact>> {
        return artifactDao.getArtifactsForProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getAllArtifacts(): Flow<List<Artifact>> {
        return artifactDao.getAllArtifacts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getArtifact(id: String): Artifact? {
        return artifactDao.getArtifact(id)?.toDomain()
    }

    suspend fun saveArtifact(artifact: Artifact) {
        artifactDao.insertArtifact(artifact.toEntity())
    }

    suspend fun deleteArtifact(artifact: Artifact) {
        artifactDao.deleteArtifact(artifact.toEntity())
    }

    // Design Systems
    fun getDesignSystems(): List<DesignSystem> {
        return loadDesignSystemsFromAssets()
    }

    fun getDesignSystem(slug: String): DesignSystem? {
        return loadDesignSystemsFromAssets().find { it.slug == slug }
    }

    // Skills
    fun getSkills(): List<Skill> {
        return loadSkillsFromAssets()
    }

    fun getSkill(slug: String): Skill? {
        return loadSkillsFromAssets().find { it.slug == slug }
    }

    // Plugins
    fun getPlugins(): List<Plugin> {
        return loadPluginsFromAssets()
    }

    fun getPluginsByCategory(category: String): List<Plugin> {
        return loadPluginsFromAssets().filter { it.category == category }
    }

    // Generation
    fun generateDesign(
        config: ApiConfig,
        request: GenerationRequest
    ): Flow<String> {
        val ds = getDesignSystem(request.designSystem)
        val designMd = ds?.designMd ?: getDefaultDesignMd()
        return api.generateDesign(config, request, designMd)
    }

    // Assets loading
    private fun loadDesignSystemsFromAssets(): List<DesignSystem> {
        return try {
            val json = context.assets.open("design-systems/metadata.json").bufferedReader().use { it.readText() }
            val arr = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonArray
            arr.map { el ->
                val obj = el.jsonObject
                DesignSystem(
                    id = obj["id"]?.jsonPrimitive?.content ?: "",
                    name = obj["name"]?.jsonPrimitive?.content ?: "",
                    slug = obj["id"]?.jsonPrimitive?.content ?: "",
                    description = obj["description"]?.jsonPrimitive?.content ?: "",
                    color = obj["color"]?.jsonPrimitive?.content ?: "#6366f1",
                    category = "Design System"
                )
            }
        } catch (_: Exception) {
            getDefaultDesignSystems()
        }
    }

    private fun loadSkillsFromAssets(): List<Skill> {
        return try {
            val json = context.assets.open("skills/metadata.json").bufferedReader().use { it.readText() }
            val arr = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonArray
            arr.map { el ->
                val obj = el.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: ""
                val category = obj["category"]?.jsonPrimitive?.content ?: "other"
                Skill(
                    id = id,
                    name = obj["name"]?.jsonPrimitive?.content ?: id,
                    slug = id,
                    mode = when (category) {
                        "prototype" -> "prototype"
                        "deck" -> "deck"
                        "marketing" -> "image"
                        "creative" -> "image"
                        else -> "prototype"
                    },
                    scenario = category,
                    description = obj["description"]?.jsonPrimitive?.content ?: ""
                )
            }
        } catch (_: Exception) {
            getDefaultSkills()
        }
    }

    private fun loadPluginsFromAssets(): List<Plugin> {
        return try {
            val json = context.assets.open("plugins/metadata.json").bufferedReader().use { it.readText() }
            val arr = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonArray
            arr.map { el ->
                val obj = el.jsonObject
                Plugin(
                    id = obj["id"]?.jsonPrimitive?.content ?: "",
                    name = obj["name"]?.jsonPrimitive?.content ?: "",
                    category = obj["category"]?.jsonPrimitive?.content ?: "",
                    source = obj["source"]?.jsonPrimitive?.content ?: "official"
                )
            }
        } catch (_: Exception) {
            getDefaultPlugins()
        }
    }

    private fun getDefaultDesignMd(): String = """
# Design System

## Colors
- Primary: #6C5CE7
- Secondary: #00D2FF
- Background: #FAFAFA
- Text: #1A1A1A

## Typography
- Font: Inter, system-ui
- Headings: Bold, tight tracking
- Body: Regular, relaxed

## Spacing
- Base unit: 4px
- Scale: 4, 8, 12, 16, 24, 32, 48, 64

## Components
- Rounded corners (8-12px)
- Subtle shadows
- Clean borders (#E8E8E8)
- Consistent padding (16px)
    """.trimIndent()

    private fun getDefaultDesignSystems() = listOf(
        DesignSystem("linear", "Linear", "linear", "Clean, minimal SaaS", color = "#5E6AD2"),
        DesignSystem("stripe", "Stripe", "stripe", "Modern fintech", color = "#635BFF"),
        DesignSystem("vercel", "Vercel", "vercel", "Developer-first", color = "#000000"),
        DesignSystem("airbnb", "Airbnb", "airbnb", "Warm, friendly", color = "#FF5A5F"),
        DesignSystem("apple", "Apple", "apple", "Premium, refined", color = "#1D1D1F"),
        DesignSystem("notion", "Notion", "notion", "Productivity", color = "#000000"),
        DesignSystem("figma", "Figma", "figma", "Creative tools", color = "#A259FF"),
        DesignSystem("supabase", "Supabase", "supabase", "Open source", color = "#3ECF8E"),
    )

    private fun getDefaultSkills() = listOf(
        Skill("web-prototype", "Web Prototype", "web-prototype", "prototype", "design"),
        Skill("mobile-app", "Mobile App", "mobile-app", "prototype", "design"),
        Skill("dashboard", "Dashboard", "dashboard", "prototype", "design"),
        Skill("pitch-deck", "Pitch Deck", "pitch-deck", "deck", "marketing"),
        Skill("social-post", "Social Post", "social-post", "image", "marketing"),
        Skill("logo", "Logo Design", "logo", "image", "design"),
    )

    private fun getDefaultPlugins() = listOf(
        Plugin("web-prototype", "Web Prototype", "Create web prototypes", "Template", "official"),
        Plugin("mobile-app", "Mobile App", "Create mobile app designs", "Template", "official"),
        Plugin("dashboard", "Dashboard", "Create dashboard designs", "Template", "official"),
        Plugin("pitch-deck", "Pitch Deck", "Create pitch decks", "Template", "official"),
        Plugin("social-post", "Social Post", "Create social media posts", "Template", "official"),
        Plugin("logo", "Logo Design", "Create logo designs", "Template", "official"),
        Plugin("hyperframes", "HyperFrames", "Motion graphics", "Video Template", "official"),
        Plugin("video-shortform", "Short-form Video", "Create short videos", "Video Template", "official")
    )

    // Mappers
    private fun ProjectEntity.toDomain() = Project(id, name, createdAt, updatedAt, designSystemId)
    private fun Project.toEntity() = ProjectEntity(id, name, createdAt, updatedAt, designSystemId)
    private fun ArtifactEntity.toDomain() = Artifact(id, projectId, title, skill, mode, prompt, htmlContent, createdAt, designSystemName)
    private fun Artifact.toEntity() = ArtifactEntity(id, projectId, title, skill, mode, prompt, htmlContent, createdAt, designSystemName)
}
