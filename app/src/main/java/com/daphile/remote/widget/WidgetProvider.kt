package com.daphile.remote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.daphile.remote.DaphileApp
import com.daphile.remote.MainActivity
import com.daphile.remote.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class WidgetProvider : AppWidgetProvider() {
    companion object {
        const val ACTION_WIDGET_COMMAND = "com.daphile.remote.WIDGET_ACTION"
        const val EXTRA_COMMAND = "command"
        const val COMMAND_PLAY = "play"
        const val COMMAND_PAUSE = "pause"
        const val COMMAND_NEXT = "next"
        const val COMMAND_PREVIOUS = "previous"
        const val COMMAND_OPEN = "open"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))
            ids.forEach { id ->
                manager.updateAppWidget(id, buildViews(context))
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val preferences = context.getSharedPreferences(WidgetBridge.PREFS, Context.MODE_PRIVATE)
            val title = preferences.getString(WidgetBridge.KEY_TITLE, "").orEmpty()
                .ifBlank { context.getString(R.string.widget_default_title) }
            val artist = preferences.getString(WidgetBridge.KEY_ARTIST, "").orEmpty()
                .ifBlank { context.getString(R.string.widget_default_artist) }
            val playing = preferences.getBoolean(WidgetBridge.KEY_PLAYING, false)

            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            views.setImageViewResource(
                R.id.widget_play,
                if (playing) R.drawable.ic_pause else R.drawable.ic_play
            )

            val coverFile = File(context.cacheDir, WidgetBridge.COVER_FILE)
            if (coverFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(coverFile.absolutePath)
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.widget_cover, bitmap)
                } else {
                    views.setImageViewResource(R.id.widget_cover, R.drawable.widget_placeholder)
                }
            } else {
                views.setImageViewResource(R.id.widget_cover, R.drawable.widget_placeholder)
            }

            views.setOnClickPendingIntent(
                R.id.widget_play,
                commandIntent(context, if (playing) COMMAND_PAUSE else COMMAND_PLAY, 21)
            )
            views.setOnClickPendingIntent(
                R.id.widget_previous,
                commandIntent(context, COMMAND_PREVIOUS, 22)
            )
            views.setOnClickPendingIntent(
                R.id.widget_next,
                commandIntent(context, COMMAND_NEXT, 23)
            )
            views.setOnClickPendingIntent(R.id.widget_root, openIntent(context))
            return views
        }

        private fun commandIntent(context: Context, command: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, WidgetProvider::class.java)
                .setAction(ACTION_WIDGET_COMMAND)
                .putExtra(EXTRA_COMMAND, command)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context,
                20,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private suspend fun executeCommand(context: Context, command: String) {
            if (command == COMMAND_OPEN) {
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                return
            }
            val app = context.applicationContext as? DaphileApp ?: return
            val preferences = context.getSharedPreferences(WidgetBridge.PREFS, Context.MODE_PRIVATE)
            val server = app.graph.settings.currentServerSnapshot() ?: return
            val playerId = app.graph.settings.playerForServer(server.id) ?: return
            when (command) {
                COMMAND_PLAY -> app.graph.client.command(server, playerId, "play")
                COMMAND_PAUSE -> app.graph.client.command(server, playerId, "pause", "1")
                COMMAND_NEXT -> app.graph.client.command(server, playerId, "playlist", "jump", "+1")
                COMMAND_PREVIOUS -> app.graph.client.command(server, playerId, "playlist", "jump", "-1")
            }
            if (command == COMMAND_PLAY || command == COMMAND_PAUSE) {
                preferences.edit()
                    .putBoolean(WidgetBridge.KEY_PLAYING, command == COMMAND_PLAY)
                    .apply()
                updateAll(context)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WIDGET_COMMAND) {
            val command = intent.getStringExtra(EXTRA_COMMAND) ?: return
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    executeCommand(context, command)
                } catch (_: Throwable) {
                    // Widget actions are best-effort; the next app session reconciles state.
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }
}
