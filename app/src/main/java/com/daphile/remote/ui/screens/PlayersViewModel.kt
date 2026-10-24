package com.daphile.remote.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daphile.remote.DaphileApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DaphileApp).graph.repository

    val players = repository.players
    val currentPlayerId = repository.currentPlayerId
    val nowPlaying = repository.nowPlaying

    private val volumeJobs = mutableMapOf<String, Job>()
    private val pendingVolumes = mutableMapOf<String, Int>()

    fun selectPlayer(playerId: String) = repository.selectPlayer(playerId)

    fun onVolumeChange(playerId: String, value: Int) {
        pendingVolumes[playerId] = value
        volumeJobs[playerId]?.cancel()
        volumeJobs[playerId] = viewModelScope.launch {
            delay(250)
            sendVolume(playerId)
        }
    }

    fun flushVolume(playerId: String) {
        volumeJobs[playerId]?.cancel()
        volumeJobs[playerId] = viewModelScope.launch { sendVolume(playerId) }
    }

    private suspend fun sendVolume(playerId: String) {
        val value = pendingVolumes.remove(playerId) ?: return
        runCatching { repository.setPlayerVolume(playerId, value) }
    }

    suspend fun setPower(playerId: String, on: Boolean) =
        repository.setPlayerPower(playerId, on)

    suspend fun playPause() = repository.playPause()

    suspend fun syncTo(playerId: String, masterId: String) =
        repository.syncPlayerTo(playerId, masterId)

    suspend fun unsync(playerId: String) = repository.unsyncPlayer(playerId)
}
