package com.seuapp.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.seuapp.music.data.model.Playlist
import com.seuapp.music.data.model.Track
import com.seuapp.music.ui.theme.*
import com.seuapp.music.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(viewModel: MusicViewModel, onTrackClick: (Track) -> Unit) {
    val playlists by viewModel.playlists.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var showAddToDialog by remember { mutableStateOf<Track?>(null) }
    var newName by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    if (selectedPlaylist != null) {
        PlaylistDetailScreen(
            playlist = selectedPlaylist!!,
            viewModel = viewModel,
            onBack = { selectedPlaylist = null },
            onTrackClick = onTrackClick
        )
        return
    }

    Box(Modifier.fillMaxSize().background(DeepNavy)) {
        LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Biblioteca", style = MaterialTheme.typography.headlineLarge, color = OffWhite)
                    Surface(onClick = { showSaveDialog = true }, shape = CircleShape, color = ElectricCyan) {
                        Icon(Icons.Default.Add, "Nova Playlist", tint = OnCyanDark, modifier = Modifier.padding(8.dp).size(24.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = ElectricCyan) {
                    Tab(tab == 0, onClick = { tab = 0 }, text = { Text("Playlists", color = if (tab == 0) ElectricCyan else MutedSteel) })
                    Tab(tab == 1, onClick = { tab = 1 }, text = { Text("Favoritos", color = if (tab == 1) ElectricCyan else MutedSteel) })
                }
            }

            if (tab == 0) {
                if (playlists.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.LibraryMusic, null, tint = MutedSteel, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Nenhuma playlist ainda.\nCrie uma e adicione músicas!", style = MaterialTheme.typography.bodyLarge, color = SteelText, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(playlists) { playlist ->
                        PlaylistCard(playlist,
                            onClick = { selectedPlaylist = playlist },
                            onDelete = { viewModel.deletePlaylist(playlist.id) })
                    }
                }
            } else {
                if (favorites.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FavoriteBorder, null, tint = MutedSteel, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Toque no ♡ em qualquer música!", style = MaterialTheme.typography.bodyLarge, color = SteelText, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(favorites) { track ->
                        TrackCard(track, onClick = { viewModel.playQueue(favorites, track); onTrackClick(track) },
                            onFavorite = { viewModel.toggleFavorite(track) }, isFav = true,
                            onAddToPlaylist = { showAddToDialog = track })
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = SteelNavy, titleContentColor = OffWhite, textContentColor = SteelText,
            title = { Text("Nova Playlist") },
            text = {
                Column {
                    Text("Crie uma playlist vazia e adicione músicas depois.", style = MaterialTheme.typography.bodyMedium, color = SteelText)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        placeholder = { Text("Nome da playlist", color = MutedSteel) }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricCyan, unfocusedBorderColor = MutedSteel,
                            focusedTextColor = OffWhite, unfocusedTextColor = OffWhite, cursorColor = ElectricCyan)
                    )
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.savePlaylist(newName); newName = ""; showSaveDialog = false }) { Text("Criar", color = ElectricCyan) } },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar", color = SteelText) } }
        )
    }

    showAddToDialog?.let { trk ->
        AlertDialog(
            onDismissRequest = { showAddToDialog = null },
            containerColor = SteelNavy, titleContentColor = OffWhite, textContentColor = SteelText,
            title = { Text("Adicionar à playlist", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    Text(trk.title, style = MaterialTheme.typography.bodyMedium, color = ElectricCyan)
                    Spacer(Modifier.height(12.dp))
                    if (playlists.isEmpty()) {
                        Text("Nenhuma playlist criada ainda.", style = MaterialTheme.typography.bodyMedium, color = SteelText)
                    } else {
                        playlists.forEach { p ->
                            Surface(onClick = {
                                viewModel.addTrackToPlaylist(p.id, trk)
                                showAddToDialog = null
                            }, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                                Text(p.name, modifier = Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = OffWhite)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddToDialog = null }) { Text("Cancelar", color = SteelText) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailScreen(
    playlist: Playlist,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onTrackClick: (Track) -> Unit
) {
    Box(Modifier.fillMaxSize().background(DeepNavy)) {
        LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(onClick = onBack, shape = CircleShape, color = NavyGlass.copy(alpha = 0.6f)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", modifier = Modifier.padding(8.dp).size(24.dp), tint = OffWhite)
                    }
                    Text(playlist.name, style = MaterialTheme.typography.titleLarge, color = OffWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Surface(shape = CircleShape, color = NavyGlass.copy(alpha = 0.6f)) {
                        Icon(Icons.Default.MoreVert, "Mais", modifier = Modifier.padding(8.dp).size(24.dp), tint = OffWhite)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("${playlist.tracks.size} músicas", style = MaterialTheme.typography.bodyMedium, color = SteelText)
                Spacer(Modifier.height(8.dp))

                if (playlist.tracks.isNotEmpty()) {
                    Surface(
                        onClick = { viewModel.loadPlaylistTracks(playlist); onTrackClick(playlist.tracks.first()) },
                        shape = RoundedCornerShape(50),
                        color = ElectricCyan,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = OnCyanDark, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tocar Playlist", style = MaterialTheme.typography.titleMedium, color = OnCyanDark)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            if (playlist.tracks.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QueueMusic, null, tint = MutedSteel, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Playlist vazia", style = MaterialTheme.typography.titleMedium, color = SteelText)
                            Spacer(Modifier.height(8.dp))
                            Text("Adicione músicas pela tela de busca ou favoritos", style = MaterialTheme.typography.bodyMedium, color = MutedSteel, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(playlist.tracks) { track ->
                    PlaylistTrackCard(track = track,
                        onClick = { viewModel.playQueue(playlist.tracks, track); onTrackClick(track) },
                        onRemove = { viewModel.removeTrackFromPlaylist(playlist.id, track) })
                }
            }
        }
    }
}

@Composable
private fun PlaylistCard(playlist: Playlist, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = NavyGlass, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = NeonPurple.copy(alpha = 0.2f), modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.QueueMusic, null, tint = ElectricCyan, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = OffWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${playlist.tracks.size} músicas", style = MaterialTheme.typography.bodyMedium, color = SteelText)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Excluir", tint = ErrorRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PlaylistTrackCard(track: Track, onClick: () -> Unit, onRemove: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = NavyGlass.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = track.thumbnail, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.bodyMedium, color = OffWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.channel, style = MaterialTheme.typography.labelSmall, color = SteelText, maxLines = 1)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.RemoveCircleOutline, "Remover", tint = ErrorRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun TrackCard(track: Track, onClick: () -> Unit, onFavorite: () -> Unit, isFav: Boolean, onAddToPlaylist: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = NavyGlass.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = track.thumbnail, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.bodyMedium, color = OffWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.channel, style = MaterialTheme.typography.labelSmall, color = SteelText, maxLines = 1)
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(Icons.Default.PlaylistAdd, "Adicionar à playlist", tint = MutedSteel, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onFavorite) {
                Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito",
                    tint = if (isFav) SoftCoral else MutedSteel, modifier = Modifier.size(20.dp))
            }
        }
    }
}
