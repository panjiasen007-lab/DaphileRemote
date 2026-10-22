package com.daphile.remote

import android.content.Context
import com.daphile.remote.data.local.CryptoStore
import com.daphile.remote.data.local.SettingsStore
import com.daphile.remote.data.online.OnlineMetadataService
import com.daphile.remote.data.remote.LmsClient
import com.daphile.remote.data.repo.DaphileRepository
import com.daphile.remote.media.MediaSessionController
import com.daphile.remote.widget.WidgetBridge

class AppGraph(context: Context) {
    val crypto = CryptoStore()
    val settings = SettingsStore(context.applicationContext, crypto)
    val client = LmsClient()
    val onlineMetadata = OnlineMetadataService(context.applicationContext)
    val repository = DaphileRepository(
        context.applicationContext,
        settings,
        client,
        onlineMetadata
    )
    val mediaSession = MediaSessionController(context.applicationContext, repository)
    val widgetBridge = WidgetBridge(context.applicationContext, repository)

    init {
        repository.start()
    }
}
