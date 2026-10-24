package com.daphile.remote.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daphile.remote.DaphileApp
import com.daphile.remote.data.model.Lyrics
import com.daphile.remote.data.model.LyricsSource
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.friendlyErrorMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LyricsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DaphileApp).graph.repository

    val nowPlaying = repository.nowPlaying
    val server = repository.server

    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics = _lyrics.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** 当前歌词已保存到的本地 .lrc 路径。 */
    private val _savedPath = MutableStateFlow<String?>(null)
    val savedPath = _savedPath.asStateFlow()

    /** 歌词匹配来源，默认自动（网易云优先），切换后持久保存。 */
    private val _source = MutableStateFlow(LyricsSource.AUTO)
    val source = _source.asStateFlow()

    /** 一次性提示，例如所选源未命中时的回退说明。 */
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    private var loadJob: Job? = null

    init {
        _source.value = LyricsSource.fromKey(repository.appSettings.value.lyricsSource)
        viewModelScope.launch {
            repository.nowPlaying
                .map { it?.track?.id }
                .distinctUntilChanged()
                .collect {
                    val track = repository.nowPlaying.value?.track
                    if (track == null) {
                        _lyrics.value = null
                        _savedPath.value = null
                        _loading.value = false
                        _error.value = "暂无可匹配的曲目"
                    } else {
                        loadLyrics(track)
                    }
                }
        }
    }

    fun refresh() {
        val track = repository.nowPlaying.value?.track
        if (track != null) loadLyrics(track, forceOnline = true)
    }

    /** 切换歌词来源并立即重新匹配当前曲目。 */
    fun selectSource(target: LyricsSource) {
        if (_source.value == target) return
        _source.value = target
        viewModelScope.launch { runCatching { repository.setLyricsSource(target) } }
        repository.nowPlaying.value?.track?.let { loadLyrics(it, forceOnline = true) }
    }

    fun clearNotice() {
        _notice.value = null
    }

    /** 返回可写入文件的 LRC 文本（优先原始文本，否则由时间轴重建）。 */
    fun lrcText(): String? {
        val lyrics = _lyrics.value ?: return null
        if (lyrics.plainText.isNotBlank()) return lyrics.plainText
        if (lyrics.lines.isEmpty()) return null
        return lyrics.lines.joinToString("\n") { line ->
            val minutes = line.timeMs / 60_000
            val seconds = (line.timeMs % 60_000) / 1000
            val centis = (line.timeMs % 1000) / 10
            String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s", minutes, seconds, centis, line.text)
        }
    }

    fun currentFileName(): String {
        val title = repository.nowPlaying.value?.track?.title.orEmpty().ifBlank { "lyrics" }
        val safe = title.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        return "${safe.ifBlank { "lyrics" }}.lrc"
    }

    suspend fun saveToLibrary(): String? {
        val track = repository.nowPlaying.value?.track ?: return null
        val lyrics = _lyrics.value ?: return null
        val path = repository.saveLyrics(track, lyrics)
        _savedPath.value = path
        return path
    }

    private fun loadLyrics(track: Track, forceOnline: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val activeSource = _source.value
            _loading.value = true
            _error.value = null
            _lyrics.value = null
            _savedPath.value = repository.savedLyricsPath(track)
            try {
                var result = repository.lyricsForTrack(
                    track = track,
                    source = activeSource,
                    forceOnline = forceOnline || activeSource != LyricsSource.AUTO
                )
                if (result == null || result.lines.isEmpty()) {
                    // 在线匹配失败时回退到本地已保存的歌词，避免页面变空
                    if (activeSource != LyricsSource.AUTO || forceOnline) {
                        val fallback = repository.lyricsForTrack(
                            track = track,
                            source = LyricsSource.AUTO,
                            forceOnline = false
                        )
                        if (fallback != null && fallback.lines.isNotEmpty()) {
                            result = fallback
                            _notice.value = if (activeSource == LyricsSource.AUTO) {
                                "在线未找到歌词，已显示本地歌词"
                            } else {
                                "未在${activeSource.label}找到歌词，已显示本地歌词"
                            }
                        }
                    }
                }
                if (result == null || result.lines.isEmpty()) {
                    _error.value = if (activeSource == LyricsSource.AUTO) {
                        "未找到歌词，可更换歌词源或稍后重试"
                    } else {
                        "未在${activeSource.label}找到歌词，可换一个歌词源"
                    }
                } else {
                    _lyrics.value = result
                    _savedPath.value = repository.savedLyricsPath(track)
                }
            } catch (error: Throwable) {
                _error.value = friendlyErrorMessage(error)
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun seekTo(positionMs: Long) {
        repository.seekTo(positionMs / 1000.0)
    }
}
