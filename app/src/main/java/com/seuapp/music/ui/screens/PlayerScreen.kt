package com.seuapp.music.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.seuapp.music.ui.theme.*
import com.seuapp.music.ui.viewmodel.MusicViewModel

@Composable
fun PlayerScreen(viewModel: MusicViewModel, onBack: () -> Unit) {
    val track by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val shuffle by viewModel.shuffle.collectAsState()
    val repeat by viewModel.repeat.collectAsState()
    val progress by viewModel.progress.collectAsState()

    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = EaseInOutCubic), repeatMode = RepeatMode.Reverse)
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = EaseInOutCubic), repeatMode = RepeatMode.Reverse)
    )

    if (track == null) {
        Box(Modifier.fillMaxSize().background(DeepNavy), contentAlignment = Alignment.Center) {
            Text("Nenhuma música tocando", color = SteelText)
        }
        return
    }

    val current = track!!
    val isFav = viewModel.isFavorite(current)

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(NeonPurple.copy(alpha = 0.25f), ElectricCyanDim.copy(alpha = 0.12f), DeepNavy),
                center = androidx.compose.ui.geometry.Offset(300f, 500f), radius = 900f
            )
        )
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(onClick = onBack, shape = CircleShape, color = NavyGlass.copy(alpha = 0.6f)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", modifier = Modifier.padding(4.dp).size(28.dp), tint = OffWhite)
                }
                Text("Tocando Agora", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                Surface(onClick = { viewModel.toggleFavorite(current) }, shape = CircleShape, color = NavyGlass.copy(alpha = 0.6f)) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito",
                        modifier = Modifier.padding(4.dp).size(28.dp), tint = if (isFav) SoftCoral else OffWhite
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(220.dp).padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(200.dp).scale(scale).background(ElectricCyan.copy(alpha = haloAlpha), CircleShape))
                Box(Modifier.size(180.dp).scale(scale * 0.7f).background(NeonPurple.copy(alpha = haloAlpha * 0.5f), CircleShape))
                AsyncImage(model = current.thumbnail, contentDescription = null, modifier = Modifier.size(200.dp).clip(RoundedCornerShape(24.dp)))
            }

            Spacer(Modifier.height(16.dp))

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(current.title, style = MaterialTheme.typography.headlineMedium, color = OffWhite, textAlign = TextAlign.Center, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(current.channel, style = MaterialTheme.typography.bodyLarge, color = SteelText)
            }

            Spacer(Modifier.height(12.dp))

            Column {
                Surface(Modifier.fillMaxWidth().height(4.dp), shape = RoundedCornerShape(50), color = SurfaceHighest) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(NeonPurple, ElectricCyan)), RoundedCornerShape(50)))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMs((progress * playerDurationOrDefault(viewModel)).toLong()), style = MaterialTheme.typography.labelSmall, color = MutedSteel)
                    Text(formatMs(playerDurationOrDefault(viewModel)), style = MaterialTheme.typography.labelSmall, color = MutedSteel)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::toggleShuffle, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.Shuffle, "Aleatório", tint = if (shuffle) ElectricCyan else MutedSteel, modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = viewModel::prev, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Anterior", tint = OffWhite, modifier = Modifier.size(42.dp))
                }
                Surface(onClick = viewModel::togglePlayPause, modifier = Modifier.size(80.dp), shape = CircleShape, color = Color.Transparent) {
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(ElectricCyan, NeonPurple)), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", modifier = Modifier.size(44.dp), tint = OnCyanDark)
                    }
                }
                IconButton(onClick = viewModel::next, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Default.SkipNext, "Próxima", tint = OffWhite, modifier = Modifier.size(42.dp))
                }
                IconButton(onClick = viewModel::toggleRepeat, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.Repeat, "Repetir", tint = if (repeat) ElectricCyan else MutedSteel, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

private fun playerDurationOrDefault(vm: MusicViewModel): Long = vm.player.duration.takeIf { it > 0 } ?: 1L
