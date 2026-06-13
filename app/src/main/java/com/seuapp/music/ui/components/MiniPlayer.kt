package com.seuapp.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.seuapp.music.data.model.Track
import com.seuapp.music.ui.theme.*

@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlayPause: () -> Unit,
    onFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        color = NavyGlass.copy(alpha = 0.75f), tonalElevation = 8.dp
    ) {
        Box {
            Box(
                modifier = Modifier.fillMaxWidth(0.35f).height(2.dp)
                    .background(Brush.horizontalGradient(listOf(ElectricCyan, NeonPurple)), RoundedCornerShape(14.dp))
            )
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = track.thumbnail, contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.bodyMedium, color = ElectricCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.channel, style = MaterialTheme.typography.labelSmall, color = SteelText, maxLines = 1)
                }
                IconButton(onClick = onFavorite) {
                    Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito",
                        tint = if (isFavorite) SoftCoral else MutedSteel, modifier = Modifier.size(22.dp))
                }
                Surface(onClick = onPlayPause, shape = CircleShape, modifier = Modifier.size(40.dp), color = ElectricCyan) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", modifier = Modifier.size(22.dp), tint = OnCyanDark)
                }
            }
        }
    }
}
