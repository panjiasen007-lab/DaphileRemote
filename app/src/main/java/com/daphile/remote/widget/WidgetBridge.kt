package com.daphile.remote.widget

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.daphile.remote.data.model.NowPlaying
import com.daphile.remote.data.model.ServerConfig
import com.daphile.remote.data.repo.DaphileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class WidgetBridge(
    private val context: Context,
    repository: DaphileRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastCoverKey: String? = null
    private var lastStateKey: String? = null

    init {
        scope.launch {
            combine(repository.nowPlaying, repository.server) { nowPlaying, server ->
                nowPlaying to server
            }.collect { (nowPlaying, server) ->
                update(nowPlaying, server)
            }
        }
    }

    private suspend fun update(nowPlaying: NowPlaying?, server: ServerConfig?) {
        val track = nowPlaying?.track
        val coverFile = File(context.cacheDir, COVER_FILE)
        val coverKey = "${server?.baseUrl}:${track?.id}:${track?.coverId}"
        val stateKey = "${coverKey}|${track?.title}|${track?.artist}|${nowPlaying?.isPlaying}|${nowPlaying?.power}"
        if (stateKey == lastStateKey) return
        lastStateKey = stateKey
        if (track == null) {
            lastCoverKey = null
            coverFile.delete()
        } else if (coverKey != lastCoverKey) {
            lastCoverKey = coverKey
            val url = server?.coverUrl(track.coverId, nowPlaying.playerId)
            if (url == null || !downloadCover(url, coverFile)) {
                coverFile.delete()
            }
        }

        val subtitle = listOfNotNull(track?.artist, track?.album)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TITLE, track?.title.orEmpty())
            .putString(KEY_ARTIST, subtitle)
            .putBoolean(KEY_PLAYING, nowPlaying?.isPlaying == true)
            .putBoolean(KEY_HAS_TRACK, track != null)
            .apply()

        WidgetProvider.updateAll(context)
    }

    private suspend fun downloadCover(url: String, target: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(200)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            val bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return@runCatching false
            target.outputStream().use { stream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, stream)
            }
            true
        }.getOrDefault(false)
    }

    companion object {
        const val PREFS = "widget_state"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_PLAYING = "playing"
        const val KEY_HAS_TRACK = "has_track"
        const val COVER_FILE = "widget_cover.png"
    }
}
