package com.daphile.remote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.daphile.remote.data.model.Lyrics
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.PageHeader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LyricsScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: LyricsViewModel = viewModel()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val track = nowPlaying?.track
    val coverUrl = server?.coverUrl(track?.coverId, nowPlaying?.playerId)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var positionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(nowPlaying?.time) {
        positionMs = ((nowPlaying?.time ?: 0.0) * 1000).toLong()
    }
    LaunchedEffect(nowPlaying?.isPlaying) {
        if (nowPlaying?.isPlaying == true) {
            while (true) {
                delay(250)
                positionMs += 250
            }
        }
    }

    val lyricsData = lyrics
    val currentIndex = remember(lyricsData, positionMs) {
        if (lyricsData?.synced == true) {
            lyricsData.lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
        } else {
            -1
        }
    }
    LaunchedEffect(currentIndex, lyricsData?.lines?.size) {
        if (currentIndex >= 0 && lyricsData != null) {
            runCatching { listState.animateScrollToItem(currentIndex) }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (coverUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(coverUrl).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.13f,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(
                title = "歌词",
                onBack = { navController.popBackStack() },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "重新匹配歌词")
                    }
                }
            )
            Text(
                text = listOfNotNull(track?.title, track?.artist)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { "暂无正在播放的曲目" },
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(8.dp))
            when {
                loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                error != null -> EmptyState(
                    icon = Icons.Rounded.Subtitles,
                    title = "暂无歌词",
                    body = error.orEmpty(),
                    actionLabel = "重新匹配",
                    onAction = viewModel::refresh
                )
                lyricsData != null -> LyricsContent(
                    lyrics = lyricsData,
                    currentIndex = currentIndex,
                    listState = listState,
                    onLineClick = { lineIndex ->
                        if (lyricsData.synced && lyricsData.lines[lineIndex].timeMs > 0) {
                            scope.launch {
                                try {
                                    viewModel.seekTo(lyricsData.lines[lineIndex].timeMs)
                                } catch (error: Throwable) {
                                    snackbar.showSnackbar(friendlyErrorMessage(error))
                                }
                            }
                        }
                    },
                    source = lyricsData.source
                )
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

@Composable
private fun LyricsContent(
    lyrics: Lyrics,
    currentIndex: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onLineClick: (Int) -> Unit,
    source: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 160.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            itemsIndexed(items = lyrics.lines) { index, line ->
                val isCurrent = index == currentIndex && lyrics.synced
                Text(
                    text = line.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLineClick(index) }
                        .padding(vertical = 2.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isCurrent) 20.sp else 16.sp,
                        lineHeight = if (isCurrent) 28.sp else 24.sp
                    ),
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
        if (source.isNotBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Subtitles,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "歌词来源：$source${if (lyrics.synced) " · 实时同步" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        if (!lyrics.synced) {
            Text(
                text = "该歌词不含时间轴，仅支持滚动浏览",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB0BAC4)
            )
        }
    }
}
