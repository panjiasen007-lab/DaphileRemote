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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.Artist
import com.daphile.remote.data.model.Genre
import com.daphile.remote.data.model.Paged
import com.daphile.remote.data.model.PlaylistSummary
import com.daphile.remote.data.model.YearItem
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.navigation.Routes
import kotlinx.coroutines.launch

private const val BROWSE_PAGE = 200

@Composable
fun BrowseScreen(navController: NavController, category: String, modifier: Modifier = Modifier) {
    val viewModel: LibraryViewModel = viewModel()
    val t = strings
    when (category) {
        "artists" -> SimplePagedScreen<Artist>(
            navController = navController,
            title = t.artists,
            modifier = modifier,
            load = viewModel::fetchArtists,
            key = { it.id },
            emptyTitle = t.noArtistsTitle,
            emptyBody = t.noArtistsBody,
            itemContent = { artist ->
                SimpleBrowseRow(
                    icon = Icons.Rounded.Person,
                    title = artist.name,
                    subtitle = t.viewAlbums,
                    onClick = {
                        navController.navigate(Routes.albums(artist.name, artistId = artist.id))
                    }
                )
            }
        )
        "genres" -> SimplePagedScreen<Genre>(
            navController = navController,
            title = t.genres,
            modifier = modifier,
            load = viewModel::fetchGenres,
            key = { it.id },
            emptyTitle = t.noGenresTitle,
            emptyBody = t.noGenresBody,
            itemContent = { genre ->
                SimpleBrowseRow(
                    icon = Icons.Rounded.Folder,
                    title = genre.name,
                    subtitle = t.viewAlbums,
                    onClick = {
                        navController.navigate(Routes.albums(genre.name, genreId = genre.id))
                    }
                )
            }
        )
        "years" -> SimplePagedScreen<YearItem>(
            navController = navController,
            title = t.years,
            modifier = modifier,
            load = viewModel::fetchYears,
            key = { it.year },
            emptyTitle = t.noYearsTitle,
            emptyBody = t.noYearsBody,
            itemContent = { year ->
                SimpleBrowseRow(
                    icon = Icons.Rounded.DateRange,
                    title = year.year,
                    subtitle = t.openYear,
                    onClick = {
                        navController.navigate(Routes.albums(year.year, year = year.year))
                    }
                )
            }
        )
        else -> SimplePagedScreen<PlaylistSummary>(
            navController = navController,
            title = t.playlists,
            modifier = modifier,
            load = viewModel::fetchPlaylists,
            key = { it.id },
            emptyTitle = t.noPlaylistsTitle,
            emptyBody = t.noPlaylistsBody,
            itemContent = { playlist ->
                SimpleBrowseRow(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = playlist.name,
                    subtitle = t.openPlaylist,
                    onClick = {
                        navController.navigate(Routes.playlist(playlist.id, playlist.name))
                    }
                )
            }
        )
    }
}

@Composable
private fun <T> SimplePagedScreen(
    navController: NavController,
    title: String,
    modifier: Modifier = Modifier,
    load: suspend (Int, Int) -> Paged<T>,
    key: (T) -> String,
    emptyTitle: String,
    emptyBody: String,
    itemContent: @Composable (T) -> Unit
) {
    var items by remember { mutableStateOf<List<T>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val t = strings
    val lastVisible by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }

    suspend fun loadMore() {
        if (loading || items.size >= total) return
        loading = true
        errorText = null
        try {
            val page = load(items.size, BROWSE_PAGE)
            items = items + page.items
            total = page.total
        } catch (error: Throwable) {
            errorText = friendlyErrorMessage(error)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(reloadKey) {
        items = emptyList()
        total = 0
        loading = true
        errorText = null
        try {
            val page = load(0, BROWSE_PAGE)
            items = page.items
            total = page.total
        } catch (error: Throwable) {
            errorText = friendlyErrorMessage(error)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(lastVisible, items.size, loading) {
        if (!loading && items.size < total && lastVisible >= items.size - 3) {
            loadMore()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PageHeader(title = title, onBack = { navController.popBackStack() })
        when {
            errorText != null && items.isEmpty() -> {
                Column(
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
            }
            items.isEmpty() && !loading -> {
                EmptyState(
                    icon = Icons.Rounded.PlaylistPlay,
                    title = emptyTitle,
                    body = emptyBody
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = items, key = key) { item ->
                        itemContent(item)
                    }
                    if (loading) {
                        item {
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
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(onClick = { scope.launch { loadMore() } }) {
                                    Text(t.loadMore.format(items.size, total))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SimpleBrowseRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
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
