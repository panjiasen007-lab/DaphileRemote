package com.daphile.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.common.TrackRow
import kotlinx.coroutines.launch

private const val SONG_PAGE_SIZE = 100

@Composable
fun SongsScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: LibraryViewModel = viewModel()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var items by remember { mutableStateOf<List<Track>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val lastVisible by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }

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

    suspend fun loadMore() {
        if (loading || items.size >= total) return
        loading = true
        try {
            val page = viewModel.fetchSongs(items.size, SONG_PAGE_SIZE)
            items = items + page.items
            total = page.total
        } catch (error: Throwable) {
            errorText = friendlyErrorMessage(error)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(reloadKey) {
        loading = true
        errorText = null
        items = emptyList()
        total = 0
        try {
            val page = viewModel.fetchSongs(0, SONG_PAGE_SIZE)
            items = page.items
            total = page.total
        } catch (error: Throwable) {
            errorText = friendlyErrorMessage(error)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(lastVisible, items.size, loading) {
        if (!loading && items.size < total && lastVisible >= items.size - 6) {
            loadMore()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = strings.songsTitle,
                onBack = { navController.popBackStack() },
                actions = { Text("$total 首", style = MaterialTheme.typography.labelMedium) }
            )
            when {
                errorText != null && items.isEmpty() -> Column(
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
                items.isEmpty() && !loading -> EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "没有歌曲",
                    body = "音乐库中尚未扫描到曲目"
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(items = items, key = { "${it.id}:${it.index}" }) { track ->
                        val isCurrent = nowPlaying?.track?.id == track.id && track.id.isNotBlank()
                        TrackRow(
                            track = track,
                            coverUrl = server?.coverUrl(track.coverId, nowPlaying?.playerId),
                            showCover = true,
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
                            onClick = { runCommand { viewModel.playTrack(track.id) } }
                        )
                    }
                    if (loading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    } else if (items.size < total) {
                        item {
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
                .padding(16.dp)
        )
    }
}
