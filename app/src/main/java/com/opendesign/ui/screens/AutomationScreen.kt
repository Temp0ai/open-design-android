package com.opendesign.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class AutomationWorkflow(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val steps: List<String>,
    val icon: String = "⚡"
)

private val DEFAULT_WORKFLOWS = listOf(
    AutomationWorkflow(
        id = "landing-page",
        name = "Landing Page Generator",
        description = "Generate a complete landing page with hero, features, pricing, and CTA",
        category = "Marketing",
        steps = listOf("Pick design system", "Enter brief", "Generate hero section", "Add features", "Add pricing", "Add CTA", "Review & export")
    ),
    AutomationWorkflow(
        id = "brand-refresh",
        name = "Brand Refresh",
        description = "Refresh an existing design to match a brand spec",
        category = "Design",
        steps = listOf("Upload existing design", "Select DESIGN.md", "Auto-migrate colors", "Update typography", "Apply components", "Export updated design")
    ),
    AutomationWorkflow(
        id = "social-campaign",
        name = "Social Media Campaign",
        description = "Create a set of social media posts with consistent branding",
        category = "Marketing",
        steps = listOf("Pick design system", "Enter campaign brief", "Generate post variations", "Create carousel versions", "Export all as HTML/PNG")
    ),
    AutomationWorkflow(
        id = "dashboard-build",
        name = "Dashboard Builder",
        description = "Build a live dashboard with KPIs and charts",
        category = "Product",
        steps = listOf("Pick design system", "Define KPIs", "Generate layout", "Add charts", "Add filters", "Export or deploy")
    ),
    AutomationWorkflow(
        id = "pitch-deck",
        name = "Pitch Deck Flow",
        description = "Create a complete pitch deck from a brief",
        category = "Business",
        steps = listOf("Pick deck template", "Enter company info", "Generate slides", "Add data visualizations", "Export PPTX/PDF")
    ),
    AutomationWorkflow(
        id = "figma-to-code",
        name = "Figma to Code",
        description = "Import Figma designs and convert to React/Next.js code",
        category = "Engineering",
        steps = listOf("Connect Figma API", "Select frames", "Generate DESIGN.md", "Convert to React components", "Export code")
    ),
    AutomationWorkflow(
        id = "mobile-app-flow",
        name = "Mobile App Prototype",
        description = "Create a multi-screen mobile app prototype",
        category = "Design",
        steps = listOf("Pick device frame", "Define screens", "Generate wireframes", "Add interactions", "Preview & export")
    ),
    AutomationWorkflow(
        id = "content-pipeline",
        name = "Content Pipeline",
        description = "Generate images, videos, and social content in batch",
        category = "Content",
        steps = listOf("Enter content brief", "Generate images", "Create video scripts", "Generate HyperFrames", "Export all assets")
    )
)

@Composable
fun AutomationScreen(
    onWorkflowClick: (AutomationWorkflow) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Marketing", "Design", "Product", "Business", "Engineering", "Content")
    val filtered = if (selectedCategory == "All") DEFAULT_WORKFLOWS
        else DEFAULT_WORKFLOWS.filter { it.category == selectedCategory }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Automation",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reusable workflows for repeat design tasks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

        items(filtered) { workflow ->
            AutomationCard(workflow) { onWorkflowClick(workflow) }
        }
    }
}

@Composable
fun AutomationCard(workflow: AutomationWorkflow, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = workflow.icon, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workflow.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = workflow.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = workflow.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                workflow.steps.take(4).forEach { step ->
                    AssistChip(
                        onClick = {},
                        label = { Text(step, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                }
                if (workflow.steps.size > 4) {
                    Text(
                        text = "+${workflow.steps.size - 4} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}
