package com.daphile.remote.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.daphile.remote.DaphileApp
import com.daphile.remote.R
import com.daphile.remote.data.model.ConnectionState
import com.daphile.remote.ui.common.ConnectionBanner
import com.daphile.remote.ui.navigation.Routes
import com.daphile.remote.ui.screens.AlbumDetailScreen
import com.daphile.remote.ui.screens.AlbumsScreen
import com.daphile.remote.ui.screens.AudioOutputScreen
import com.daphile.remote.ui.screens.BrowseScreen
import com.daphile.remote.ui.screens.BrowserScreen
import com.daphile.remote.ui.screens.FolderBrowserScreen
import com.daphile.remote.ui.screens.LibraryScreen
import com.daphile.remote.ui.screens.LyricsScreen
import com.daphile.remote.ui.screens.NowPlayingScreen
import com.daphile.remote.ui.screens.PlayersScreen
import com.daphile.remote.ui.screens.PlaylistDetailScreen
import com.daphile.remote.ui.screens.RadioScreen
import com.daphile.remote.ui.screens.ServerEditScreen
import com.daphile.remote.ui.screens.ServerListScreen
import com.daphile.remote.ui.screens.SettingsScreen
import com.daphile.remote.ui.screens.SongsScreen
import com.daphile.remote.ui.screens.SystemInfoScreen

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(Routes.NOW, "正在播放", Icons.Rounded.PlayCircle),
    BottomTab(Routes.LIBRARY, "音乐库", Icons.Rounded.LibraryMusic),
    BottomTab(Routes.RADIO, "电台与收藏", Icons.Rounded.Radio),
    BottomTab(Routes.PLAYERS, "播放器", Icons.Rounded.Speaker),
    BottomTab(Routes.SETTINGS, "设置", Icons.Rounded.Settings)
)

@Composable
fun DaphileRemoteApp() {
    val context = LocalContext.current
    val graph = (context.applicationContext as DaphileApp).graph
    val servers by graph.settings.servers.collectAsState(initial = null)
    val appSettings by graph.repository.appSettings.collectAsStateWithLifecycle()
    val connection by graph.repository.connection.collectAsStateWithLifecycle()
    val server by graph.repository.server.collectAsStateWithLifecycle()

    val dark = when (appSettings.theme) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    SystemBars(dark = dark)

    if (servers == null) {
        SplashScreen()
        return
    }

    val startDestination = remember {
        if (servers.isNullOrEmpty()) Routes.SERVERS else Routes.NOW
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.topLevel

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ConnectionBanner(
                state = connection,
                hasServer = server != null,
                onRetry = { graph.repository.retryConnection() }
            )
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.weight(1f)
            ) {
                composable(Routes.NOW) { NowPlayingScreen(navController) }
                composable(Routes.LIBRARY) { LibraryScreen(navController) }
                composable(Routes.RADIO) { RadioScreen(navController) }
                composable(Routes.PLAYERS) { PlayersScreen(navController) }
                composable(Routes.SETTINGS) { SettingsScreen(navController) }

                composable(Routes.SERVERS) { ServerListScreen(navController) }
                composable(
                    route = Routes.SERVER_EDIT,
                    arguments = listOf(
                        navArgument("serverId") { defaultValue = "" },
                        navArgument("first") {
                            type = NavType.BoolType
                            defaultValue = false
                        }
                    )
                ) { entry ->
                    ServerEditScreen(
                        navController = navController,
                        serverId = entry.arguments?.getString("serverId")?.takeIf { it.isNotBlank() },
                        first = entry.arguments?.getBoolean("first") ?: false
                    )
                }

                composable(
                    route = Routes.ALBUMS,
                    arguments = listOf(
                        navArgument("title") { defaultValue = "专辑" },
                        navArgument("artistId") { defaultValue = "" },
                        navArgument("genreId") { defaultValue = "" },
                        navArgument("year") { defaultValue = "" },
                        navArgument("sort") { defaultValue = "album" }
                    )
                ) { entry ->
                    AlbumsScreen(
                        navController = navController,
                        title = entry.arguments?.getString("title") ?: "专辑",
                        artistId = entry.arguments?.getString("artistId")?.takeIf { it.isNotBlank() },
                        genreId = entry.arguments?.getString("genreId")?.takeIf { it.isNotBlank() },
                        year = entry.arguments?.getString("year")?.takeIf { it.isNotBlank() },
                        initialSort = entry.arguments?.getString("sort") ?: "album"
                    )
                }

                composable(
                    route = Routes.ALBUM,
                    arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                ) { entry ->
                    AlbumDetailScreen(
                        navController = navController,
                        albumId = entry.arguments?.getString("albumId").orEmpty()
                    )
                }

                composable(
                    route = Routes.PLAYLIST,
                    arguments = listOf(
                        navArgument("playlistId") { type = NavType.StringType },
                        navArgument("name") { defaultValue = "" }
                    )
                ) { entry ->
                    PlaylistDetailScreen(
                        navController = navController,
                        playlistId = entry.arguments?.getString("playlistId").orEmpty(),
                        playlistName = entry.arguments?.getString("name").orEmpty()
                    )
                }

                composable(
                    route = Routes.BROWSE,
                    arguments = listOf(navArgument("category") { type = NavType.StringType })
                ) { entry ->
                    BrowseScreen(
                        navController = navController,
                        category = entry.arguments?.getString("category").orEmpty()
                    )
                }

                composable(
                    route = Routes.BROWSER,
                    arguments = listOf(
                        navArgument("source") { type = NavType.StringType },
                        navArgument("pluginCmd") { type = NavType.StringType },
                        navArgument("itemId") { type = NavType.StringType },
                        navArgument("title") { defaultValue = "浏览" }
                    )
                ) { entry ->
                    BrowserScreen(
                        navController = navController,
                        source = entry.arguments?.getString("source").orEmpty(),
                        pluginCmd = entry.arguments?.getString("pluginCmd").orEmpty(),
                        itemId = entry.arguments?.getString("itemId").orEmpty(),
                        title = entry.arguments?.getString("title") ?: "浏览"
                    )
                }

                composable(Routes.SYSTEM_INFO) { SystemInfoScreen(navController) }
                composable(Routes.SONGS) { SongsScreen(navController) }
                composable(Routes.FOLDERS) { FolderBrowserScreen(navController) }
                composable(Routes.LYRICS) { LyricsScreen(navController) }
                composable(Routes.AUDIO_OUTPUT) { AudioOutputScreen(navController) }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 10.dp)
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 22.dp)
                    .size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun SystemBars(dark: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    SideEffect {
        val window = activity?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
