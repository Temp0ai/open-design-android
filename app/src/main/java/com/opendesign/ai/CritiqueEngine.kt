package com.opendesign.ai

data class CritiqueResult(
    val accessibility: CritiqueDimension,
    val uiux: CritiqueDimension,
    val visuals: CritiqueDimension,
    val performance: CritiqueDimension,
    val architecture: CritiqueDimension,
    val overall: Double,
    val suggestions: List<String>
)

data class CritiqueDimension(
    val score: Double,       // 0-10
    val weight: Double,      // relative weight
    val notes: List<String>,
    val autoFixable: List<String>
) {
    val weighted get() = score * weight
    val grade get() = when {
        score >= 9.0 -> "A+"
        score >= 8.0 -> "A"
        score >= 7.0 -> "B"
        score >= 6.0 -> "C"
        score >= 5.0 -> "D"
        else -> "F"
    }
    val color get() = when {
        score >= 8.0 -> "green"
        score >= 6.0 -> "amber"
        else -> "red"
    }
}

object CritiqueEngine {
    private val dimensions = listOf(
        "accessibility",
        "ui-ux",
        "visuals",
        "performance",
        "architecture"
    )

    suspend fun critique(
        html: String,
        brief: String = "",
        designSystem: String = "",
        apiClient: OpenDesignApi? = null,
        model: String = "gpt-4o-mini"
    ): CritiqueResult {
        val prompt = buildString {
            appendLine("You are a senior product designer. Critically evaluate this HTML artifact across 5 dimensions.")
            appendLine()
            if (brief.isNotBlank()) appendLine("Original brief: $brief")
            if (designSystem.isNotBlank()) appendLine("Design system used: $designSystem")
            appendLine()
            appendLine("HTML artifact (first 3000 chars):")
            appendLine(html.take(3000))
            appendLine()
            appendLine("Score each dimension 0-10 with:")
            appendLine("- notes: specific issues found")
            appendLine("- autoFixable: issues that can be automatically fixed")
            appendLine()
            appendLine("Return JSON with dimensions: accessibility, ui-ux, visuals, performance, architecture")
            appendLine("Each: { score: double, notes: [string], autoFixable: [string] }")
            appendLine("Also provide: suggestions: [string] (top 5 improvement recommendations)")
        }

        if (apiClient != null) {
            try {
                val response = apiClient.streamMessage(
                    userMessage = prompt,
                    systemMessage = "You are a design critique engine. Return valid JSON only.",
                    model = model
                )
                return parseCritiqueJson(response)
            } catch (_: Exception) { }
        }

        return analyzeLocally(html, brief)
    }

    private fun analyzeLocally(html: String, brief: String): CritiqueResult {
        val lower = html.lowercase()
        val issues = mutableListOf<String>()
        val autoFix = mutableListOf<String>()

        // Accessibility
        val a11yScore = run {
            var s = 7.0
            if (!lower.contains("aria-")) { s -= 1.5; issues.add("Missing ARIA attributes"); autoFix.add("Add aria-label to interactive elements") }
            if (!lower.contains("alt=")) { s -= 1.0; issues.add("Missing alt text on images"); autoFix.add("Add descriptive alt text") }
            if (!lower.contains("role=")) { s -= 0.5; issues.add("Missing semantic roles"); autoFix.add("Add role attributes") }
            if (lower.contains("tabindex")) s += 0.5
            if (lower.contains("<label")) s += 0.5
            if (lower.contains("lang=")) s += 0.5
            s.coerceIn(0.0, 10.0)
        }

        // UI/UX
        val uiuxScore = run {
            var s = 7.0
            if (lower.contains("overflow: hidden")) { s -= 0.5; issues.add("Overflow hidden may clip content") }
            if (!lower.contains("hover") && !lower.contains(":focus")) { s -= 1.0; issues.add("No hover/focus states"); autoFix.add("Add hover and focus styles") }
            if (lower.contains("cursor: pointer")) s += 0.5
            if (lower.contains("transition")) s += 1.0
            if (lower.contains("@media")) s += 0.5
            if (!lower.contains("@media")) { issues.add("Not responsive"); autoFix.add("Add responsive media queries") }
            s.coerceIn(0.0, 10.0)
        }

        // Visuals
        val visualsScore = run {
            var s = 7.5
            if (lower.contains("background-color") || lower.contains("background:")) s += 0.5
            if (lower.contains("border-radius")) s += 0.5
            if (lower.contains("box-shadow") || lower.contains("text-shadow")) s += 0.5
            if (lower.contains("linear-gradient") || lower.contains("radial-gradient")) s += 0.5
            if (lower.contains("animation") || lower.contains("@keyframes")) s += 0.5
            s.coerceIn(0.0, 10.0)
        }

        // Performance
        val perfScore = run {
            var s = 8.0
            val sizeKB = html.length / 1024.0
            if (sizeKB > 100) { s -= 1.0; issues.add("HTML is ${sizeKB.toInt()}KB (large)") }
            if (sizeKB > 500) { s -= 1.0; issues.add("Very large HTML, consider splitting") }
            val scriptCount = "<script".toRegex(RegexOption.IGNORE_CASE).findAll(lower).count()
            if (scriptCount > 5) { s -= 0.5; issues.add("$scriptCount script tags"); autoFix.add("Consolidate scripts") }
            val styleCount = "<style".toRegex(RegexOption.IGNORE_CASE).findAll(lower).count()
            if (styleCount > 3) { s -= 0.5; issues.add("$styleCount style tags"); autoFix.add("Consolidate styles") }
            s.coerceIn(0.0, 10.0)
        }

        // Architecture
        val archScore = run {
            var s = 7.0
            if (lower.contains("class=")) { s += 0.5; issues.add("Using inline classes"); autoFix.add("Extract to CSS classes") }
            if (!lower.contains("<!--")) { s += 0.5 }
            if (lower.contains("<!doctype")) s += 0.5
            if (lower.contains("meta viewport")) s += 0.5
            if (lower.contains("font-family")) s += 0.3
            if (lower.contains("color-scheme") || lower.contains("prefers-color-scheme")) s += 0.5
            s.coerceIn(0.0, 10.0)
        }

        val total = (a11yScore * 0.2 + uiuxScore * 0.25 + visualsScore * 0.25 + perfScore * 0.15 + archScore * 0.15)

        val suggestions = mutableListOf<String>()
        if (a11yScore < 8) suggestions.add("Improve accessibility with ARIA labels and semantic HTML")
        if (uiuxScore < 8) suggestions.add("Add hover/focus states and responsive breakpoints")
        if (visualsScore < 8) suggestions.add("Enhance visual polish with gradients, shadows, and animations")
        if (perfScore < 8) suggestions.add("Optimize HTML size and consolidate scripts/styles")
        if (archScore < 8) suggestions.add("Extract inline styles to external CSS classes")
        if (suggestions.isEmpty()) suggestions.add("Design looks solid. Consider adding dark mode support.")

        return CritiqueResult(
            accessibility = CritiqueDimension(a11yScore, 0.2, issues.filter { it.contains("aria") || it.contains("alt") || it.contains("role") }, autoFix.filter { it.contains("aria") || it.contains("alt") || it.contains("role") }),
            uiux = CritiqueDimension(uiuxScore, 0.25, issues.filter { it.contains("hover") || it.contains("responsive") || it.contains("focus") }, autoFix.filter { it.contains("hover") || it.contains("responsive") || it.contains("focus") }),
            visuals = CritiqueDimension(visualsScore, 0.25, issues.filter { it.contains("gradient") || it.contains("shadow") }, emptyList()),
            performance = CritiqueDimension(perfScore, 0.15, issues.filter { it.contains("KB") || it.contains("script") || it.contains("style") }, autoFix.filter { it.contains("script") || it.contains("style") }),
            architecture = CritiqueDimension(archScore, 0.15, issues.filter { it.contains("inline") || it.contains("class") }, autoFix.filter { it.contains("class") }),
            overall = (total * 10.0 / 10.0).coerceIn(0.0, 10.0),
            suggestions = suggestions
        )
    }

    private fun parseCritiqueJson(json: String): CritiqueResult {
        return try {
            val obj = org.json.JSONObject(json)
            fun parseDim(key: String, weight: Double): CritiqueDimension {
                val d = obj.getJSONObject(key)
                return CritiqueDimension(
                    score = d.getDouble("score"),
                    weight = weight,
                    notes = d.optJSONArray("notes")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList(),
                    autoFixable = d.optJSONArray("autoFixable")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()
                )
            }
            val a = parseDim("accessibility", 0.2)
            val u = parseDim("ui-ux", 0.25)
            val v = parseDim("visuals", 0.25)
            val p = parseDim("performance", 0.15)
            val ar = parseDim("architecture", 0.15)
            val overall = a.weighted + u.weighted + v.weighted + p.weighted + ar.weighted
            val suggestions = obj.optJSONArray("suggestions")?.let { arr -> (0 until arr.length()).map { arr.getString(it) } } ?: emptyList()
            CritiqueResult(a, u, v, p, ar, overall, suggestions)
        } catch (_: Exception) {
            analyzeLocally(json, "")
        }
    }
}
