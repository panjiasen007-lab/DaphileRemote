package com.daphile.remote.media

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.daphile.remote.MainActivity
import com.daphile.remote.R
import com.daphile.remote.data.model.AppSettings
import com.daphile.remote.data.model.NowPlaying
import com.daphile.remote.data.model.ServerConfig
import com.daphile.remote.data.repo.DaphileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaSessionController(
    private val context: Context,
    private val repository: DaphileRepository
) {
    companion object {
        const val CHANNEL_ID = "daphile_playback"
        const val NOTIFICATION_ID = 1001
        private const val ARTWORK_MAX = 320
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val session = MediaSessionCompat(context, "DaphileRemote").apply {
        setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                scope.launch { runCatching { repository.play() } }
            }

            override fun onPause() {
                scope.launch { runCatching { repository.pause() } }
            }

            override fun onStop() {
                scope.launch { runCatching { repository.stop() } }
            }

            override fun onSkipToNext() {
                scope.launch { runCatching { repository.next() } }
            }

            override fun onSkipToPrevious() {
                scope.launch { runCatching { repository.previous() } }
            }

            override fun onSeekTo(pos: Long) {
                scope.launch { runCatching { repository.seekTo(pos / 1000.0) } }
            }
        })
        setSessionActivity(openAppIntent())
    }

    private var lastArtworkUrl: String? = null
    private var lastArtwork: Bitmap? = null
    private var lastQueueKey: String? = null

    init {
        createNotificationChannel()
        scope.launch {
            combine(
                repository.nowPlaying,
                repository.server,
                repository.appSettings
            ) { nowPlaying, server, settings ->
                Triple(nowPlaying, server, settings)
            }.collect { (nowPlaying, server, settings) ->
                update(nowPlaying, server, settings)
            }
        }
    }

    private suspend fun update(
        nowPlaying: NowPlaying?,
        server: ServerConfig?,
        settings: AppSettings
    ) {
        val track = nowPlaying?.track
        if (nowPlaying == null || track == null || !nowPlaying.power) {
            session.isActive = false
            session.setPlaybackState(stoppedState())
            cancelNotification()
            return
        }

        session.isActive = settings.lockScreenControls
        val artworkUrl = server?.coverUrl(track.coverId, nowPlaying.playerId)
        if (artworkUrl != lastArtworkUrl) {
            lastArtwork = loadArtwork(artworkUrl)
            lastArtworkUrl = artworkUrl
            lastQueueKey = null
        }

        val queueKey = nowPlaying.queue.joinToString("|") { "${it.id}:${it.title}" }
        if (queueKey != lastQueueKey) {
            lastQueueKey = queueKey
            val queueItems = nowPlaying.queue.mapIndexed { index, item ->
                MediaSessionCompat.QueueItem(
                    MediaDescriptionCompat.Builder()
                        .setMediaId(item.id)
                        .setTitle(item.title.ifBlank { "未知曲目" })
                        .setSubtitle(item.artist)
                        .build(),
                    index.toLong()
                )
            }
            session.setQueue(queueItems)
            session.setQueueTitle(nowPlaying.track?.album.orEmpty())
        }

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, (nowPlaying.duration * 1000).toLong())
            .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artworkUrl)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artworkUrl)
            .apply { lastArtwork?.let { putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it) } }
            .build()
        session.setMetadata(metadata)
        session.setPlaybackState(playbackState(nowPlaying))

        if (settings.notificationsEnabled) {
            postNotification(nowPlaying, artwork = lastArtwork, lockScreen = settings.lockScreenControls)
        } else {
            cancelNotification()
        }
    }

    /** 通知优先交给前台服务承载，锁屏后系统不会冻结轮询。 */
    private fun publishNotification(notification: Notification) {
        runCatching { PlaybackService.publish(context, notification) }
            .onFailure {
                runCatching {
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                }
            }
    }

    private fun playbackState(nowPlaying: NowPlaying): PlaybackStateCompat {
        val state = when {
            nowPlaying.isPlaying -> PlaybackStateCompat.STATE_PLAYING
            nowPlaying.isPaused -> PlaybackStateCompat.STATE_PAUSED
            else -> PlaybackStateCompat.STATE_STOPPED
        }
        var actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        if (nowPlaying.canSeek) actions = actions or PlaybackStateCompat.ACTION_SEEK_TO
        val durationMs = (nowPlaying.duration * 1000).toLong()
        val positionMs = (nowPlaying.time * 1000).toLong().coerceAtLeast(0L).let {
            if (durationMs > 0) it.coerceAtMost(durationMs) else it
        }
        return PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(
                state,
                positionMs,
                if (nowPlaying.isPlaying) 1f else 0f
            )
            .build()
    }

    private fun stoppedState(): PlaybackStateCompat =
        PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
            .setState(PlaybackStateCompat.STATE_STOPPED, 0L, 0f)
            .build()

    private fun postNotification(nowPlaying: NowPlaying, artwork: Bitmap?, lockScreen: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val track = nowPlaying.track ?: return
        val playAction = if (nowPlaying.isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "暂停",
                notificationAction(NotificationActionReceiver.ACTION_TOGGLE, 11)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play,
                "播放",
                notificationAction(NotificationActionReceiver.ACTION_TOGGLE, 12)
            )
        }
        val subtitle = listOf(track.artist, track.album)
            .filter { it.isNotBlank() }
            .joinToString(" · ")

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle(track.title.ifBlank { context.getString(R.string.app_name) })
            .setContentText(subtitle)
            .setContentIntent(openAppIntent())
            .setVisibility(
                if (lockScreen) NotificationCompat.VISIBILITY_PUBLIC
                else NotificationCompat.VISIBILITY_SECRET
            )
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(nowPlaying.isPlaying)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_previous,
                    "上一首",
                    notificationAction(NotificationActionReceiver.ACTION_PREVIOUS, 13)
                )
            )
            .addAction(playAction)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_skip_next,
                    "下一首",
                    notificationAction(NotificationActionReceiver.ACTION_NEXT, 14)
                )
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        artwork?.let { builder.setLargeIcon(it) }

        publishNotification(builder.build())
    }

    private fun cancelNotification() {
        PlaybackService.stop(context)
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }

    private suspend fun loadArtwork(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(ARTWORK_MAX)
                    .allowHardware(false)
                    .build()
                when (val result = context.imageLoader.execute(request)) {
                    is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
                    else -> null
                }
            }.getOrNull()
        }
    }

    private fun notificationAction(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            10,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }
}
