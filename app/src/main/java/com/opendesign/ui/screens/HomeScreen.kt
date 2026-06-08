package com.opendesign.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.opendesign.data.model.DesignSystem
import com.opendesign.data.model.Skill
import com.opendesign.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onSkillClick: (Skill) -> Unit = {},
    onDesignSystemClick: (DesignSystem) -> Unit = {},
    onSearch: (String) -> Unit = {}
) {
    val projects by viewModel.projects.collectAsState()
    val recentArtifacts by viewModel.recentArtifacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Open Design",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI-Powered Design Studio",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe what you want to create...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { 
                            onSearch(searchQuery)
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Search")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                )
            )
        }

        item {
            Column {
                Text(
                    text = "Skills",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DEFAULT_SKILLS.forEach { skill ->
                        SkillCard(skill) { onSkillClick(skill) }
                    }
                }
            }
        }

        item {
            Column {
                Text(
                    text = "Design Systems",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(DEFAULT_DESIGN_SYSTEMS.chunked(2)) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { system ->
                    DesignSystemCard(system, Modifier.weight(1f)) { onDesignSystemClick(system) }
                }
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        if (recentArtifacts.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Recent Projects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(recentArtifacts) { artifact ->
                ArtifactCard(artifact)
            }
        } else {
            item {
                Column {
                    Text(
                        text = "Recent Projects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No projects yet. Create your first design!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val DEFAULT_SKILLS = listOf(
    Skill("web-prototype", "Web Prototype", "web-prototype", "prototype", "design", "24 templates"),
    Skill("mobile-app", "Mobile App", "mobile-app", "prototype", "design", "18 templates"),
    Skill("dashboard", "Dashboard", "dashboard", "prototype", "design", "12 templates"),
    Skill("pitch-deck", "Pitch Deck", "pitch-deck", "deck", "marketing", "15 templates"),
    Skill("social-post", "Social Post", "social-post", "image", "marketing", "8 templates"),
    Skill("logo", "Logo Design", "logo", "image", "design", "6 templates"),
)

private val DEFAULT_DESIGN_SYSTEMS = listOf(
    DesignSystem("linear", "Linear", "linear", "Clean, minimal SaaS", color = "#5E6AD2"),
    DesignSystem("stripe", "Stripe", "stripe", "Modern fintech", color = "#635BFF"),
    DesignSystem("vercel", "Vercel", "vercel", "Developer-first", color = "#000000"),
    DesignSystem("airbnb", "Airbnb", "airbnb", "Warm, friendly", color = "#FF5A5F"),
    DesignSystem("apple", "Apple", "apple", "Premium, refined", color = "#1D1D1F"),
    DesignSystem("notion", "Notion", "notion", "Productivity", color = "#000000"),
    DesignSystem("figma", "Figma", "figma", "Creative tools", color = "#A259FF"),
    DesignSystem("supabase", "Supabase", "supabase", "Open source", color = "#3ECF8E"),
)

@Composable
fun SkillCard(skill: Skill, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when (skill.slug) {
                    "web-prototype" -> "\uD83C\uDF10"
                    "mobile-app" -> "\uD83D\uDCF1"
                    "dashboard" -> "\uD83D\uDCCA"
                    "pitch-deck" -> "\uD83D\uDCC1"
                    "social-post" -> "\uD83D\uDCE3"
                    "logo" -> "\uD83C\uDFA8"
                    else -> "\u2728"
                },
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = skill.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = skill.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DesignSystemCard(system: DesignSystem, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val color = try {
        Color(android.graphics.Color.parseColor(system.color))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = system.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = system.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
