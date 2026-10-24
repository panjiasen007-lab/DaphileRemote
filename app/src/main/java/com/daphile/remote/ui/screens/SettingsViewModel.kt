package com.daphile.remote.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.daphile.remote.DaphileApp
import com.daphile.remote.data.model.DiscoveredServer
import com.daphile.remote.data.model.HostPowerAction
import com.daphile.remote.data.model.ServerConfig
import com.daphile.remote.data.model.ServerInfo

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DaphileApp).graph.repository

    val server = repository.server
    val servers = repository.servers
    val serverInfo = repository.serverInfo
    val connection = repository.connection
    val appSettings = repository.appSettings
    val players = repository.players

    suspend fun testServer(serverConfig: ServerConfig): ServerInfo =
        repository.testServer(serverConfig)

    suspend fun saveServer(serverConfig: ServerConfig) =
        repository.saveServer(serverConfig, makeCurrent = true)

    suspend fun deleteServer(serverId: String) = repository.deleteServer(serverId)

    suspend fun switchServer(serverId: String) = repository.switchServer(serverId)

    suspend fun setTheme(theme: String) = repository.setTheme(theme)

    suspend fun setLanguage(language: com.daphile.remote.data.model.AppLanguage) =
        repository.setLanguage(language)

    suspend fun setPollSeconds(seconds: Int) = repository.setPollSeconds(seconds)

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        repository.setNotificationsEnabled(enabled)

    suspend fun setHostPowerEnabled(enabled: Boolean) =
        repository.setHostPowerEnabled(enabled)

    suspend fun setOnlineMetadataEnabled(enabled: Boolean) =
        repository.setOnlineMetadataEnabled(enabled)

    suspend fun setLockScreenControls(enabled: Boolean) =
        repository.setLockScreenControls(enabled)

    suspend fun hostAction(action: HostPowerAction) = repository.hostPower(action)

    suspend fun rescan() = repository.rescan()

    suspend fun discoverServers(): List<DiscoveredServer> = repository.discoverServers()

    fun lyricsLibraryPath(): String = repository.lyricsLibraryPath()

    fun lyricsLibraryCount(): Int = repository.lyricsLibraryCount()

    suspend fun clearLyricsLibrary(): Int = repository.clearLyricsLibrary()
}
