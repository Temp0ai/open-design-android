package com.opendesign.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.opendesign.data.model.Artifact
import com.opendesign.ui.viewmodel.GalleryViewModel

private val FILTERS = listOf("All", "Prototypes", "Dashboards", "Decks", "Images")

@Composable
fun GalleryScreen(viewModel: GalleryViewModel = viewModel()) {
    val activeFilter by viewModel.activeFilter.collectAsState()
    val filteredArtifacts by viewModel.filteredArtifacts.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gallery",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your generated designs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            modifier = Modifier.padding(start = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FILTERS) { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { viewModel.setFilter(filter) },
                    label = { Text(filter) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredArtifacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No designs yet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create your first design in the Create tab",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredArtifacts.chunked(2)) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { artifact ->
                            ArtifactCard(artifact, Modifier.weight(1f))
                        }
                        if (row.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtifactCard(artifact: Artifact, modifier: Modifier = Modifier) {
    val icon = when (artifact.mode) {
        "prototype" -> "\uD83C\uDF10"
        "deck" -> "\uD83D\uDCD1"
        "image" -> "\uD83C\uDFA8"
        else -> "\uD83D\uDCF1"
    }

    val color = when (artifact.designSystemName) {
        "Linear" -> Color(0xFF5E6AD2)
        "Stripe" -> Color(0xFF635BFF)
        "Vercel" -> Color(0xFF000000)
        "Airbnb" -> Color(0xFFFF5A5F)
        "Apple" -> Color(0xFF1D1D1F)
        "Notion" -> Color(0xFF000000)
        "Figma" -> Color(0xFFA259FF)
        "Supabase" -> Color(0xFF3ECF8E)
        else -> Color(0xFF6C5CE7)
    }

    Card(
        modifier = modifier.clickable { },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, style = MaterialTheme.typography.headlineLarge)
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = artifact.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${artifact.designSystemName} \u00B7 ${artifact.mode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
