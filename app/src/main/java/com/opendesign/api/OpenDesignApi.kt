package com.opendesign.api

import com.opendesign.data.model.ApiConfig
import com.opendesign.data.model.GenerationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID

class OpenDesignApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        const val PROVIDER_ANTHROPIC = "anthropic"
        const val PROVIDER_OPENAI = "openai"
        const val PROVIDER_GOOGLE = "google"
        const val PROVIDER_OLLAMA = "ollama"
        const val PROVIDER_MIMO = "mimo"
        const val PROVIDER_LOCAL = "local"

        val PROVIDER_MODELS = mapOf(
            PROVIDER_LOCAL to listOf(
                "gemma-2b-it-q4",
                "phi-3-mini-q4",
                "qwen2.5-1.5b-q4"
            ),
            PROVIDER_ANTHROPIC to listOf(
                "claude-3-5-sonnet-20241022",
                "claude-3-opus-20240229",
                "claude-3-haiku-20240307"
            ),
            PROVIDER_OPENAI to listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4-turbo"
            ),
            PROVIDER_GOOGLE to listOf(
                "gemini-pro",
                "gemini-1.5-flash",
                "gemini-1.5-pro"
            ),
            PROVIDER_OLLAMA to listOf(
                "llama3.1",
                "codellama",
                "mistral",
                "mixtral",
                "qwen2.5",
                "deepseek-coder"
            ),
            PROVIDER_MIMO to listOf(
                "MiMo-7B-RL",
                "MiMo-7B-SFT",
                "mimo-v2.5"
            )
        )

        val PROVIDER_DEFAULT_URLS = mapOf(
            PROVIDER_ANTHROPIC to "https://api.anthropic.com",
            PROVIDER_OPENAI to "https://api.openai.com/v1",
            PROVIDER_GOOGLE to "https://generativelanguage.googleapis.com/v1beta",
            PROVIDER_OLLAMA to "http://10.0.2.2:11434/v1",
            PROVIDER_MIMO to "http://10.0.2.2:11434/v1",
            PROVIDER_LOCAL to "local"
        )
    }

    fun generateDesign(
        config: ApiConfig,
        request: GenerationRequest,
        designSystemMd: String
    ): Flow<String> = flow {
        val baseUrl = getBaseUrl(config)
        val systemPrompt = buildSystemPrompt(request, designSystemMd)
        val userMessage = buildUserPrompt(request)

        val lines = withContext(Dispatchers.IO) {
            when (config.provider) {
                PROVIDER_ANTHROPIC -> fetchSSELines(buildAnthropicRequest(baseUrl, config.apiKey, config.model, systemPrompt, userMessage))
                PROVIDER_OPENAI, PROVIDER_OLLAMA, PROVIDER_MIMO -> fetchSSELines(buildOpenAICompatibleRequest(baseUrl, config.apiKey, config.model, systemPrompt, userMessage))
                PROVIDER_GOOGLE -> fetchSSELines(buildOpenAICompatibleRequest(baseUrl, config.apiKey, config.model, systemPrompt, userMessage))
                else -> fetchSSELines(buildOpenAICompatibleRequest(baseUrl, config.apiKey, config.model, systemPrompt, userMessage))
            }
        }

        for (line in lines) {
            if (!line.startsWith("data: ")) continue
            val data = line.removePrefix("data: ")
            if (data == "[DONE]") break

            try {
                val event = json.parseToJsonElement(data).jsonObject
                val text = when (config.provider) {
                    PROVIDER_ANTHROPIC -> parseAnthropicDelta(event)
                    else -> parseOpenAIDelta(event)
                }
                if (text != null) emit(text)
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.Default)

    private fun getBaseUrl(config: ApiConfig): String {
        if (config.baseUrl.isNotBlank()) return config.baseUrl
        return PROVIDER_DEFAULT_URLS[config.provider] ?: "https://api.openai.com/v1"
    }

    private fun buildSystemPrompt(request: GenerationRequest, designSystemMd: String): String {
        return """You are Open Design, an expert UI/UX designer and frontend developer.

Your task is to create a complete, self-contained HTML artifact based on the user's brief.

DESIGN SYSTEM:
$designSystemMd

INSTRUCTIONS:
1. Create a complete, single-file HTML document with embedded CSS and JavaScript
2. Follow the design system's color palette, typography, spacing, and component patterns
3. Make it responsive and mobile-first
4. Use modern CSS (Grid, Flexbox, custom properties)
5. Include smooth animations and transitions
6. Output ONLY the HTML code, no explanations

ARTIFACT TYPE: ${request.skill}
STYLE: ${request.style}
"""
    }

    private fun buildUserPrompt(request: GenerationRequest): String {
        return "Create a ${request.skill} design: ${request.prompt}"
    }

    private fun buildAnthropicRequest(
        baseUrl: String, apiKey: String, model: String,
        systemPrompt: String, userMessage: String
    ): Request {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", 8192)
            put("stream", true)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", userMessage)
                }
            }
            put("system", systemPrompt)
        }

        return Request.Builder()
            .url("$baseUrl/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    private fun buildOpenAICompatibleRequest(
        baseUrl: String, apiKey: String, model: String,
        systemPrompt: String, userMessage: String
    ): Request {
        val body = buildJsonObject {
            put("model", model)
            put("stream", true)
            put("max_tokens", 8192)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", userMessage)
                }
            }
        }

        val builder = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("content-type", "application/json")

        if (apiKey.isNotBlank()) {
            builder.addHeader("Authorization", "Bearer $apiKey")
        }

        return builder
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    private fun fetchSSELines(request: Request): List<String> {
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            response.close()
            throw IOException("API error ${response.code}: $errorBody")
        }

        val source = response.body?.source() ?: throw IOException("Empty response body")
        val lines = mutableListOf<String>()
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isNotBlank()) {
                    lines.add(line)
                }
            }
        } finally {
            response.close()
        }
        return lines
    }

    private fun parseAnthropicDelta(event: JsonObject): String? {
        val type = event["type"]?.jsonPrimitive?.content
        if (type == "content_block_delta") {
            val delta = event["delta"]?.jsonObject
            return delta?.get("text")?.jsonPrimitive?.content
        }
        return null
    }

    private fun parseOpenAIDelta(event: JsonObject): String? {
        val choices = event["choices"]?.jsonArray
        val delta = choices?.firstOrNull()?.jsonObject?.get("delta")?.jsonObject
        return delta?.get("content")?.jsonPrimitive?.content
    }

    fun generateId(): String = UUID.randomUUID().toString()
}
