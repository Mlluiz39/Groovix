package com.seuapp.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.seuapp.music.data.model.Track
import com.seuapp.music.ui.theme.*
import com.seuapp.music.ui.viewmodel.MusicViewModel
import java.util.Calendar

@Composable
fun HomeScreen(viewModel: MusicViewModel, onNavigateToSearch: () -> Unit, onNavigateToSettings: () -> Unit = {}) {
    val recentTracks by viewModel.recentTracks.collectAsState()
    val userName by viewModel.userName.collectAsState()

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Bom Dia,"
            hour < 18 -> "Boa Tarde,"
            else -> "Boa Noite,"
        }
    }

    val displayName = if (userName.isNotBlank()) userName else "Neon Rider"

    val playlistsEmAlta = listOf(
        Triple("Top Brasil 2026", NeonPurple, Brush.horizontalGradient(listOf(NeonPurple, SoftPurple))),
        Triple("Top Brasil 2025", ElectricCyan, Brush.horizontalGradient(listOf(ElectricCyanDim, ElectricCyan))),
        Triple("Funk Hits", SoftCoral, Brush.horizontalGradient(listOf(SoftCoral, CoralDim))),
        Triple("Samba e Pagode", ElectricCyan, Brush.horizontalGradient(listOf(ElectricCyan, NeonPurple))),
        Triple("Top Spotify 2026", NeonPurple, Brush.horizontalGradient(listOf(SoftPurple, NeonPurple))),
        Triple("Top Spotify 2025", ElectricCyan, Brush.horizontalGradient(listOf(ElectricCyanDim, ElectricCyan))),
        Triple("Melhores de 2026", SoftCoral, Brush.horizontalGradient(listOf(CoralDim, SoftCoral))),
        Triple("Melhores de 2025", NeonPurple, Brush.horizontalGradient(listOf(NeonPurple, ElectricCyan))),
        Triple("Rock Clássico", ElectricCyan, Brush.horizontalGradient(listOf(ElectricCyan, ElectricCyanDim))),
        Triple("Eletrônica", NeonPurple, Brush.horizontalGradient(listOf(NeonPurple, SoftPurple))),
        Triple("Hip Hop", SoftCoral, Brush.horizontalGradient(listOf(SoftCoral, CoralDim))),
        Triple("Sertanejo", ElectricCyan, Brush.horizontalGradient(listOf(ElectricCyanDim, ElectricCyan))),
        Triple("MPB", NeonPurple, Brush.horizontalGradient(listOf(SoftPurple, NeonPurple))),
        Triple("Indie", ElectricCyan, Brush.horizontalGradient(listOf(ElectricCyan, NeonPurple))),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DeepNavy),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(greeting, style = MaterialTheme.typography.bodyLarge, color = SteelText)
                    Text(displayName, style = MaterialTheme.typography.headlineLarge, color = ElectricCyan)
                }
                Surface(
                    onClick = onNavigateToSettings,
                    shape = CircleShape,
                    color = NavyGlass.copy(alpha = 0.6f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Settings, "Configurações", tint = OffWhite, modifier = Modifier.padding(10.dp))
                }
            }
        }

        item {
            Box(
                modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(220.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(NeonPurple.copy(alpha = 0.3f), ElectricCyanDim.copy(alpha = 0.15f), DeepNavy))
                    )
                )
                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                    Surface(color = NeonPurple.copy(alpha = 0.15f), shape = RoundedCornerShape(50), modifier = Modifier.padding(bottom = 12.dp)) {
                        Text("NOVO LANÇAMENTO", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = NeonPurple)
                    }
                    Text("MÚSICAS DO BRASIL 2026", style = MaterialTheme.typography.titleLarge, color = OffWhite)
                    Text("As melhores do ano", style = MaterialTheme.typography.bodyMedium, color = SteelText)
                }
                Surface(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).size(48.dp),
                    shape = CircleShape, color = NeonPurple,
                    onClick = { viewModel.onQueryChange("músicas do brasil 2026"); viewModel.search(); onNavigateToSearch() }
                ) {
                    Icon(Icons.Default.PlayArrow, "Tocar", tint = OffWhite, modifier = Modifier.padding(12.dp))
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }

        if (recentTracks.isNotEmpty()) {
            item {
                Text("Tocadas Recentemente", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium, color = OffWhite)
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentTracks) { track ->
                        RecentTrackCard(track = track) {
                            viewModel.playQueue(recentTracks, track)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }

        item {
            Text("Playlists em Alta", modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium, color = OffWhite)
        }

        items(playlistsEmAlta.chunked(2)) { row ->
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (label, color, gradient) ->
                    Surface(
                        onClick = { viewModel.onQueryChange(label); viewModel.search(); onNavigateToSearch() },
                        shape = RoundedCornerShape(12.dp), color = Color.Transparent,
                        modifier = Modifier.weight(1f).height(80.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                                .background(gradient, RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(label, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.labelSmall, color = color)
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RecentTrackCard(track: Track, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = NavyGlass,
        tonalElevation = 2.dp,
        modifier = Modifier.width(140.dp).height(80.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = track.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            ) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = OffWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.channel,
                    style = MaterialTheme.typography.labelSmall,
                    color = SteelText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
