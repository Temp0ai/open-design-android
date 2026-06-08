package com.opendesign.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
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
    val designSystems by viewModel.designSystems.collectAsState()
    val skills by viewModel.skills.collectAsState()
    val recentArtifacts by viewModel.recentArtifacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAllDesignSystems by remember { mutableStateOf(false) }
    var showAllSkills by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Design System", "Style", "Brand", "Industry")
    val filteredDesignSystems = if (selectedCategory == "All") designSystems
        else designSystems.filter { it.category.contains(selectedCategory, ignoreCase = true) }

    val filteredSkills = if (selectedCategory == "All") skills
        else skills.filter { it.scenario.contains(selectedCategory, ignoreCase = true) }

    val visibleDesignSystems = if (showAllDesignSystems) filteredDesignSystems else filteredDesignSystems.take(12)
    val visibleSkills = if (showAllSkills) filteredSkills else filteredSkills.take(12)

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
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Skills (${filteredSkills.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (filteredSkills.size > 12) {
                        TextButton(onClick = { showAllSkills = !showAllSkills }) {
                            Text(if (showAllSkills) "Show Less" else "View All")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    visibleSkills.forEach { skill ->
                        SkillCard(skill) { onSkillClick(skill) }
                    }
                }
            }
        }

        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Design Systems (${filteredDesignSystems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (filteredDesignSystems.size > 12) {
                        TextButton(onClick = { showAllDesignSystems = !showAllDesignSystems }) {
                            Text(if (showAllDesignSystems) "Show Less" else "View All")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(visibleDesignSystems.chunked(2)) { row ->
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

@Composable
fun SkillCard(skill: Skill, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = when {
                    skill.slug.contains("web") || skill.slug.contains("prototype") -> "\uD83C\uDF10"
                    skill.slug.contains("mobile") || skill.slug.contains("app") -> "\uD83D\uDCF1"
                    skill.slug.contains("dashboard") || skill.slug.contains("data") -> "\uD83D\uDCCA"
                    skill.slug.contains("deck") || skill.slug.contains("slide") || skill.slug.contains("pitch") -> "\uD83D\uDCC1"
                    skill.slug.contains("social") || skill.slug.contains("ad") -> "\uD83D\uDCE3"
                    skill.slug.contains("logo") || skill.slug.contains("brand") -> "\uD83C\uDFA8"
                    skill.slug.contains("doc") || skill.slug.contains("report") -> "\uD83D\uDCDD"
                    skill.slug.contains("figma") -> "\uD83C\uDFA8"
                    skill.slug.contains("music") || skill.slug.contains("video") -> "\uD83C\uDFA5"
                    skill.slug.contains("3d") || skill.slug.contains("fal") -> "\uD83D\uDD2D"
                    else -> "\u2728"
                },
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = skill.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (skill.description.isNotBlank()) {
                Text(
                    text = skill.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (system.description.isNotBlank()) {
                    Text(
                        text = system.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
