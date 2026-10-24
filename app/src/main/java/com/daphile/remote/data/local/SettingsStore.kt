package com.daphile.remote.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.daphile.remote.data.model.AppSettings
import com.daphile.remote.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "daphile_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class SettingsStore(
    private val context: Context,
    private val crypto: CryptoStore
) {
    private object Keys {
        val servers = stringPreferencesKey("servers")
        val currentServerId = stringPreferencesKey("current_server_id")
        val theme = stringPreferencesKey("theme")
        val pollSeconds = intPreferencesKey("poll_seconds")
        val notifications = booleanPreferencesKey("notifications_enabled")
        val hostPower = booleanPreferencesKey("host_power_enabled")
        val coverCacheMb = intPreferencesKey("cover_cache_mb")
        val onlineMetadata = booleanPreferencesKey("online_metadata_enabled")
        val lockScreenControls = booleanPreferencesKey("lock_screen_controls")
        val lyricsSource = stringPreferencesKey("lyrics_source")
        val language = stringPreferencesKey("app_language")
        fun playerFor(serverId: String) = stringPreferencesKey("player_$serverId")
    }

    private val safeData: Flow<Preferences> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    val servers: Flow<List<ServerConfig>> = safeData.map { preferences ->
        decodeServers(preferences[Keys.servers])
    }

    val currentServerId: Flow<String?> = safeData.map { it[Keys.currentServerId] }

    val currentServer: Flow<ServerConfig?> = combine(servers, currentServerId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }

    val appSettings: Flow<AppSettings> = safeData.map { preferences ->
        AppSettings(
            theme = preferences[Keys.theme] ?: "dark",
            pollSeconds = preferences[Keys.pollSeconds] ?: 3,
            notificationsEnabled = preferences[Keys.notifications] ?: true,
            hostPowerEnabled = preferences[Keys.hostPower] ?: true,
            coverCacheMb = preferences[Keys.coverCacheMb] ?: 200,
            onlineMetadataEnabled = preferences[Keys.onlineMetadata] ?: true,
            lockScreenControls = preferences[Keys.lockScreenControls] ?: true,
            lyricsSource = preferences[Keys.lyricsSource] ?: "auto",
            language = preferences[Keys.language] ?: "auto"
        )
    }

    suspend fun upsertServer(server: ServerConfig, makeCurrent: Boolean = false) {
        context.settingsDataStore.edit { preferences ->
            val existing = decodeServers(preferences[Keys.servers]).toMutableList()
            val index = existing.indexOfFirst { it.id == server.id }
            if (index >= 0) existing[index] = server else existing.add(server)
            preferences[Keys.servers] = encodeServers(existing)
            if (makeCurrent || preferences[Keys.currentServerId].isNullOrBlank()) {
                preferences[Keys.currentServerId] = server.id
            }
        }
    }

    suspend fun removeServer(serverId: String) {
        context.settingsDataStore.edit { preferences ->
            val existing = decodeServers(preferences[Keys.servers])
                .filterNot { it.id == serverId }
                .toMutableList()
            preferences[Keys.servers] = encodeServers(existing)
            preferences.remove(Keys.playerFor(serverId))
            if (preferences[Keys.currentServerId] == serverId) {
                if (existing.isEmpty()) {
                    preferences.remove(Keys.currentServerId)
                } else {
                    preferences[Keys.currentServerId] = existing.first().id
                }
            }
        }
    }

    suspend fun setCurrentServer(serverId: String) {
        context.settingsDataStore.edit { it[Keys.currentServerId] = serverId }
    }

    suspend fun currentServerSnapshot(): ServerConfig? {
        val list = servers.first()
        val id = currentServerId.first()
        return list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }

    suspend fun playerForServer(serverId: String): String? =
        safeData.first()[Keys.playerFor(serverId)]

    suspend fun setPlayerForServer(serverId: String, playerId: String) {
        context.settingsDataStore.edit { it[Keys.playerFor(serverId)] = playerId }
    }

    suspend fun setTheme(theme: String) {
        context.settingsDataStore.edit { it[Keys.theme] = theme }
    }

    suspend fun setPollSeconds(seconds: Int) {
        context.settingsDataStore.edit { it[Keys.pollSeconds] = seconds.coerceIn(2, 30) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.notifications] = enabled }
    }

    suspend fun setHostPowerEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.hostPower] = enabled }
    }

    suspend fun setCoverCacheMb(mb: Int) {
        context.settingsDataStore.edit { it[Keys.coverCacheMb] = mb.coerceIn(50, 1024) }
    }

    suspend fun setOnlineMetadataEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.onlineMetadata] = enabled }
    }

    suspend fun setLockScreenControls(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.lockScreenControls] = enabled }
    }

    suspend fun setLyricsSource(sourceKey: String) {
        context.settingsDataStore.edit { it[Keys.lyricsSource] = sourceKey }
    }

    suspend fun setLanguage(languageKey: String) {
        context.settingsDataStore.edit { it[Keys.language] = languageKey }
    }

    private fun decodeServers(raw: String?): List<ServerConfig> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val password = crypto.decrypt(item.optString("password"))
                    val id = item.optString("id")
                    if (id.isBlank() || item.optString("host").isBlank()) continue
                    add(
                        ServerConfig(
                            id = id,
                            name = item.optString("name").ifBlank { item.optString("host") },
                            host = item.optString("host"),
                            port = item.optInt("port", 9000),
                            daphilePort = item.optInt("daphilePort", 80),
                            username = item.optString("username"),
                            password = password,
                            useHttps = item.optBoolean("useHttps", false)
                        )
                    )
                }
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun encodeServers(servers: List<ServerConfig>): String {
        val array = JSONArray()
        servers.forEach { server ->
            array.put(
                JSONObject()
                    .put("id", server.id)
                    .put("name", server.name)
                    .put("host", server.host)
                    .put("port", server.port)
                    .put("daphilePort", server.daphilePort)
                    .put("username", server.username)
                    .put("password", crypto.encrypt(server.password))
                    .put("useHttps", server.useHttps)
            )
        }
        return array.toString()
    }
}
