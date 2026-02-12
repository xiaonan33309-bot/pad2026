package com.pad2026.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pad2026.app.R

class PlaybackForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(1, createNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "PAD Playback Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.foreground_service_desc))
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "playback_fg"

        fun start(context: Context) {
            val intent = Intent(context, PlaybackForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
