package com.daphile.remote.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daphile.remote.DaphileApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TOGGLE = "com.daphile.remote.NOTIFY_TOGGLE"
        const val ACTION_NEXT = "com.daphile.remote.NOTIFY_NEXT"
        const val ACTION_PREVIOUS = "com.daphile.remote.NOTIFY_PREVIOUS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as? DaphileApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = app.graph.repository
                when (action) {
                    ACTION_TOGGLE -> repository.playPause()
                    ACTION_NEXT -> repository.next()
                    ACTION_PREVIOUS -> repository.previous()
                }
            } catch (_: Throwable) {
                // Notification actions are best-effort; the next poll corrects the UI.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
