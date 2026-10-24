@file:OptIn(ExperimentalFoundationApi::class)

package com.daphile.remote.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.daphile.remote.data.model.Album
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.CoverArt
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.navigation.Routes
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 100

@Composable
fun AlbumsScreen(
    navController: NavController,
    title: String,
    artistId: String?,
    genreId: String?,
    year: String?,
    initialSort: String = "album",
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryViewModel = viewModel()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val t = strings

    var grid by rememberSaveable { mutableStateOf(true) }
    var sort by rememberSaveable { mutableStateOf(initialSort) }
    var items by remember { mutableStateOf<List<Album>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    var actionAlbum by remember { mutableStateOf<Album?>(null) }

    val snackbar = remember { SnackbarHostState() }
    fun runCommand(successMessage: String? = null, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                if (successMessage != null) snackbar.showSnackbar(successMessage)
            } catch (error: Throwable) {
                snackbar.showSnackbar(friendlyErrorMessage(error))
            }
        }
    }

    val gridState = rememberLazyGridState()
    val lastVisible by remember {
        derivedStateOf { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }

    suspend fun loadMore() {
        if (loading || items.size >= total) return
        loading = true
        errorText = null
        try {
            val page = viewModel.fetchAlbums(
                start = items.size,
                count = PAGE_SIZE,
                sort = sort,
                artistId = artistId,
                genreId = genreId,
                year = year
            )
            items = items + page.items
            total = page.total
        } catch (error: Throwable) {
            errorText = friendlyErrorMessage(error)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(sort, artistId, genreId, year, reloadKey) {
        items = emptyList()
        total = 0
        errorText = null
        loading = true
        try {
            val page = viewModel.fetchAlbums(
                start = 0,
                count = PAGE_SIZE,
                sort = sort,
                artistId = artistId,
                genreId = genreId,
                year = year
            )
            items = page.items
            total = page.total
        } catch (error: Throwable) {
            errorText = friendlyErrorMessage(error)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(lastVisible, items.size, loading) {
        if (!loading && items.size < total && lastVisible >= items.size - 4) {
            loadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(
            title = title,
            onBack = { navController.popBackStack() },
            actions = {
                IconButton(
                    onClick = {
                        runCommand(t.randomAlbum) { viewModel.randomAlbum() }
                    }
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = strings.randomAlbum)
                }
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.Rounded.Sort, contentDescription = strings.sortBy)
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        listOf(
                            "album" to strings.sortAlbum,
                            "artistalbum" to strings.sortArtist,
                            "yearalbum" to strings.sortYear,
                            "new" to strings.sortNew
                        ).forEach { (value, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        color = if (sort == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    sort = value
                                    sortMenu = false
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = { grid = !grid }) {
                    Icon(
                        imageVector = if (grid) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                        contentDescription = if (grid) strings.listView else strings.gridView
                    )
                }
            }
        )

        if (errorText != null && items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorText.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = { reloadKey++ }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重试")
                    }
                }
            }
        } else if (items.isEmpty() && !loading) {
            EmptyState(
                icon = Icons.Rounded.LibraryMusic,
                title = "没有专辑",
                body = "当前分类下没有可浏览的专辑"
            )
        } else {
            LazyVerticalGrid(
                columns = if (grid) GridCells.Fixed(3) else GridCells.Fixed(1),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(if (grid) 12.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(if (grid) 16.dp else 2.dp)
            ) {
                if (grid) {
                    items(items = items, key = { it.id }) { album ->
                        AlbumGridCard(
                            album = album,
                            coverUrl = server?.coverUrl(album.artworkId),
                            onClick = { navController.navigate(Routes.album(album.id)) },
                            onLongClick = { actionAlbum = album }
                        )
                    }
                } else {
                    items(items = items, key = { it.id }) { album ->
                        AlbumListRow(
                            album = album,
                            coverUrl = server?.coverUrl(album.artworkId),
                            onClick = { navController.navigate(Routes.album(album.id)) },
                            onLongClick = { actionAlbum = album }
                        )
                    }
                }

                if (loading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                } else if (items.size < total) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TextButton(onClick = { scope.launch { loadMore() } }) {
                                Text("加载更多（${items.size}/$total）")
                            }
                        }
                    }
                }
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

    actionAlbum?.let { album ->
        AlertDialog(
            onDismissRequest = { actionAlbum = null },
            title = { Text(album.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = { Text(listOf(album.artist, album.year).filter { it.isNotBlank() && it != "0" }.joinToString(" · ")) },
            confirmButton = {
                TextButton(onClick = {
                    actionAlbum = null
                    runCommand("正在播放 ${album.title}") { viewModel.playAlbum(album.id) }
                }) { Text("播放全部") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        actionAlbum = null
                        runCommand("已加入队列") { viewModel.queueAlbum(album.id) }
                    }) { Text("加入队列") }
                    TextButton(onClick = {
                        val target = album
                        actionAlbum = null
                        navController.navigate(Routes.album(target.id))
                    }) { Text("查看") }
                }
            }
        )
    }
}

@Composable
private fun AlbumGridCard(
    album: Album,
    coverUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        CoverArt(
            url = coverUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            cornerRadius = 10.dp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AlbumListRow(
    album: Album,
    coverUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(url = coverUrl, modifier = Modifier.size(56.dp), cornerRadius = 8.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(album.artist, album.year).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
