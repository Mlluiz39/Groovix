package com.seuapp.music.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.seuapp.music.MainActivity
import com.seuapp.music.ui.components.MiniPlayer
import com.seuapp.music.ui.screens.HomeScreen
import com.seuapp.music.ui.screens.PlayerScreen
import com.seuapp.music.ui.screens.PlaylistScreen
import com.seuapp.music.ui.screens.RegisterScreen
import com.seuapp.music.ui.screens.SearchScreen
import com.seuapp.music.ui.screens.SettingsScreen
import com.seuapp.music.ui.theme.*
import com.seuapp.music.ui.viewmodel.MusicViewModel

sealed class Screen(val route: String) {
    object Register : Screen("register")
    object Home : Screen("home")
    object Search : Screen("search")
    object Player : Screen("player")
    object Playlists : Screen("playlists")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: MusicViewModel = viewModel()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val activity = context as? MainActivity
        activity?.getService()?.let { service ->
            viewModel.initServiceConnection(service)
        }
    }

    val startDestination = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isLoggedIn, isLoading) {
        if (!isLoading && startDestination.value == null) {
            startDestination.value = if (isLoggedIn) Screen.Home.route else Screen.Register.route
        }
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && startDestination.value == Screen.Register.route) {
            startDestination.value = Screen.Home.route
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Register.route) { inclusive = true }
            }
        }
    }

    if (startDestination.value == null) {
        Box(Modifier.fillMaxSize().background(DeepNavy))
        return
    }

    Scaffold(
        containerColor = DeepNavy,
        bottomBar = {
            if (isLoggedIn) {
                Column {
                    if (currentTrack != null) {
                        MiniPlayer(
                            track = currentTrack!!, isPlaying = isPlaying,
                            isFavorite = viewModel.isFavorite(currentTrack!!),
                            onPlayPause = viewModel::togglePlayPause,
                            onFavorite = { viewModel.toggleFavorite(currentTrack!!) },
                            onClick = { navController.navigate(Screen.Player.route) }
                        )
                    }
                    NavigationBar(containerColor = NavyGlass.copy(alpha = 0.85f), tonalElevation = 0.dp) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        listOf(
                            Triple(Screen.Home, "Início", Icons.Default.Home),
                            Triple(Screen.Search, "Buscar", Icons.Default.Search),
                            Triple(Screen.Playlists, "Biblioteca", Icons.Default.LibraryMusic),
                        ).forEach { (screen, label, icon) ->
                            NavigationBarItem(
                                selected = currentRoute == screen.route,
                                onClick = { navController.navigate(screen.route) { popUpTo(0) } },
                                icon = { Icon(icon, null, tint = if (currentRoute == screen.route) ElectricCyan else MutedSteel) },
                                label = { Text(label, color = if (currentRoute == screen.route) ElectricCyan else MutedSteel) },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination.value!!,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Register.route) {
                RegisterScreen { name, email, password ->
                    viewModel.registerUser(name, email, password)
                }
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Search.route) { SearchScreen(viewModel) { navController.navigate(Screen.Player.route) } }
            composable(Screen.Player.route) { PlayerScreen(viewModel, onBack = { navController.popBackStack() }) }
            composable(Screen.Playlists.route) {
                PlaylistScreen(viewModel) { track -> viewModel.playTrack(track); navController.navigate(Screen.Player.route) }
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
