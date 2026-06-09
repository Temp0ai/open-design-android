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
    val score: Double,
    val weight: Double,
    val notes: List<String>,
    val autoFixable: List<String>
) {
    val grade get() = when {
        score >= 9.0 -> "A+"
        score >= 8.0 -> "A"
        score >= 7.0 -> "B"
        score >= 6.0 -> "C"
        score >= 5.0 -> "D"
        else -> "F"
    }
}

object CritiqueEngine {

    fun critique(html: String, brief: String = ""): CritiqueResult {
        val lower = html.lowercase()
        val issues = mutableListOf<String>()
        val autoFix = mutableListOf<String>()

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

        val uiuxScore = run {
            var s = 7.0
            if (!lower.contains("hover") && !lower.contains(":focus")) { s -= 1.0; issues.add("No hover/focus states"); autoFix.add("Add hover and focus styles") }
            if (lower.contains("cursor: pointer")) s += 0.5
            if (lower.contains("transition")) s += 1.0
            if (lower.contains("@media")) s += 0.5
            if (!lower.contains("@media")) { issues.add("Not responsive"); autoFix.add("Add responsive media queries") }
            s.coerceIn(0.0, 10.0)
        }

        val visualsScore = run {
            var s = 7.5
            if (lower.contains("background-color") || lower.contains("background:")) s += 0.5
            if (lower.contains("border-radius")) s += 0.5
            if (lower.contains("box-shadow") || lower.contains("text-shadow")) s += 0.5
            if (lower.contains("linear-gradient") || lower.contains("radial-gradient")) s += 0.5
            if (lower.contains("animation") || lower.contains("@keyframes")) s += 0.5
            s.coerceIn(0.0, 10.0)
        }

        val perfScore = run {
            var s = 8.0
            val sizeKB = html.length / 1024.0
            if (sizeKB > 100) { s -= 1.0; issues.add("HTML is ${sizeKB.toInt()}KB (large)") }
            if (sizeKB > 500) { s -= 1.0; issues.add("Very large HTML") }
            val scriptCount = "<script".toRegex(RegexOption.IGNORE_CASE).findAll(lower).count()
            if (scriptCount > 5) { s -= 0.5; issues.add("$scriptCount script tags"); autoFix.add("Consolidate scripts") }
            s.coerceIn(0.0, 10.0)
        }

        val archScore = run {
            var s = 7.0
            if (!lower.contains("class=")) { s += 0.5 }
            if (lower.contains("<!doctype")) s += 0.5
            if (lower.contains("meta viewport")) s += 0.5
            if (lower.contains("font-family")) s += 0.3
            if (lower.contains("prefers-color-scheme")) s += 0.5
            s.coerceIn(0.0, 10.0)
        }

        val total = a11yScore * 0.2 + uiuxScore * 0.25 + visualsScore * 0.25 + perfScore * 0.15 + archScore * 0.15

        val suggestions = mutableListOf<String>()
        if (a11yScore < 8) suggestions.add("Improve accessibility with ARIA labels and semantic HTML")
        if (uiuxScore < 8) suggestions.add("Add hover/focus states and responsive breakpoints")
        if (visualsScore < 8) suggestions.add("Enhance visual polish with gradients, shadows, and animations")
        if (perfScore < 8) suggestions.add("Optimize HTML size and consolidate scripts")
        if (archScore < 8) suggestions.add("Extract inline styles to external CSS classes")
        if (suggestions.isEmpty()) suggestions.add("Design looks solid. Consider adding dark mode support.")

        return CritiqueResult(
            accessibility = CritiqueDimension(a11yScore, 0.2, issues.filter { it.contains("aria") || it.contains("alt") || it.contains("role") }, autoFix.filter { it.contains("aria") || it.contains("alt") || it.contains("role") }),
            uiux = CritiqueDimension(uiuxScore, 0.25, issues.filter { it.contains("hover") || it.contains("responsive") || it.contains("focus") }, autoFix.filter { it.contains("hover") || it.contains("responsive") || it.contains("focus") }),
            visuals = CritiqueDimension(visualsScore, 0.25, emptyList(), emptyList()),
            performance = CritiqueDimension(perfScore, 0.15, issues.filter { it.contains("KB") || it.contains("script") }, autoFix.filter { it.contains("script") }),
            architecture = CritiqueDimension(archScore, 0.15, emptyList(), emptyList()),
            overall = total.coerceIn(0.0, 10.0),
            suggestions = suggestions
        )
    }
}
