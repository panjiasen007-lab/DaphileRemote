package com.daphile.remote.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.daphile.remote.DaphileApp
import com.daphile.remote.data.model.SearchResults
import com.daphile.remote.data.model.friendlyErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as DaphileApp).graph.repository

    val serverInfo = repository.serverInfo
    val server = repository.server
    val nowPlaying = repository.nowPlaying
    val scanStatus = repository.scanStatus

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _results = MutableStateFlow<SearchResults?>(null)
    val results = _results.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching = _searching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _query.value = value
        searchJob?.cancel()
        if (value.isBlank()) {
            _results.value = null
            _searching.value = false
            _searchError.value = null
            return
        }
        val term = value.trim()
        searchJob = viewModelScope.launch {
            delay(400)
            _searching.value = true
            _searchError.value = null
            try {
                _results.value = repository.search(term)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Throwable) {
                _searchError.value = friendlyErrorMessage(error)
            } finally {
                _searching.value = false
            }
        }
    }

    fun clearQuery() {
        _query.value = ""
        searchJob?.cancel()
        _results.value = null
        _searching.value = false
        _searchError.value = null
    }

    fun retrySearch() {
        onQueryChange(_query.value)
    }

    suspend fun playTrack(trackId: String) = repository.playTrack(trackId)
    suspend fun queueTrack(trackId: String) = repository.queueTrack(trackId)
    suspend fun playAlbum(albumId: String) = repository.playAlbum(albumId)
    suspend fun queueAlbum(albumId: String) = repository.playAlbum(albumId, append = true)
    suspend fun randomAlbum() = repository.randomAlbum()

    suspend fun fetchAlbums(
        start: Int,
        count: Int,
        sort: String,
        artistId: String? = null,
        genreId: String? = null,
        year: String? = null
    ) = repository.albums(start, count, sort, artistId, genreId, year)

    suspend fun fetchArtists(start: Int, count: Int) = repository.artists(start, count)
    suspend fun fetchGenres(start: Int, count: Int) = repository.genres(start, count)
    suspend fun fetchYears(start: Int, count: Int) = repository.years(start, count)
    suspend fun fetchPlaylists(start: Int, count: Int) = repository.playlists(start, count)
    suspend fun fetchPlaylistTracks(playlistId: String) = repository.playlistTracks(playlistId)
    suspend fun playPlaylist(playlistId: String) = repository.playPlaylist(playlistId)
    suspend fun fetchAlbumDetails(albumId: String) = repository.albumDetails(albumId)

    suspend fun fetchSongs(start: Int, count: Int) = repository.songs(start, count)
    suspend fun fetchMusicFolders(parentId: String?, start: Int, count: Int) =
        repository.musicFolders(parentId, start, count)

    suspend fun playFolder(folderId: String) = repository.playFolder(folderId)
    suspend fun playFolderItem(item: com.daphile.remote.data.model.BrowserItem) =
        repository.playFolderItem(item)
    suspend fun queueFolderItem(item: com.daphile.remote.data.model.BrowserItem) =
        repository.queueFolderItem(item)

    suspend fun startScan(full: Boolean) = repository.startScan(full)
    suspend fun refreshServerInfo() = repository.refreshServerInfo()
    suspend fun resolveAlbumCover(artist: String, album: String, force: Boolean = true) =
        repository.resolveOnlineCover(artist, album, force)
}
