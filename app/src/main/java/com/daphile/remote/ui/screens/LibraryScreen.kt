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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.Album
import com.daphile.remote.data.model.Artist
import com.daphile.remote.data.model.SearchResults
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.CoverArt
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.LoadingState
import com.daphile.remote.ui.common.TrackRow
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: LibraryViewModel = viewModel()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val t = strings
    var showScanDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

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

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.libraryTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = { showScanDialog = true },
                    enabled = server != null
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "扫描音乐库")
                }
            }
            scanStatus?.let { status ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (status.scanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = status.message.ifBlank {
                                    if (status.scanning) "正在扫描音乐库…" else "扫描完成"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (status.scanning) {
                            Spacer(Modifier.height(8.dp))
                            if (status.percent != null) {
                                LinearProgressIndicator(
                                    progress = { (status.percent ?: 0) / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text(strings.searchHint) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Rounded.Close, contentDescription = "清除")
                        }
                    }
                }
            )

            Spacer(Modifier.height(18.dp))
            if (query.isBlank()) {
                if (serverInfo != null) {
                    Text(
                        text = "${strings.albumUnit.format(serverInfo?.albumCount ?: 0)} · " +
                            strings.songUnit.format(serverInfo?.songCount ?: 0),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                }
                val categories = listOf(
                    CategoryItem(
                        title = strings.albums,
                        subtitle = strings.albumUnit.format(serverInfo?.albumCount ?: 0),
                        icon = Icons.Rounded.LibraryMusic
                    ) { navController.navigate(Routes.albums(t.albums)) },
                    CategoryItem(
                        title = strings.songs,
                        subtitle = strings.songUnit.format(serverInfo?.songCount ?: 0),
                        icon = Icons.Rounded.MusicNote
                    ) { navController.navigate(Routes.SONGS) },
                    CategoryItem(
                        title = strings.artists,
                        subtitle = strings.artistUnit.format(serverInfo?.artistCount ?: 0),
                        icon = Icons.Rounded.Person
                    ) { navController.navigate(Routes.browse("artists")) },
                    CategoryItem(
                        title = strings.genres,
                        subtitle = strings.genreUnit.format(serverInfo?.genreCount ?: 0),
                        icon = Icons.Rounded.Folder
                    ) { navController.navigate(Routes.browse("genres")) },
                    CategoryItem(
                        title = strings.years,
                        subtitle = strings.browseByYear,
                        icon = Icons.Rounded.DateRange
                    ) { navController.navigate(Routes.browse("years")) },
                    CategoryItem(
                        title = strings.folders,
                        subtitle = strings.browseByFolder,
                        icon = Icons.Rounded.FolderOpen
                    ) { navController.navigate(Routes.FOLDERS) },
                    CategoryItem(
                        title = strings.playlists,
                        subtitle = strings.serverPlaylists,
                        icon = Icons.Rounded.PlaylistPlay
                    ) { navController.navigate(Routes.browse("playlists")) }
                )
                categories.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            CategoryCard(item = item, modifier = Modifier.weight(1f))
                        }
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            runCommand(t.randomAlbum) { viewModel.randomAlbum() }
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.randomAlbum,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.randomAlbumSub,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate(Routes.albums("最近添加", sort = "new"))
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.NewReleases,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "最近添加",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "按入库时间查看新专辑",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                SearchContent(
                    results = results,
                    searching = searching,
                    error = searchError,
                    serverCoverUrl = { album -> server?.coverUrl(album.artworkId) },
                    onRetry = viewModel::retrySearch,
                    onPlayTrack = { track ->
                        runCommand { viewModel.playTrack(track.id) }
                    },
                    onQueueTrack = { track ->
                        runCommand("已加入队列") { viewModel.queueTrack(track.id) }
                    },
                    onOpenAlbum = { album -> navController.navigate(Routes.album(album.id)) },
                    onOpenArtist = { artist ->
                        navController.navigate(Routes.albums(artist.name, artistId = artist.id))
                    }
                )
            }
            Spacer(Modifier.height(28.dp))
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = { showScanDialog = false },
            title = { Text("扫描音乐库") },
            text = {
                Column {
                    Text(
                        text = "让 Daphile 重新检查音乐目录，更新新增或变更的曲目。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            showScanDialog = false
                            runCommand("已开始扫描") { viewModel.startScan(full = false) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("扫描更新（推荐）")
                    }
                    TextButton(
                        onClick = {
                            showScanDialog = false
                            runCommand("已开始重建音乐库") { viewModel.startScan(full = true) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("重建音乐库", color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        text = "重建会清空索引并重新扫描，可能耗时较长。扫描进度会显示在音乐库页面顶部。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showScanDialog = false }) { Text("取消") }
            }
        )
    }
}

private data class CategoryItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun CategoryCard(item: CategoryItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(116.dp)
            .clickable(onClick = item.onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchContent(
    results: SearchResults?,
    searching: Boolean,
    error: String?,
    serverCoverUrl: (Album) -> String?,
    onRetry: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onQueueTrack: (Track) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit
) {
    when {
        searching -> LoadingState()
        error != null -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onRetry) { Text("重试") }
            }
        }
        results != null -> {
            if (results.songs.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "没有找到结果",
                    body = "未找到与「${results.query}」相关的内容，试试其他关键词"
                )
            } else {
                if (results.songs.isNotEmpty()) {
                    SearchSectionTitle("曲目", results.songs.size)
                    results.songs.forEach { track ->
                        TrackRow(
                            track = track,
                            leadingText = null,
                            showCover = false,
                            trailing = {
                                IconButton(onClick = { onQueueTrack(track) }) {
                                    Icon(
                                        Icons.Rounded.PlaylistAdd,
                                        contentDescription = "加入队列",
                                        modifier = Modifier.size(19.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = { onPlayTrack(track) }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (results.albums.isNotEmpty()) {
                    SearchSectionTitle("专辑", results.albums.size)
                    results.albums.forEach { album ->
                        SearchAlbumRow(
                            album = album,
                            coverUrl = serverCoverUrl(album),
                            onClick = { onOpenAlbum(album) }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (results.artists.isNotEmpty()) {
                    SearchSectionTitle("艺术家", results.artists.size)
                    results.artists.forEach { artist ->
                        SearchArtistRow(artist = artist, onClick = { onOpenArtist(artist) })
                    }
                }
            }
        }
        else -> {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchAlbumRow(album: Album, coverUrl: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(url = coverUrl, modifier = Modifier.size(52.dp), cornerRadius = 8.dp)
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
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SearchArtistRow(artist: Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(21.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
