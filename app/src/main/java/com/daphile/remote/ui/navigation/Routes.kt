package com.daphile.remote.ui.navigation

import android.net.Uri

object Routes {
    const val NOW = "now"
    const val LIBRARY = "library"
    const val RADIO = "radio"
    const val PLAYERS = "players"
    const val SETTINGS = "settings"

    const val SERVERS = "servers"
    const val SERVER_EDIT = "server_edit?serverId={serverId}&first={first}"

    const val ALBUMS = "albums?title={title}&artistId={artistId}&genreId={genreId}&year={year}&sort={sort}"
    const val ALBUM = "album/{albumId}"
    const val PLAYLIST = "playlist/{playlistId}?name={name}"
    const val BROWSE = "browse/{category}"
    const val BROWSER = "browser/{source}/{pluginCmd}/{itemId}?title={title}"
    const val SYSTEM_INFO = "system_info"
    const val SONGS = "songs"
    const val FOLDERS = "folders"
    const val LYRICS = "lyrics"
    const val AUDIO_OUTPUT = "audio_output"

    fun serverEdit(serverId: String? = null, first: Boolean = false): String =
        "server_edit?serverId=${Uri.encode(serverId.orEmpty())}&first=$first"

    fun albums(
        title: String,
        artistId: String? = null,
        genreId: String? = null,
        year: String? = null,
        sort: String = "album"
    ): String =
        "albums?title=${Uri.encode(title)}&artistId=${Uri.encode(artistId.orEmpty())}" +
            "&genreId=${Uri.encode(genreId.orEmpty())}&year=${Uri.encode(year.orEmpty())}" +
            "&sort=${Uri.encode(sort)}"

    fun album(albumId: String): String = "album/${Uri.encode(albumId)}"

    fun playlist(playlistId: String, name: String): String =
        "playlist/${Uri.encode(playlistId)}?name=${Uri.encode(name)}"

    fun browse(category: String): String = "browse/$category"

    fun browser(source: String, pluginCmd: String, itemId: String, title: String): String =
        "browser/$source/${Uri.encode(pluginCmd)}/${Uri.encode(itemId)}?title=${Uri.encode(title)}"

    val topLevel: Set<String> = setOf(NOW, LIBRARY, RADIO, PLAYERS, SETTINGS)
}
