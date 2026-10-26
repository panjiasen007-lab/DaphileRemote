package com.daphile.remote.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daphile.remote.DaphileApp
import com.daphile.remote.data.model.BrowserItem
import com.daphile.remote.data.model.PluginEntry
import com.daphile.remote.data.model.friendlyErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RadioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DaphileApp).graph.repository

    val server = repository.server

    private val _favorites = MutableStateFlow<List<BrowserItem>?>(null)
    val favorites = _favorites.asStateFlow()

    private val _radios = MutableStateFlow<List<PluginEntry>?>(null)
    val radios = _radios.asStateFlow()

    private val _apps = MutableStateFlow<List<PluginEntry>?>(null)
    val apps = _apps.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _favorites.value = repository.favorites(null)
                _radios.value = repository.radios()
                _apps.value = repository.apps()
            } catch (error: Throwable) {
                _error.value = friendlyErrorMessage(error)
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun fetchFavorites(parentId: String?): List<BrowserItem> =
        repository.favorites(parentId)

    suspend fun fetchPluginItems(pluginCmd: String, parentId: String): List<BrowserItem> =
        repository.pluginItems(pluginCmd, parentId)

    suspend fun playFavorite(itemId: String) = repository.playFavorite(itemId)

    suspend fun favoritesToQueue(play: Boolean) = repository.favoritesToQueue(play)

    suspend fun playItem(pluginCmd: String?, item: BrowserItem) =
        repository.playBrowserItem(pluginCmd, item)
}
