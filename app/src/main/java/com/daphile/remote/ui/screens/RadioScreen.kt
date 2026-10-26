package com.daphile.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.BrowserItem
import com.daphile.remote.data.model.PluginEntry
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.CoverArt
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun RadioScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: RadioViewModel = viewModel()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val radios by viewModel.radios.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val t = strings

    LaunchedEffect(Unit) { viewModel.refresh() }

    fun runCommand(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (error: Throwable) {
                snackbar.showSnackbar(friendlyErrorMessage(error))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = t.navRadio)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ) {
                listOf(t.favorites, t.radios, t.streamingServices).forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) }
                    )
                }
            }

            when {
                loading && favorites == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                error != null && favorites == null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t.retry)
                    }
                }
                else -> when (selectedTab) {
                    0 -> FavoritesTab(
                        favorites = favorites.orEmpty(),
                        serverIconUrl = { icon -> resolveUrl(server?.baseUrl.orEmpty(), icon) },
                        onOpenFolder = { item ->
                            navController.navigate(
                                Routes.browser("favorites", "favorites", item.id, item.name)
                            )
                        },
                        onPlay = { item ->
                            runCommand { viewModel.playFavorite(item.id) }
                        },
                        onPlayAll = {
                            runCommand { viewModel.favoritesToQueue(play = true) }
                        },
                        onQueueAll = {
                            runCommand { viewModel.favoritesToQueue(play = false) }
                        }
                    )
                    1 -> PluginTab(
                        plugins = radios.orEmpty(),
                        emptyTitle = t.noRadiosTitle,
                        emptyBody = t.noRadiosBody,
                        iconUrl = { icon -> resolveUrl(server?.baseUrl.orEmpty(), icon) },
                        onClick = { plugin ->
                            navController.navigate(
                                Routes.browser("radio", plugin.cmd, "0", plugin.name)
                            )
                        }
                    )
                    else -> PluginTab(
                        plugins = apps.orEmpty(),
                        emptyTitle = t.noStreamingTitle,
                        emptyBody = t.noStreamingBody,
                        iconUrl = { icon -> resolveUrl(server?.baseUrl.orEmpty(), icon) },
                        onClick = { plugin ->
                            navController.navigate(
                                Routes.browser("apps", plugin.cmd, "0", plugin.name)
                            )
                        }
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

private fun resolveUrl(baseUrl: String, icon: String): String? {
    if (icon.isBlank()) return null
    if (icon.startsWith("http://") || icon.startsWith("https://")) return icon
    return "$baseUrl/${icon.trimStart('/')}"
}

@Composable
private fun FavoritesTab(
    favorites: List<BrowserItem>,
    serverIconUrl: (String) -> String?,
    onOpenFolder: (BrowserItem) -> Unit,
    onPlay: (BrowserItem) -> Unit,
    onPlayAll: () -> Unit,
    onQueueAll: () -> Unit
) {
    val t = strings
    if (favorites.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.FavoriteBorder,
            title = t.favoritesEmptyTitle,
            body = t.favoritesEmptyBody
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onPlayAll, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(t.playAllItems)
                }
                OutlinedButton(onClick = onQueueAll, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Rounded.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(t.addAllToQueue)
                }
            }
        }
        items(items = favorites, key = { it.id }) { item ->
            BrowserItemRow(
                item = item,
                iconUrl = serverIconUrl(item.icon),
                fallbackIcon = Icons.Rounded.FavoriteBorder,
                onClick = { if (item.hasItems) onOpenFolder(item) else onPlay(item) }
            )
        }
    }
}

@Composable
private fun PluginTab(
    plugins: List<PluginEntry>,
    emptyTitle: String,
    emptyBody: String,
    iconUrl: (String) -> String?,
    onClick: (PluginEntry) -> Unit
) {
    val t = strings
    if (plugins.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Radio,
            title = emptyTitle,
            body = emptyBody
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(items = plugins, key = { it.cmd }) { plugin ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(plugin) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(
                    url = iconUrl(plugin.icon),
                    modifier = Modifier.size(52.dp),
                    cornerRadius = 12.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = t.browseContents,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun BrowserItemRow(
    item: BrowserItem,
    iconUrl: String?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconUrl != null) {
            CoverArt(url = iconUrl, modifier = Modifier.size(52.dp), cornerRadius = 12.dp)
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = if (item.hasItems) Icons.Rounded.ChevronRight else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
