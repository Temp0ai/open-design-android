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
import java.io.BufferedReader
import java.io.IOException
import java.util.UUID

class OpenDesignApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun generateDesign(
        config: ApiConfig,
        request: GenerationRequest,
        designSystemMd: String
    ): Flow<String> = flow {
        val baseUrl = when (config.provider) {
            "anthropic" -> config.baseUrl.ifEmpty { "https://api.anthropic.com" }
            "openai" -> config.baseUrl.ifEmpty { "https://api.openai.com/v1" }
            "google" -> config.baseUrl.ifEmpty { "https://generativelanguage.googleapis.com/v1beta" }
            else -> config.baseUrl.ifEmpty { "https://api.anthropic.com" }
        }

        val systemPrompt = buildSystemPrompt(request, designSystemMd)
        val userMessage = buildUserPrompt(request)

        val lines = withContext(Dispatchers.IO) {
            when (config.provider) {
                "anthropic" -> fetchSSELines(buildAnthropicRequest(baseUrl, config.apiKey, config.model, systemPrompt, userMessage))
                "openai" -> fetchSSELines(buildOpenAIRequest(baseUrl, config.apiKey, config.model, systemPrompt, userMessage))
                else -> fetchSSELines(buildAnthropicRequest(baseUrl, config.apiKey, config.model, systemPrompt, userMessage))
            }
        }

        for (line in lines) {
            if (!line.startsWith("data: ")) continue
            val data = line.removePrefix("data: ")
            if (data == "[DONE]") break

            try {
                val event = json.parseToJsonElement(data).jsonObject
                val text = when (config.provider) {
                    "anthropic" -> parseAnthropicDelta(event)
                    "openai" -> parseOpenAIDelta(event)
                    else -> parseAnthropicDelta(event)
                }
                if (text != null) emit(text)
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.Default)

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

ARTIFACT TYPE: ${request.mode}
STYLE: ${request.style}
"""
    }

    private fun buildUserPrompt(request: GenerationRequest): String {
        return "Create a ${request.mode} design: ${request.prompt}"
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

    private fun buildOpenAIRequest(
        baseUrl: String, apiKey: String, model: String,
        systemPrompt: String, userMessage: String
    ): Request {
        val body = buildJsonObject {
            put("model", model)
            put("stream", true)
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

        return Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("content-type", "application/json")
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
