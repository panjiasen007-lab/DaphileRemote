package com.daphile.remote.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.Album
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.CoverArt
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.LoadingState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.common.QualityBadge
import com.daphile.remote.ui.common.TrackRow
import com.daphile.remote.ui.common.bestTrackQuality
import com.daphile.remote.ui.i18n.strings
import kotlinx.coroutines.launch

@Composable
fun AlbumDetailScreen(
    navController: NavController,
    albumId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryViewModel = viewModel()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()

    var album by remember { mutableStateOf<Album?>(null) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var onlineCover by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val t = strings

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

    LaunchedEffect(albumId, reloadKey) {
        loading = true
        errorText = null
        try {
            val (info, list) = viewModel.fetchAlbumDetails(albumId)
            album = info
            tracks = list
        } catch (error: Throwable) {
            errorText = friendlyErrorMessage(error)
        } finally {
            loading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = album?.title ?: "专辑",
                onBack = { navController.popBackStack() }
            )
            when {
                loading -> LoadingState()
                errorText != null -> Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { reloadKey++ }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("重试")
                    }
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        val info = album
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CoverArt(
                                url = onlineCover ?: server?.coverUrl(
                                    info?.artworkId ?: tracks.firstOrNull()?.coverId
                                ),
                                modifier = Modifier.size(220.dp),
                                cornerRadius = 18.dp
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = info?.title ?: "专辑",
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = info?.artist.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            val meta = listOf(
                                info?.year.orEmpty(),
                                strings.tracksUnit.format(tracks.size)
                            ).filter { it.isNotBlank() && it != "0" }.joinToString(" · ")
                            val quality = bestTrackQuality(tracks)
                            if (meta.isNotBlank() || quality != null) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (meta.isNotBlank()) {
                                        Text(
                                            text = meta,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (quality != null) {
                                        Spacer(Modifier.width(6.dp))
                                        QualityBadge(quality)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = {
                                    runCommand { viewModel.playAlbum(albumId) }
                                }) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.playAll)
                                }
                                OutlinedButton(onClick = {
                                    runCommand(t.addToQueue) { viewModel.queueAlbum(albumId) }
                                }) {
                                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(t.addToQueue)
                                }
                            }
                            TextButton(onClick = {
                                val info = album ?: return@TextButton
                                runCommand {
                                    val resolved = viewModel.resolveAlbumCover(info.artist, info.title)
                                    if (resolved != null) {
                                        onlineCover = resolved
                                        snackbar.showSnackbar("已匹配在线封面")
                                    } else {
                                        snackbar.showSnackbar("未找到在线封面")
                                    }
                                }
                            }) {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("在线匹配封面")
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    if (tracks.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Rounded.PlayArrow,
                                title = "没有曲目",
                                body = "这张专辑没有可播放的曲目"
                            )
                        }
                    } else {
                        itemsIndexed(items = tracks, key = { _, track -> track.id + track.index }) { index, track ->
                            val isCurrent = nowPlaying?.track?.id == track.id
                            TrackRow(
                                track = track,
                                coverUrl = server?.coverUrl(track.coverId),
                                leadingText = (if (track.trackNum > 0) track.trackNum else index + 1)
                                    .toString()
                                    .padStart(2, '0'),
                                isCurrent = isCurrent,
                                trailing = {
                                    IconButton(onClick = {
                                        runCommand("已加入队列") { viewModel.queueTrack(track.id) }
                                    }) {
                                        Icon(
                                            Icons.Rounded.PlaylistAdd,
                                            contentDescription = "加入队列",
                                            modifier = Modifier.size(19.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    runCommand { viewModel.playTrack(track.id) }
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
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
