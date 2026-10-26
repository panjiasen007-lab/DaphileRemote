package com.daphile.remote.media

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * 正在播放时保持进程存活的前台服务。
 *
 * 达菲是遥控端播放：如果没有前台服务，锁屏后系统会限制应用后台运行，
 * 轮询停止导致锁屏进度条与通知不再刷新。这里用 mediaPlayback 类型的前台服务
 * 承载媒体通知，并持有唤醒锁，保证锁屏后仍能持续同步播放状态。
 */
class PlaybackService : Service() {

    companion object {
        private const val WAKE_LOCK_TAG = "DaphileRemote:playback"

        @Volatile
        private var latestNotification: Notification? = null

        @Volatile
        private var running = false

        /** 更新前台通知；服务未启动时先启动前台服务。 */
        fun publish(context: Context, notification: Notification) {
            latestNotification = notification
            val intent = Intent(context, PlaybackService::class.java)
            runCatching {
                if (running) {
                    context.startService(intent)
                } else {
                    ContextCompat.startForegroundService(context, intent)
                }
            }
        }

        /** 停止前台服务并移除通知。 */
        fun stop(context: Context) {
            latestNotification = null
            running = false
            runCatching { context.stopService(Intent(context, PlaybackService::class.java)) }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        val power = getSystemService(PowerManager::class.java)
        wakeLock = power
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            ?.apply { setReferenceCounted(false) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = latestNotification
        if (notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    MediaSessionController.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(MediaSessionController.NOTIFICATION_ID, notification)
            }
        }
        runCatching { wakeLock?.acquire() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
