package com.seuapp.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.seuapp.music.ui.components.TrackItem
import com.seuapp.music.ui.theme.*
import com.seuapp.music.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: MusicViewModel, onTrackClick: () -> Unit) {
    val query by viewModel.query.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val loading by viewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current

    Column(
        Modifier.fillMaxSize().background(DeepNavy).padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50))
                .background(SurfaceHigh)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(50))
                .padding(horizontal = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = ElectricCyan, modifier = Modifier.padding(start = 16.dp).size(22.dp))
                OutlinedTextField(
                    value = query, onValueChange = viewModel::onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Artistas, músicas ou gêneros...", color = MutedSteel) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = ElectricCyan, focusedTextColor = OffWhite, unfocusedTextColor = OffWhite
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.search()
                        focusManager.clearFocus()
                    })
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = ElectricCyan)
        } else if (tracks.isEmpty()) {
            Text("Buscas Recentes", style = MaterialTheme.typography.titleMedium, color = OffWhite, modifier = Modifier.padding(bottom = 12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    RecentChip("Neon Genesis") { viewModel.onQueryChange("Neon Genesis"); viewModel.search() }
                }
                item {
                    RecentChip("Synthwave 2084") { viewModel.onQueryChange("Synthwave 2084"); viewModel.search() }
                }
                item {
                    RecentChip("Cyber Grind") { viewModel.onQueryChange("Cyber Grind"); viewModel.search() }
                }
            }
        } else {
            Text("Resultados", style = MaterialTheme.typography.titleMedium, color = OffWhite, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(tracks) { track ->
                    TrackItem(track = track) { viewModel.playTrack(track); onTrackClick() }
                }
            }
        }
    }
}

@Composable
private fun RecentChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = RoundedCornerShape(50), color = NavyGlass,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = MutedSteel, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = SteelText)
        }
    }
}
