package com.opendesign.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opendesign.data.model.Skill
import com.opendesign.ui.viewmodel.CreateViewModel

private val STYLE_OPTIONS = listOf(
    Triple("Minimal", Color(0xFF1A1A1A), "minimal"),
    Triple("Bold", Color(0xFFE74C3C), "bold"),
    Triple("Gradient", Color(0xFF6C5CE7), "gradient"),
    Triple("Neon", Color(0xFF00D2FF), "neon"),
    Triple("Elegant", Color(0xFFC4A882), "elegant"),
    Triple("Playful", Color(0xFFFF6B6B), "playful"),
)

@Composable
fun CreateScreen(viewModel: CreateViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val designSystems by viewModel.designSystems.collectAsState()

    if (uiState.generatedHtml != null) {
        PreviewScreen(
            html = uiState.generatedHtml!!,
            onBack = { viewModel.clearResult() }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Create Design",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Describe your vision, AI brings it to life",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Column {
                Text(
                    text = "What to create?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(skills) { skill ->
                        FilterChip(
                            selected = uiState.selectedSkillId == skill.id,
                            onClick = { viewModel.selectSkill(skill.id) },
                            label = { Text(skill.name) },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Describe it",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.prompt,
                    onValueChange = { viewModel.updatePrompt(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("A modern SaaS landing page with pricing, testimonials, and hero CTA...")
                    },
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        item {
            Column {
                Text(
                    text = "Design Style",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    STYLE_OPTIONS.take(3).forEach { (name, color, id) ->
                        StyleCard(
                            name = name,
                            color = color,
                            selected = uiState.selectedStyleId == id,
                            onClick = { viewModel.selectStyle(id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    STYLE_OPTIONS.drop(3).forEach { (name, color, id) ->
                        StyleCard(
                            name = name,
                            color = color,
                            selected = uiState.selectedStyleId == id,
                            onClick = { viewModel.selectStyle(id) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Design System",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                val selectedName = designSystems.find { it.slug == uiState.selectedDesignSystemSlug }?.name ?: "Linear"
                OutlinedTextField(
                    value = selectedName,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        item {
            if (uiState.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.generate() },
                enabled = uiState.prompt.isNotBlank() && uiState.selectedSkillId != null && !uiState.isGenerating,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (uiState.isGenerating) "Generating..." else "Generate Design",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (uiState.isGenerating && uiState.streamingText != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = uiState.streamingText!!.takeLast(500),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StyleCard(
    name: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = androidx.compose.ui.graphics.SolidColor(color)
        ) else CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(color, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
