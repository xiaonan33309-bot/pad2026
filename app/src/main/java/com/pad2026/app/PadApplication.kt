package com.pad2026.app

import android.app.Application
import com.pad2026.app.sync.SyncScheduler

class PadApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SyncScheduler.schedulePeriodicSync(this)
    }
}
