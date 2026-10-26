package com.daphile.remote.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.LoadingState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.common.TrackRow
import com.daphile.remote.ui.i18n.strings
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    navController: NavController,
    playlistId: String,
    playlistName: String,
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryViewModel = viewModel()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()

    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
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

    LaunchedEffect(playlistId, reloadKey) {
        loading = true
        errorText = null
        try {
            tracks = viewModel.fetchPlaylistTracks(playlistId)
        } catch (error: Throwable) {
            errorText = friendlyErrorMessage(error)
        } finally {
            loading = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = playlistName.ifBlank { t.playlists },
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
                        Text(t.retry)
                    }
                }
                tracks.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = t.playlistEmptyTitle,
                    body = t.playlistEmptyBody
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        RoundedCornerShape(18.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.PlaylistPlay,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlistName.ifBlank { t.playlists },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = t.tracksUnit.format(tracks.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = {
                                runCommand { viewModel.playPlaylist(playlistId) }
                            }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(t.play)
                            }
                        }
                    }
                    itemsIndexed(items = tracks, key = { _, track -> track.id + track.index }) { index, track ->
                        val isCurrent = nowPlaying?.track?.id == track.id
                        TrackRow(
                            track = track,
                            coverUrl = server?.coverUrl(track.coverId),
                            showCover = true,
                            isCurrent = isCurrent,
                            trailing = {
                                IconButton(onClick = {
                                    runCommand(t.queueAddedCount) { viewModel.queueTrack(track.id) }
                                }) {
                                    Icon(
                                        Icons.Rounded.PlaylistAdd,
                                        contentDescription = t.addToQueue,
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
