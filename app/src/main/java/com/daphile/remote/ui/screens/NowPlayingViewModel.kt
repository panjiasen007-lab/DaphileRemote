package com.daphile.remote.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daphile.remote.DaphileApp
import com.daphile.remote.data.model.HostPowerAction
import com.daphile.remote.data.model.SleepAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NowPlayingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DaphileApp).graph.repository

    val nowPlaying = repository.nowPlaying
    val players = repository.players
    val currentPlayerId = repository.currentPlayerId
    val server = repository.server
    val connection = repository.connection
    val sleepRemaining = repository.sleepRemaining
    val serverInfo = repository.serverInfo

    private var volumeJob: Job? = null
    private var pendingVolume: Int? = null

    private val _coverOverride = MutableStateFlow<String?>(null)
    val coverOverride = _coverOverride.asStateFlow()

    init {
        viewModelScope.launch {
            repository.nowPlaying
                .map { it?.track?.id }
                .distinctUntilChanged()
                .collect { _coverOverride.value = null }
        }
    }

    suspend fun matchCover(force: Boolean = true): Boolean {
        val track = nowPlaying.value?.track ?: return false
        val url = repository.resolveOnlineCover(track, force) ?: return false
        _coverOverride.value = url
        return true
    }

    fun selectPlayer(playerId: String) = repository.selectPlayer(playerId)

    fun onVolumeChange(value: Int) {
        pendingVolume = value
        volumeJob?.cancel()
        volumeJob = viewModelScope.launch {
            delay(200)
            sendPendingVolume()
        }
    }

    fun flushVolume() {
        volumeJob?.cancel()
        volumeJob = viewModelScope.launch { sendPendingVolume() }
    }

    private suspend fun sendPendingVolume() {
        val value = pendingVolume ?: return
        pendingVolume = null
        runCatching { repository.setVolume(value) }
    }

    suspend fun playPause() = repository.playPause()
    suspend fun play() = repository.play()
    suspend fun pause() = repository.pause()
    suspend fun stop() = repository.stop()
    suspend fun next() = repository.next()
    suspend fun previous() = repository.previous()
    suspend fun seekTo(seconds: Double) = repository.seekTo(seconds)
    suspend fun toggleMute() = repository.toggleMute()
    suspend fun setShuffle(enabled: Boolean) = repository.setShuffle(enabled)
    suspend fun setRepeat(value: Int) = repository.setRepeat(value)
    suspend fun setPlayerPower(on: Boolean) {
        val playerId = currentPlayerId.value ?: return
        repository.setPlayerPower(playerId, on)
    }

    suspend fun hostPower(action: HostPowerAction) = repository.hostPower(action)

    suspend fun playQueueIndex(index: Int) = repository.playQueueIndex(index)
    suspend fun removeQueueItem(index: Int) = repository.removeQueueItem(index)
    suspend fun clearQueue() = repository.clearQueue()
    suspend fun saveQueue(name: String) = repository.saveQueue(name)
    suspend fun addCurrentFavorite() = repository.addCurrentFavorite()
    suspend fun startSleepTimer(minutes: Int, action: SleepAction) =
        repository.startSleepTimer(minutes, action)
    suspend fun cancelSleepTimer() = repository.cancelSleepTimer()
}
