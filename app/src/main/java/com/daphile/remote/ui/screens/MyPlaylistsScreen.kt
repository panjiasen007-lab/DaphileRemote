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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.CustomPlaylist
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.LoadingState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.common.TrackRow
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun MyPlaylistsScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: LibraryViewModel = viewModel()
    val playlists by viewModel.customPlaylists.collectAsStateWithLifecycle()
    val t = strings
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<CustomPlaylist?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomPlaylist?>(null) }

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

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = t.myPlaylists,
                onBack = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = { showCreate = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = t.createPlaylist)
                    }
                }
            )
            val ordered = playlists.sortedByDescending { it.updatedAt }
            if (ordered.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = t.emptyPlaylistsTitle,
                    body = t.emptyPlaylistsBody
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = ordered, key = { it.id }) { playlist ->
                        PlaylistListItem(
                            playlist = playlist,
                            trackCountText = t.trackCount.format(playlist.tracks.size),
                            onClick = { navController.navigate(Routes.customPlaylist(playlist.id)) },
                            onPlay = { runCommand { viewModel.playCustomPlaylist(playlist) } },
                            onQueue = { runCommand(t.queueAddedCount) { viewModel.playCustomPlaylist(playlist, append = true) } },
                            onRename = { renameTarget = playlist },
                            onDelete = { deleteTarget = playlist }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    if (showCreate) {
        PlaylistNameDialog(
            title = t.createPlaylist,
            initial = "",
            confirmText = t.confirm,
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                runCommand(t.playlistCreated) { viewModel.createCustomPlaylist(name) }
            }
        )
    }
    renameTarget?.let { target ->
        PlaylistNameDialog(
            title = t.renamePlaylist,
            initial = target.name,
            confirmText = t.save,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                runCommand(t.playlistRenamed) { viewModel.renameCustomPlaylist(target.id, name) }
            }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(t.deletePlaylistTitle) },
            text = { Text(t.deletePlaylistBody.format(target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    runCommand(t.playlistDeleted) { viewModel.deleteCustomPlaylist(target.id) }
                }) {
                    Text(t.deletePlaylist, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(t.cancel) }
            }
        )
    }
}

@Composable
private fun PlaylistListItem(
    playlist: CustomPlaylist,
    trackCountText: String,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onQueue: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val t = strings
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.PlaylistPlay,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = trackCountText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onPlay) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = t.playAll,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = t.more)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(t.addAllToQueue) },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        menu = false
                        onQueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text(t.renamePlaylist) },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    onClick = {
                        menu = false
                        onRename()
                    }
                )
                DropdownMenuItem(
                    text = { Text(t.deletePlaylist) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = {
                        menu = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun CustomPlaylistScreen(
    navController: NavController,
    playlistId: String,
    modifier: Modifier = Modifier
) {
    val viewModel: LibraryViewModel = viewModel()
    val playlists by viewModel.customPlaylists.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val t = strings
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var renameTarget by remember { mutableStateOf<CustomPlaylist?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomPlaylist?>(null) }
    var menu by remember { mutableStateOf(false) }

    val playlist = playlists.firstOrNull { it.id == playlistId }

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

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = playlist?.name ?: t.myPlaylists,
                onBack = { navController.popBackStack() },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = t.more)
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(t.renamePlaylist) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    renameTarget = playlist
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(t.deletePlaylist) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    deleteTarget = playlist
                                }
                            )
                        }
                    }
                }
            )
            when {
                playlist == null -> EmptyState(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = t.emptyPlaylistsTitle,
                    body = t.emptyPlaylistsBody
                )
                playlist.tracks.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = t.emptyPlaylistTitle,
                    body = t.emptyPlaylistBody
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = t.trackCount.format(playlist.tracks.size),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = {
                                runCommand { viewModel.playCustomPlaylist(playlist) }
                            }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(t.playAll)
                            }
                            OutlinedButton(onClick = {
                                runCommand(t.queueAddedCount) {
                                    viewModel.playCustomPlaylist(playlist, append = true)
                                }
                            }) {
                                Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(t.addAllToQueue)
                            }
                        }
                    }
                    items(items = playlist.tracks, key = { it.id.ifBlank { it.url } }) { track ->
                        val isCurrent = nowPlaying?.track?.id == track.id && track.id.isNotBlank()
                        TrackRow(
                            track = track,
                            coverUrl = server?.coverUrl(track.coverId),
                            showCover = true,
                            isCurrent = isCurrent,
                            trailing = {
                                IconButton(onClick = {
                                    runCommand(t.removedFromPlaylist) {
                                        viewModel.removeTrackFromPlaylist(playlist, track)
                                    }
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = t.removeFromPlaylist,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = { runCommand { viewModel.playTrack(track.id) } }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    renameTarget?.let { target ->
        PlaylistNameDialog(
            title = t.renamePlaylist,
            initial = target.name,
            confirmText = t.save,
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                runCommand(t.playlistRenamed) { viewModel.renameCustomPlaylist(target.id, name) }
            }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(t.deletePlaylistTitle) },
            text = { Text(t.deletePlaylistBody.format(target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    runCommand { viewModel.deleteCustomPlaylist(target.id) }
                    navController.popBackStack()
                }) {
                    Text(t.deletePlaylist, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(t.cancel) }
            }
        )
    }
}

@Composable
fun DailyRecommendationScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: LibraryViewModel = viewModel()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val daily by viewModel.dailyRecommendation.collectAsStateWithLifecycle()
    val t = strings
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showSave by remember { mutableStateOf(false) }

    fun load(force: Boolean) {
        scope.launch {
            loading = true
            errorText = null
            try {
                viewModel.refreshDaily(force)
            } catch (error: Throwable) {
                errorText = friendlyErrorMessage(error)
            } finally {
                loading = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (daily == null || daily?.tracks.isNullOrEmpty()) load(false)
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = t.dailyRecommendation,
                onBack = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = { load(true) }, enabled = !loading) {
                        Icon(Icons.Rounded.Refresh, contentDescription = t.regenerate)
                    }
                }
            )
            val tracks = daily?.tracks.orEmpty()
            when {
                loading && tracks.isEmpty() -> LoadingState()
                errorText != null && tracks.isEmpty() -> Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorText.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = { load(true) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(t.retry)
                    }
                }
                tracks.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = t.emptyDailyTitle,
                    body = t.emptyDailyBody
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                            Text(
                                text = t.dailyRecommendationSub,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        runCommand {
                                            viewModel.playCustomPlaylist(
                                                CustomPlaylist(id = "daily", name = t.dailyRecommendation, tracks = tracks)
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(t.playAll)
                                }
                                OutlinedButton(
                                    onClick = {
                                        runCommand(t.queueAddedCount) {
                                            viewModel.playCustomPlaylist(
                                                CustomPlaylist(id = "daily", name = t.dailyRecommendation, tracks = tracks),
                                                append = true
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(t.addAllToQueue)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { showSave = true }) {
                                Text(t.saveToMyPlaylists)
                            }
                        }
                    }
                    items(items = tracks, key = { it.id.ifBlank { it.url } }) { track ->
                        val isCurrent = nowPlaying?.track?.id == track.id && track.id.isNotBlank()
                        TrackRow(
                            track = track,
                            coverUrl = server?.coverUrl(track.coverId),
                            showCover = true,
                            isCurrent = isCurrent,
                            onClick = { runCommand { viewModel.playTrack(track.id) } }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }

    if (showSave) {
        PlaylistNameDialog(
            title = t.saveToMyPlaylists,
            initial = t.dailyRecommendation,
            confirmText = t.save,
            onDismiss = { showSave = false },
            onConfirm = { name ->
                showSave = false
                runCommand(t.playlistCreated) {
                    viewModel.saveTracksAsPlaylist(name, daily?.tracks.orEmpty())
                }
            }
        )
    }
}

@Composable
fun PlaylistNameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    val t = strings
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(t.playlistName) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t.cancel) }
        }
    )
}

/** 长按曲目时弹出的「加入歌单」选择器。 */
@Composable
fun AddToPlaylistDialog(
    playlists: List<CustomPlaylist>,
    onDismiss: () -> Unit,
    onPick: (CustomPlaylist) -> Unit,
    onCreateAndAdd: (String) -> Unit
) {
    val t = strings
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.addToPlaylist) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (playlists.isEmpty()) {
                    Text(
                        text = t.noPlaylistsHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                playlists.sortedByDescending { it.updatedAt }.forEach { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(playlist) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.PlaylistPlay,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = t.trackCount.format(playlist.tracks.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(t.newPlaylistName) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreateAndAdd(newName.trim()) },
                enabled = newName.isNotBlank()
            ) { Text(t.createAndAdd) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t.cancel) }
        }
    )
}
