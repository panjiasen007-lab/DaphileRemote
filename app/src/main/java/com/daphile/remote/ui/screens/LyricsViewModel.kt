package com.daphile.remote.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daphile.remote.DaphileApp
import com.daphile.remote.data.model.Lyrics
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

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            repository.nowPlaying
                .map { it?.track?.id }
                .distinctUntilChanged()
                .collect {
                    val track = repository.nowPlaying.value?.track
                    if (track == null) {
                        _lyrics.value = null
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
        if (track != null) loadLyrics(track)
    }

    private fun loadLyrics(track: Track) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _lyrics.value = null
            try {
                val result = repository.lyricsForTrack(track)
                if (result == null || result.lines.isEmpty()) {
                    _error.value = "未找到歌词，可稍后重试"
                } else {
                    _lyrics.value = result
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
