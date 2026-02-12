package com.pad2026.app.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pad2026.app.MainActivity
import com.pad2026.app.service.PlaybackForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            SyncScheduler.schedulePeriodicSync(context)
            PlaybackForegroundService.start(context)
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(launchIntent)
        }
    }
}
