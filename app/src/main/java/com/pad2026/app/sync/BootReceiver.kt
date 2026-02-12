package com.pad2026.app.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pad2026.app.service.PlaybackForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            SyncScheduler.schedulePeriodicSync(context)
            PlaybackForegroundService.start(context)
        }
    }
}
